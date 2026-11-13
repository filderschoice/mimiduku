"""PreToolUse フック: guardrails 12.2 の禁止操作を、Bash・PowerShell ツールの実行前に拒否する（BL-077・BL-082）。

位置づけ: エージェントの誤操作を止める**安全網**であり、意図的な回避まで防ぐサンドボックスではない。普通に書かれ得る形は
確実に判定し、判定しきれない形は下の「限界」に挙げる（`.claude/settings.json` の `permissions.deny` も同じく境界ではない）。

`permissions.deny` はコマンドの先頭一致でしか判定できず、`git -C . push` や `git commit -m x --amend` のような書き方を
取りこぼす。本フックはコマンドをシェルの語に分解し、サブコマンドとオプションを語として判定して deny を補う。
コミットメッセージやヒアドキュメントの本文に「push」「rebase」などの単語が含まれるだけでは拒否しない。

拒否するもの:

- git push（`--force` を含む全形）・send-pack、git rebase、git reset --hard、git commit --amend
- `--no-verify`（git commit の `-n` を含む）、git の `--force` 系の長いオプション
- git branch -d / -D / --delete / -f / -M / -C、git tag -d / --delete / -f（ブランチ・タグの削除と上書き）
- 規定ブランチ（main / master）上での git commit・merge・cherry-pick・revert・am
- 検証のバイパスとエイリアス経由の実行: `git -c core.hooksPath=...`、`git config` での core.hooksPath・alias.* の設定。
  `git -c alias.x=...` は展開して判定する
- gh の書き込み: pr・issue の作成と更新、`gh api` の書き込み（`-X`/`--method` が GET 以外、または `-f`/`-F`/`--field`/
  `--raw-field`/`--input` で本文を送る形）、release の作成・編集・削除、workflow run、pr update-branch など（GH_WRITE）

判定する書き方:

- git の長いオプションの短縮形（`--am`・`--no-ver`・`--har`・`--del` のように、git が一意な接頭辞として受け付ける形）
- 制御構文（`if`・`then`・`do`・`!` 等の後）、オプション付きのラッパー（`env -i`・`command -p`・`sudo -u x`・`time -p`・
  `timeout 5`・`xargs -n1` 等）、リダイレクト（`git >log push`）
- コマンド置換（`$(...)`・バッククォート。ダブルクォートの中と、引用符なしのヒアドキュメント本文の中を含む）
- `bash -c` / `sh -c` / `eval` / `powershell -Command`（`-EncodedCommand` を含む）/ `Invoke-Expression` / `cmd /c` の
  引数の文字列、`bash <<EOF` の本文（同じ判定へ再帰的に通す）

限界（判定しない・しきれない形。安全網の範囲外として扱う）:

- 変数・展開を経由する名前（`$GIT push`、`g=git; $g push`、`git $(echo push)`、cmd の `%VAR%`）
- 標準入力やファイルから読まれる引数とスクリプト（`echo push | xargs git`、`echo "git push" | bash`、`bash script.sh`、
  `source`、`powershell -File`）
- 外部の設定に依存する形（gitconfig に定義済みの git のエイリアス、gh のエイリアス・拡張、環境変数 `GIT_CONFIG_*`、
  シェル関数・alias を定義してからの呼び出し）
- 別プログラム経由の起動（`Start-Process`、`find -exec`、Python の subprocess、make など）
- `$(...)` の中の bash の `case` の `)`、`((` を入れ子のサブシェルとして使う書き方
- 誤止めとして承知しているもの: `gh api graphql` は読み取りでも POST で本文を送るため拒否する

入力は Claude Code が標準入力へ渡す JSON（`tool_name`・`tool_input.command`・`cwd`）。
拒否するときは理由を標準エラーへ出して終了コード2で終わる（Claude へ理由が伝わる）。
通すときは何も出さず終了コード0（通常の権限判定へ進む）。標準入力の JSON が読めない場合は終了コード1
（ブロックしないエラー）で通す。判定中に例外が起きた場合は、コマンドに禁止操作の語（push・amend・commit・gh など、
FALLBACK_PATTERN）が含まれるときだけ安全側で終了コード2とし、それ以外は通す（フックの不具合で全コマンドを
止めないため）。標準ライブラリのみで動作する。

テスト（実際の禁止コマンドは実行しない。JSON を標準入力へ渡すだけ）:

    python scripts/test-guard-commands.py

手動での確かめ方:

    echo '{"tool_name":"Bash","tool_input":{"command":"git -C . push"},"cwd":"."}' | python .claude/hooks/guard-commands.py
"""

import base64
import json
import os
import re
import subprocess
import sys

DEFAULT_BRANCHES = {"main", "master"}
COMMIT_LIKE = {"commit", "merge", "cherry-pick", "revert", "am"}
MAX_DEPTH = 8

GH_WRITE = {
    "pr": {"create", "new", "edit", "merge", "close", "reopen", "comment", "review", "ready", "lock", "unlock",
           "update-branch", "revert"},
    "issue": {"create", "new", "edit", "close", "reopen", "comment", "delete", "transfer", "pin", "unpin", "lock",
              "unlock", "develop"},
    "release": {"create", "new", "edit", "delete", "upload", "delete-asset"},
    "workflow": {"run", "enable", "disable"},
    "run": {"rerun", "cancel", "delete"},
    "repo": {"create", "new", "delete", "edit", "fork", "rename", "archive", "unarchive", "sync"},
    "secret": {"set", "delete", "remove"},
    "variable": {"set", "delete"},
    "label": {"create", "edit", "delete", "clone"},
    "gist": {"create", "new", "edit", "delete"},
    "cache": {"delete"},
    "alias": {"set", "import"},
}
GH_FLAGS_WITH_VALUE = {"-R", "--repo", "--hostname"}
GH_API_SHORT_WITH_VALUE = set("XfFHqtp")
GH_API_LONG_WITH_VALUE = {"--method", "--field", "--raw-field", "--input", "--header", "--jq", "--template", "--cache",
                          "--preview", "--hostname"}

# 値を1つ取る git のグローバルオプション（`git -C <path> push` の <path> をサブコマンドと誤認しないため）
GIT_GLOBAL_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--config-env", "--super-prefix",
                         "--exec-path"}
# サブコマンドごとの、値を取るオプション（値をオプションと誤認しないため）。(短い・長い・値が任意で直後に付く短い)
GIT_SUB_OPTIONS = {
    "commit": ("mFCct", {"message", "file", "reuse-message", "reedit-message", "template", "author", "date", "cleanup",
                         "fixup", "squash", "trailer", "pathspec-from-file"}, "Su"),
    "merge": ("mFsX", {"message", "file", "strategy", "strategy-option", "cleanup", "into-name"}, "S"),
    "cherry-pick": ("msX", {"mainline", "strategy", "strategy-option", "cleanup"}, "S"),
    "revert": ("msX", {"mainline", "strategy", "strategy-option", "cleanup"}, "S"),
    "branch": ("u", {"set-upstream-to", "contains", "no-contains", "merged", "no-merged", "points-at", "sort",
                     "format"}, ""),
    "tag": ("mFu", {"message", "file", "local-user", "contains", "no-contains", "merged", "no-merged", "points-at",
                    "sort", "format", "cleanup", "trailer"}, "n"),
    "reset": ("", {"pathspec-from-file"}, ""),
    "checkout": ("bB", {"orphan", "conflict", "pathspec-from-file"}, ""),
    "switch": ("cC", {"create", "force-create", "orphan", "conflict"}, ""),
    "config": ("f", {"file", "blob", "type", "default", "comment", "value"}, ""),
}

SHELLS = {"bash", "sh", "zsh", "dash", "ksh", "ash"}
KEYWORDS = {"if", "then", "else", "elif", "fi", "do", "done", "while", "until", "!", "elseif", "try", "finally"}
# ラッパーと、その値を1つ取るオプション
WRAPPERS = {
    "env": {"-u", "--unset", "-C", "--chdir", "-S", "--split-string"},
    "command": set(), "builtin": set(), "nohup": set(), "coproc": set(), "winpty": set(), ".": set(),
    "exec": {"-a"},
    "time": {"-f", "--format", "-o", "--output"},
    "sudo": {"-u", "-g", "-h", "-p", "-C", "-D", "-r", "-t", "-U", "-T", "--user", "--group", "--host", "--prompt",
             "--chdir", "--role", "--type", "--other-user", "--command-timeout", "--close-from"},
    "doas": {"-u", "-C"},
    "nice": {"-n", "--adjustment"},
    "timeout": {"-s", "--signal", "-k", "--kill-after"},
    "stdbuf": {"-i", "-o", "-e"},
    "xargs": {"-I", "-L", "-n", "-P", "-s", "-d", "-E", "-a", "--arg-file", "--delimiter", "--max-args", "--max-procs",
              "--max-chars", "--eof", "--replace", "--max-lines", "--process-slot-var"},
}
# 判定中の例外時に安全側で止める語（単純な部分一致。誤止めは例外時だけなので許容する）
FALLBACK_PATTERN = re.compile(
    r"push|amend|rebase|reset|no-verify|--force|hookspath|alias|delete|commit|merge|cherry-pick|revert|"
    r"\bam\b|\bbranch\b|\btag\b|\bgh\b|\bapi\b", re.IGNORECASE)


class Command:
    """1つの単純コマンド（語の並び）と、そのコマンドへ渡されるヒアドキュメントの本文。"""

    def __init__(self) -> None:
        self.words: list[str] = []
        self.heredocs: list[str] = []


class Parser:
    """シェルの文字列を単純コマンドの並びへ分ける。dialect は bash・powershell・cmd。

    コマンド置換（`$(...)`・バッククォート）の中のコマンドも、同じ一覧へ別のコマンドとして加える。
    """

    def __init__(self, text: str, dialect: str, depth: int = 0) -> None:
        if depth > MAX_DEPTH:
            raise RecursionError("入れ子が深すぎる")
        self.text = text
        self.n = len(text)
        self.pos = 0
        self.dialect = dialect
        self.bash = dialect == "bash"
        self.ps = dialect == "powershell"
        self.cmd = dialect == "cmd"
        self.escape = "`" if self.ps else "^" if self.cmd else "\\"
        self.depth = depth
        self.commands: list[Command] = []
        self.pending: list[tuple[str, bool, bool, Command]] = []

    def parse(self) -> list[Command]:
        self._parse_list(closing=False)
        return self.commands

    def _sub(self, text: str, dialect: str | None = None) -> None:
        self.commands.extend(Parser(text, dialect or self.dialect, self.depth + 1).parse())

    def _parse_list(self, closing: bool) -> None:  # noqa: C901 - 字句の分岐を1か所にまとめる
        text = self.text
        cur = Command()
        buf: list[str] = []
        has = False
        skip_word = False
        paren = 0

        def flush() -> None:
            nonlocal buf, has, skip_word
            if has:
                if skip_word:
                    skip_word = False
                else:
                    cur.words.append("".join(buf))
            buf = []
            has = False

        def end_cmd() -> None:
            nonlocal cur, skip_word
            flush()
            skip_word = False
            if cur.words or cur.heredocs:
                self.commands.append(cur)
            cur = Command()

        while self.pos < self.n:
            p = self.pos
            ch = text[p]
            if ch == self.escape and p + 1 < self.n:
                nxt = text[p + 1]
                if nxt == "\n":
                    self.pos += 2
                    continue
                if nxt == "\r" and text.startswith("\n", p + 2):
                    self.pos += 3
                    continue
                buf.append(nxt)
                has = True
                self.pos += 2
                continue
            if ch == "'" and not self.cmd:
                end = text.find("'", p + 1)
                end = self.n if end < 0 else end
                buf.append(text[p + 1:end])
                has = True
                self.pos = end + 1
                continue
            if ch == '"':
                self.pos += 1
                buf.append(self._expand_text(until_quote=True))
                has = True
                continue
            if self.ps and text.startswith(("@'", '@"'), p) and self._here_string():
                buf.append("@here")
                has = True
                continue
            if self.ps and text.startswith("<#", p):
                end = text.find("#>", p + 2)
                self.pos = self.n if end < 0 else end + 2
                continue
            if not self.cmd:
                if self.bash and (text.startswith("$((", p) or text.startswith("$[", p)):
                    self._skip_arith()
                    buf.append("$((…))")
                    has = True
                    continue
                if self.bash and text.startswith("((", p) and not has and (not cur.words or cur.words == ["for"]):
                    self._skip_arith()
                    continue
                if text.startswith("$(", p):
                    self.pos += 2
                    self._parse_list(closing=True)
                    buf.append("$(…)")
                    has = True
                    continue
                if text.startswith("${", p):
                    end = text.find("}", p)
                    end = self.n if end < 0 else end
                    buf.append(text[p:end + 1])
                    has = True
                    self.pos = end + 1
                    continue
                if self.bash and ch == "`":
                    self._backtick()
                    buf.append("`…`")
                    has = True
                    continue
                if ch == "#" and not has:
                    end = text.find("\n", p)
                    self.pos = self.n if end < 0 else end
                    continue
            if self.bash and text.startswith("<<<", p):
                flush()
                self.pos += 3
                continue
            if self.bash and text.startswith("<<", p):
                flush()
                self.pos += 2
                self._heredoc_start(cur)
                continue
            if ch in "<>" or (ch == "&" and text.startswith(">", p + 1)):
                # リダイレクト。直前の数字（2>&1 の 2）と、対象の語は判定の語から除く
                if has and "".join(buf).isdigit():
                    buf = []
                    has = False
                else:
                    flush()
                while self.pos < self.n and text[self.pos] in "<>&|":
                    self.pos += 1
                if self.pos < self.n and text[self.pos] == "(" and not self.cmd:
                    self.pos += 1  # プロセス置換 <(...) >(...)
                    self._parse_list(closing=True)
                    continue
                while self.pos < self.n and text[self.pos] in " \t":
                    self.pos += 1
                skip_word = True
                continue
            if ch == "\n":
                end_cmd()
                self.pos += 1
                if self.pending:
                    self._heredoc_bodies()
                continue
            if ch == "(":
                end_cmd()
                paren += 1
                self.pos += 1
                continue
            if ch == ")":
                end_cmd()
                self.pos += 1
                if closing and paren == 0:
                    return
                paren = max(0, paren - 1)
                continue
            if ch in ";|&{}":
                end_cmd()
                self.pos += 1
                continue
            if ch in " \t\r":
                flush()
                self.pos += 1
                continue
            buf.append(ch)
            has = True
            self.pos += 1
        end_cmd()

    def _expand_text(self, until_quote: bool) -> str:
        """ダブルクォートの中（until_quote）またはヒアドキュメント本文を読み、コマンド置換を判定へ加える。"""
        text = self.text
        part: list[str] = []
        while self.pos < self.n:
            p = self.pos
            ch = text[p]
            if until_quote and ch == '"':
                if self.ps and text.startswith('""', p):
                    part.append('"')
                    self.pos += 2
                    continue
                self.pos += 1
                break
            if self.cmd:
                part.append(ch)
                self.pos += 1
                continue
            if ch == self.escape and p + 1 < self.n:
                nxt = text[p + 1]
                if self.ps or nxt in '$`"\\\n':
                    part.append("" if nxt == "\n" else nxt)
                    self.pos += 2
                    continue
                part.append(ch)
                self.pos += 1
                continue
            if self.bash and (text.startswith("$((", p) or text.startswith("$[", p)):
                self._skip_arith()
                part.append("$((…))")
                continue
            if text.startswith("$(", p):
                self.pos += 2
                self._parse_list(closing=True)
                part.append("$(…)")
                continue
            if self.bash and ch == "`":
                self._backtick()
                part.append("`…`")
                continue
            part.append(ch)
            self.pos += 1
        return "".join(part)

    def _skip_arith(self) -> None:
        text = self.text
        if text.startswith("$[", self.pos):
            end = text.find("]", self.pos)
            self.pos = self.n if end < 0 else end + 1
            return
        self.pos += 3 if text.startswith("$((", self.pos) else 2
        depth = 2
        while self.pos < self.n and depth > 0:
            if text[self.pos] == "(":
                depth += 1
            elif text[self.pos] == ")":
                depth -= 1
            self.pos += 1

    def _backtick(self) -> None:
        text = self.text
        j = self.pos + 1
        part: list[str] = []
        while j < self.n and text[j] != "`":
            if text[j] == "\\" and j + 1 < self.n and text[j + 1] in "`\\$":
                part.append(text[j + 1])
                j += 2
                continue
            part.append(text[j])
            j += 1
        self.pos = j + 1
        self._sub("".join(part))

    def _heredoc_start(self, cur: Command) -> None:
        text = self.text
        strip_tabs = False
        if self.pos < self.n and text[self.pos] == "-":
            strip_tabs = True
            self.pos += 1
        while self.pos < self.n and text[self.pos] in " \t":
            self.pos += 1
        word: list[str] = []
        quoted = False
        while self.pos < self.n and text[self.pos] not in " \t\r\n;|&<>()":
            ch = text[self.pos]
            if ch in "'\"":
                end = text.find(ch, self.pos + 1)
                end = self.n if end < 0 else end
                word.append(text[self.pos + 1:end])
                quoted = True
                self.pos = end + 1
                continue
            if ch == "\\" and self.pos + 1 < self.n:
                word.append(text[self.pos + 1])
                quoted = True
                self.pos += 2
                continue
            word.append(ch)
            self.pos += 1
        if word:
            self.pending.append(("".join(word), strip_tabs, not quoted, cur))

    def _heredoc_bodies(self) -> None:
        text = self.text
        pending, self.pending = self.pending, []
        for delimiter, strip_tabs, expand, cur in pending:
            lines: list[str] = []
            while self.pos < self.n:
                eol = text.find("\n", self.pos)
                end = self.n if eol < 0 else eol
                line = text[self.pos:end]
                self.pos = self.n if eol < 0 else eol + 1
                check = line.rstrip("\r")
                if strip_tabs:
                    check = check.lstrip("\t")
                if check == delimiter:
                    break
                lines.append(line)
            body = "\n".join(lines)
            cur.heredocs.append(body)
            if expand:
                sub = Parser(body, self.dialect, self.depth + 1)
                sub._expand_text(until_quote=False)
                self.commands.extend(sub.commands)

    def _here_string(self) -> bool:
        """PowerShell のヒア文字列 @'...'@ / @"..."@。開始が行末で終わる場合だけ扱う。"""
        text = self.text
        quote = text[self.pos + 1]
        eol = text.find("\n", self.pos + 2)
        if eol < 0 or text[self.pos + 2:eol].strip():
            return False
        start = eol + 1
        closing = re.compile(r"^[ \t]*" + re.escape(quote) + "@", re.M)
        match = closing.search(text, start)
        end = match.start() if match else self.n
        body = text[start:end]
        self.pos = match.end() if match else self.n
        if quote == '"':
            sub = Parser(body, self.dialect, self.depth + 1)
            sub._expand_text(until_quote=False)
            self.commands.extend(sub.commands)
        return True


def program_name(word: str) -> str:
    base = re.split(r"[\\/]", word)[-1].lower()
    return base[:-4] if base.endswith(".exe") else base


def first_words(script: str, dialect: str = "bash") -> list[str]:
    commands = Parser(script, dialect).parse()
    return commands[0].words if commands else []


def strip_prefix(words: list[str], dialect: str) -> list[str]:
    """制御構文の語・変数の代入・ラッパー（オプションつきを含む）を先頭から除く。"""
    i = 0
    while i < len(words):
        word = words[i]
        name = program_name(word)
        if word in KEYWORDS:
            i += 1
        elif dialect == "powershell" and word.startswith("$") and i + 1 < len(words) and re.fullmatch(
                r"[-+*/%]?=", words[i + 1]):
            i += 2
        elif dialect == "powershell" and re.match(r"^\$[\w:]+=", word):
            rest = word.split("=", 1)[1]
            if rest:
                return strip_prefix([rest] + words[i + 1:], dialect)
            i += 1
        elif re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", word):
            i += 1
        elif name in WRAPPERS:
            with_value = WRAPPERS[name]
            i += 1
            while i < len(words) and words[i].startswith("-") and len(words[i]) > 1:
                opt, eq, value = words[i].partition("=")
                if opt == "--":
                    i += 1
                    break
                if opt in with_value and not eq:
                    value = words[i + 1] if i + 1 < len(words) else ""
                    i += 1
                if name == "env" and opt in ("-S", "--split-string"):
                    return strip_prefix(first_words(value) + words[i + 1:], dialect)
                i += 1
            if name == "timeout" and i < len(words):
                i += 1  # 時間の指定
        else:
            break
    return words[i:]


def current_branch(directory: str) -> str:
    return _git_output(directory, ["branch", "--show-current"])


def previous_branch(directory: str) -> str:
    return _git_output(directory, ["rev-parse", "--abbrev-ref", "@{-1}"])


def _git_output(directory: str, args: list[str]) -> str:
    try:
        result = subprocess.run(["git", "-C", directory, *args], capture_output=True, text=True, timeout=10,
                                check=False)
    except (OSError, subprocess.SubprocessError):
        return ""
    return result.stdout.strip() if result.returncode == 0 else ""


def abbrev_of(name: str, full: str) -> bool:
    """name が長いオプション full そのもの、または git が受け付ける短縮形（接頭辞）か。"""
    return name == full or (len(name) >= 3 and full.startswith(name))


def parse_options(args: list[str], spec: tuple[str, set[str], str]) -> tuple[set[str], list[str], dict, list[str],
                                                                            list[str] | None]:
    """git のサブコマンドの引数を分ける。戻り値は (短いオプション, 長いオプション名, 値, 位置引数, `--` の後)。"""
    short_val, long_val, short_opt = spec
    shorts: set[str] = set()
    longs: list[str] = []
    values: dict = {}
    positional: list[str] = []
    after: list[str] | None = None
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--":
            after = args[i + 1:]
            break
        if arg.startswith("--"):
            name, eq, value = arg.partition("=")
            longs.append(name)
            candidates = [name[2:]] if name[2:] in long_val else [f for f in long_val if abbrev_of(name, "--" + f)]
            if len(candidates) == 1:
                if not eq:
                    value = args[i + 1] if i + 1 < len(args) else ""
                    i += 1
                values[candidates[0]] = value
        elif arg.startswith("-") and len(arg) > 1:
            for k, c in enumerate(arg[1:]):
                if c in short_val:
                    value = arg[k + 2:]
                    if not value:
                        value = args[i + 1] if i + 1 < len(args) else ""
                        i += 1
                    values[c] = value
                    break
                shorts.add(c)
                if c in short_opt:
                    break
        else:
            positional.append(arg)
        i += 1
    return shorts, longs, values, positional, after


def check_git(args: list[str], cwd: str, state: dict, depth: int) -> str | None:
    directory = cwd
    aliases: dict[str, str] = {}
    i = 0
    while i < len(args) and args[i].startswith("-"):
        name, eq, value = args[i].partition("=")
        if name in GIT_GLOBAL_WITH_VALUE:
            if not eq:
                value = args[i + 1] if i + 1 < len(args) else ""
                i += 1
            if name == "-C":
                directory = os.path.join(directory, value)
            if name in ("-c", "--config-env"):
                key, _, setting = value.partition("=")
                key = key.lower()
                if key == "core.hookspath":
                    return "core.hooksPath の差し替えはフック（検証）のバイパスにあたるため禁止（guardrails 12.2）"
                if key.startswith("alias."):
                    if name == "--config-env":
                        return "環境変数から git のエイリアスを定義する形は判定できないため禁止（guardrails 12.2）"
                    aliases[key[len("alias."):]] = setting
        i += 1
    if i >= len(args):
        return None
    return check_git_sub(args[i], args[i + 1:], os.path.normpath(directory), state, aliases, depth)


def check_git_sub(sub: str, rest: list[str], directory: str, state: dict, aliases: dict, depth: int) -> str | None:
    if sub.lower() in aliases:
        value = aliases.pop(sub.lower())
        if value.startswith("!"):
            return evaluate(value[1:] + " " + " ".join(rest), "bash", directory, state, depth + 1)
        words = first_words(value)
        if not words:
            return None
        return check_git_sub(words[0], words[1:] + rest, directory, state, aliases, depth + 1)

    shorts, longs, values, positional, after = parse_options(rest, GIT_SUB_OPTIONS.get(sub, ("", set(), "")))

    if sub in ("push", "send-pack"):
        return "git push はユーザーが実行する（guardrails 5.3・12.2）。コマンド例の提示にとどめること"
    if sub == "rebase":
        return "git rebase は公開済みコミットの改変にあたるため禁止（guardrails 12.2）。修正コミットを積むこと"
    if sub == "reset" and any(abbrev_of(o, "--hard") for o in longs):
        return "git reset --hard は禁止（guardrails 12.2）"
    if any(abbrev_of(o, "--no-verify") for o in longs) or (sub == "commit" and "n" in shorts):
        return "--no-verify など検証をバイパスするオプションは禁止（guardrails 12.2）"
    if any(o.startswith("--force") or abbrev_of(o, "--force") for o in longs):
        return "--force 系のオプションは禁止（guardrails 12.2 の push・改変・削除の禁止を補う）"
    if sub == "commit" and any(abbrev_of(o, "--amend") for o in longs):
        return "git commit --amend は禁止（guardrails 12.2）。修正コミットを積むこと"
    if sub == "branch" and (any(abbrev_of(o, "--delete") for o in longs) or shorts & set("dDfMC")):
        return "ブランチの削除と上書き（-d・-D・-f・-M・-C）は禁止（guardrails 12.2）"
    if sub == "tag" and (any(abbrev_of(o, "--delete") for o in longs) or shorts & set("df")):
        return "タグの削除と上書き（-d・-f）は禁止（guardrails 12.2）"
    if sub == "config":
        return check_git_config(shorts, longs, positional)
    if sub in ("checkout", "switch"):
        # 同じコマンド内で切り替えた先を覚える（`git checkout main && git commit` を見逃さないため）
        target = switch_target(sub, shorts, longs, values, positional, after)
        if target == "-":
            target = previous_branch(directory)
        if target is not None:
            state[directory] = target.removeprefix("refs/heads/")
        return None
    if sub in COMMIT_LIKE:
        branch = state.get(directory) or current_branch(directory)
        if branch in DEFAULT_BRANCHES:
            return f"規定ブランチ（{branch}）へのコミットは禁止（guardrails 5.3・12.2）。作業ブランチで行うこと"
    return None


def switch_target(sub: str, shorts: set, longs: list, values: dict, positional: list, after: list | None) -> str | None:
    """checkout・switch で切り替わる先のブランチ名。切り替えない形（ファイルの取り出し）は None。"""
    for key in ("b", "B", "c", "C", "orphan", "create", "force-create"):
        if values.get(key):
            return values[key]
    if any(abbrev_of(o, "--detach") for o in longs) or (sub == "switch" and "d" in shorts):
        return "(detached)"
    if sub == "switch":
        return positional[0] if positional else None
    if "pathspec-from-file" in values or (after is not None and after) or len(positional) != 1:
        return None
    return positional[0]


def check_git_config(shorts: set, longs: list, positional: list) -> str | None:
    verb = positional[0] if positional and positional[0] in (
        "get", "set", "unset", "list", "rename-section", "remove-section", "edit") else None
    keys = positional[1:] if verb else positional
    targets = [k for k in keys if k.lower() == "core.hookspath" or k.lower().startswith("alias.")]
    if not targets:
        return None
    write = verb in ("set", "unset") or (verb is None and len(keys) >= 2) or any(
        o.startswith("--unset") or o in ("--add", "--replace-all") for o in longs)
    if write:
        return ("git config での core.hooksPath・alias.* の設定は、検証のバイパスや判定のすり抜けにつながるため禁止"
                "（guardrails 12.2）")
    return None


def check_gh(args: list[str]) -> str | None:
    positional: list[tuple[int, str]] = []
    i = 0
    while i < len(args) and len(positional) < 2:
        arg = args[i]
        if arg.startswith("-"):
            if arg in GH_FLAGS_WITH_VALUE:
                i += 1
        else:
            positional.append((i, arg))
        i += 1
    if not positional:
        return None
    index, group = positional[0]
    if group == "api":
        return check_gh_api(args[index + 1:])
    if len(positional) >= 2 and group in GH_WRITE and positional[1][1] in GH_WRITE[group]:
        sub = positional[1][1]
        if group in ("pr", "issue"):
            return (f"gh {group} {sub}（PR・Issue の作成と更新）はエージェントから実行しない（guardrails 12.2・"
                    "BL-077）。本文をファイルへ用意し、コマンド例をユーザーへ提示すること")
        if group == "alias":
            return "gh のエイリアスの定義は判定のすり抜けにつながるため禁止（guardrails 12.2）"
        return (f"gh {group} {sub} は GitHub 上を変更する操作のためエージェントから実行しない（guardrails 12.2）。"
                "コマンド例をユーザーへ提示すること")
    return None


def check_gh_api(args: list[str]) -> str | None:
    method = None
    body = False
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--":
            break
        if arg.startswith("--"):
            name, eq, value = arg.partition("=")
            if name in GH_API_LONG_WITH_VALUE and not eq:
                value = args[i + 1] if i + 1 < len(args) else ""
                i += 1
            if name == "--method":
                method = value
            elif name in ("--field", "--raw-field", "--input"):
                body = True
        elif arg.startswith("-") and len(arg) > 1:
            for k, c in enumerate(arg[1:]):
                if c in GH_API_SHORT_WITH_VALUE:
                    value = arg[k + 2:].removeprefix("=")
                    if not arg[k + 2:]:
                        value = args[i + 1] if i + 1 < len(args) else ""
                        i += 1
                    if c == "X":
                        method = value
                    elif c in "fF":
                        body = True
                    break
        i += 1
    if (method is not None and method.upper() != "GET") or (method is None and body):
        return ("gh api の書き込み（GET 以外のメソッド、または -f・-F・--input で本文を送る形）はエージェントから"
                "実行しない（guardrails 12.2）。読み取りは -X GET を付けるか本文を送らずに行うこと")
    return None


def shell_script(args: list[str]) -> tuple[str, str | None]:
    """bash -c 等の引数から、実行される文字列を取り出す。("cmd", 文字列) / ("stdin", None) / ("file", None)。"""
    i = 0
    while i < len(args):
        arg = args[i]
        if arg in ("-o", "+o", "-O", "+O", "--rcfile", "--init-file"):
            i += 2
            continue
        if arg == "--":
            return ("file", None) if i + 1 < len(args) else ("stdin", None)
        if arg.startswith("--"):
            i += 1
            continue
        if arg[:1] in "-+" and len(arg) > 1:
            if "c" in arg[1:]:
                return "cmd", (args[i + 1] if i + 1 < len(args) else "")
            i += 1
            continue
        return "file", None
    return "stdin", None


PS_PARAMS_WITH_VALUE = ("executionpolicy", "windowstyle", "outputformat", "inputformat", "configurationname",
                        "workingdirectory", "version", "psconsolefile", "custompipename", "settingsfile")


def powershell_script(name: str, args: list[str]) -> str | None:
    i = 0
    while i < len(args):
        arg = args[i]
        if arg.startswith("-") or arg.startswith("/"):
            param = arg.lstrip("-/").lower()
            if param and "command".startswith(param):
                return " ".join(args[i + 1:])
            if (param and "encodedcommand".startswith(param)) or param == "ec":
                value = args[i + 1] if i + 1 < len(args) else ""
                return base64.b64decode(value + "=" * (-len(value) % 4)).decode("utf-16-le", errors="replace")
            if param and "file".startswith(param):
                return None
            if len(param) >= 2 and any(p.startswith(param) for p in PS_PARAMS_WITH_VALUE):
                i += 1
            i += 1
            continue
        # 位置引数は Windows PowerShell ではコマンド、pwsh ではスクリプトファイル
        return " ".join(args[i:]) if name == "powershell" else None
    return None


def check_command(command: Command, dialect: str, cwd: str, state: dict, depth: int) -> str | None:
    words = strip_prefix(command.words, dialect)
    if not words:
        return None
    name = program_name(words[0])
    args = words[1:]
    if name == "git":
        return check_git(args, cwd, state, depth)
    if name == "gh":
        return check_gh(args)
    if name in SHELLS:
        kind, script = shell_script(args)
        if kind == "cmd":
            return evaluate(script or "", "bash", cwd, state, depth + 1)
        if kind == "stdin":
            for body in command.heredocs:
                reason = evaluate(body, "bash", cwd, state, depth + 1)
                if reason:
                    return reason
        return None
    if name == "eval":
        return evaluate(" ".join(args), "bash", cwd, state, depth + 1)
    if name in ("powershell", "pwsh"):
        script = powershell_script(name, args)
        return evaluate(script, "powershell", cwd, state, depth + 1) if script else None
    if name in ("invoke-expression", "iex"):
        rest = [a for a in args if not (a.startswith("-") and "command".startswith(a.lstrip("-").lower()))]
        return evaluate(" ".join(rest), "powershell", cwd, state, depth + 1)
    if name == "cmd":
        for k, arg in enumerate(args):
            if arg.lower() in ("/c", "/k", "/r"):
                return evaluate(" ".join(args[k + 1:]), "cmd", cwd, state, depth + 1)
    return None


def evaluate(command: str, dialect: str, cwd: str, state: dict, depth: int = 0) -> str | None:
    """拒否する理由を返す。通すときは None。dialect は bash・powershell・cmd。"""
    if depth > MAX_DEPTH:
        raise RecursionError("入れ子が深すぎる")
    for parsed in Parser(command, dialect, depth).parse():
        reason = check_command(parsed, dialect, cwd, state, depth)
        if reason:
            return reason
    return None


def decide(raw: bytes) -> tuple[int, str]:
    """標準入力の内容から (終了コード, 標準エラーへ出す文) を決める。"""
    try:
        data = json.loads(raw.decode("utf-8") or "{}")
        tool = data.get("tool_name", "")
        command = (data.get("tool_input") or {}).get("command") or ""
        cwd = data.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
    except Exception as exc:  # noqa: BLE001 - 入力が読めないときは判定できないので通す（deny は別に効く）
        return 1, f"guard-commands.py: 入力の JSON を読めなかった（判定をスキップ）: {exc}\n"
    if tool not in ("Bash", "PowerShell"):
        return 0, ""
    try:
        reason = evaluate(command, "powershell" if tool == "PowerShell" else "bash", cwd, {})
    except Exception as exc:  # noqa: BLE001 - 判定の不具合では、禁止操作の語を含むときだけ安全側で止める
        if FALLBACK_PATTERN.search(command):
            return 2, (f"[guard-commands] 拒否: hook の判定に失敗したので安全側で止めた（{type(exc).__name__}: {exc}）。"
                       "禁止操作でなければ、コマンドを単純な形に分けて実行し直すこと\n")
        return 0, ""
    if reason:
        return 2, f"[guard-commands] 拒否: {reason}\n"
    return 0, ""


def main() -> int:
    code, message = decide(sys.stdin.buffer.read())
    if message:
        sys.stderr.buffer.write(message.encode("utf-8"))
    return code


if __name__ == "__main__":
    sys.exit(main())
