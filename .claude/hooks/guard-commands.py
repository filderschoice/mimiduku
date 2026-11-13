"""PreToolUse フック: guardrails 12.2 の禁止操作を、Bash・PowerShell ツールの実行前に拒否する（BL-077）。

`.claude/settings.json` の `permissions.deny` はコマンドの先頭一致でしか判定できず、`git -C . push` や
`git commit -m x --amend` のような書き方を取りこぼす。本フックはコマンドをシェルの語に分解し、
サブコマンドとオプションを語として判定して deny を補う。コミットメッセージやヒアドキュメントの本文に
「push」「rebase」などの単語が含まれるだけでは拒否しない。

拒否するもの:

- git push（`--force` を含む全形）、git rebase、git reset --hard、git commit --amend
- `--no-verify`（git commit の `-n` を含む）、git の `--force` 系の長いオプション
- git branch -d / -D / --delete、git tag -d / --delete（ブランチ・タグの削除）
- 規定ブランチ（main / master）上での git commit・merge・cherry-pick・revert・am
- gh pr / gh issue の作成・更新（create・edit・merge・close・reopen・comment・review・ready・delete など）

入力は Claude Code が標準入力へ渡す JSON（`tool_name`・`tool_input.command`・`cwd`）。
拒否するときは理由を標準エラーへ出して終了コード2で終わる（Claude へ理由が伝わる）。
通すときは何も出さず終了コード0（通常の権限判定へ進む）。フック自身の不具合は終了コード1
（ブロックしないエラー）とし、permissions.deny は引き続き効く。標準ライブラリのみで動作する。

手動での確かめ方（実際の git push 等は実行しない。JSON を渡すだけ）:

    echo '{"tool_name":"Bash","tool_input":{"command":"git -C . push"},"cwd":"."}' | python .claude/hooks/guard-commands.py
"""

import json
import os
import re
import subprocess
import sys

DEFAULT_BRANCHES = {"main", "master"}
COMMIT_LIKE = {"commit", "merge", "cherry-pick", "revert", "am"}
GH_WRITE = {
    "pr": {"create", "edit", "merge", "close", "reopen", "comment", "review", "ready", "lock", "unlock"},
    "issue": {"create", "edit", "close", "reopen", "comment", "delete", "transfer", "pin", "unpin", "lock", "unlock",
              "develop"},
}
# 値を1つ取る git のグローバルオプション（`git -C <path> push` の <path> をサブコマンドと誤認しないため）
GIT_GLOBAL_WITH_VALUE = {"-C", "-c", "--git-dir", "--work-tree", "--namespace", "--config-env", "--super-prefix",
                         "--exec-path"}
# 値を1つ取る git commit のオプション（`-m "-n"` のような値をオプションと誤認しないため）
COMMIT_LONG_WITH_VALUE = {"--message", "--file", "--reuse-message", "--reedit-message", "--template", "--author",
                          "--date", "--cleanup", "--fixup", "--squash", "--trailer", "--pathspec-from-file",
                          "--gpg-sign"}
COMMIT_SHORT_WITH_VALUE = set("mFCct")
GH_GLOBAL_WITH_VALUE = {"-R", "--repo"}
WRAPPERS = {"env", "command", "builtin", "exec", "nohup", "time", "sudo"}
SEPARATORS = {"&&", "||", ";", "|", "&", "\n", "(", ")", "$(", "`", "{", "}"}


def strip_heredocs(text: str) -> str:
    """ヒアドキュメント（bash の <<EOF）と PowerShell のヒア文字列（@' ... '@）の本文を除く。"""
    lines = text.split("\n")
    out: list[str] = []
    terminator = None
    strip_tabs = False
    in_here_string = None
    for line in lines:
        if terminator is not None:
            check = line.lstrip("\t") if strip_tabs else line
            if check.strip() == terminator:
                terminator = None
            continue
        if in_here_string is not None:
            if line.lstrip().startswith(in_here_string):
                in_here_string = None
                out.append(line.lstrip()[2:])
            continue
        match = re.search(r"<<(-?)\s*(['\"]?)([A-Za-z_][A-Za-z0-9_]*)\2", line)
        here = re.search(r"@(['\"])\s*$", line)
        if match:
            terminator = match.group(3)
            strip_tabs = match.group(1) == "-"
            out.append(line[: match.start()])
        elif here:
            in_here_string = here.group(1) + "@"
            out.append(line[: here.start()] + "''")
        else:
            out.append(line)
    return "\n".join(out)


def tokenize(text: str, powershell: bool) -> list[str]:
    """引用符を解釈して語に分ける。区切り演算子は独立した語として返す。"""
    tokens: list[str] = []
    buf: list[str] = []
    has_word = False
    i = 0
    escape = "`" if powershell else "\\"

    def flush() -> None:
        nonlocal buf, has_word
        if has_word:
            tokens.append("".join(buf))
        buf = []
        has_word = False

    while i < len(text):
        ch = text[i]
        if ch == escape and i + 1 < len(text):
            if text[i + 1] == "\n":
                i += 2
                continue
            buf.append(text[i + 1])
            has_word = True
            i += 2
            continue
        if ch == "'":
            end = text.find("'", i + 1)
            end = len(text) if end < 0 else end
            buf.append(text[i + 1:end])
            has_word = True
            i = end + 1
            continue
        if ch == '"':
            j = i + 1
            part: list[str] = []
            while j < len(text) and text[j] != '"':
                if text[j] == escape and j + 1 < len(text):
                    part.append(text[j + 1])
                    j += 2
                    continue
                part.append(text[j])
                j += 1
            buf.append("".join(part))
            has_word = True
            i = j + 1
            continue
        if ch == "#" and not has_word:
            end = text.find("\n", i)
            i = len(text) if end < 0 else end
            continue
        two = text[i:i + 2]
        if two in ("&&", "||", "$("):
            flush()
            tokens.append(two)
            i += 2
            continue
        if ch in ";|&\n(){}`":
            flush()
            tokens.append(ch)
            i += 1
            continue
        if ch in " \t\r":
            flush()
            i += 1
            continue
        buf.append(ch)
        has_word = True
        i += 1
    flush()
    return tokens


def split_commands(tokens: list[str]) -> list[list[str]]:
    commands: list[list[str]] = []
    current: list[str] = []
    for tok in tokens:
        if tok in SEPARATORS:
            if current:
                commands.append(current)
            current = []
        else:
            current.append(tok)
    if current:
        commands.append(current)
    return commands


def program_name(word: str) -> str:
    base = re.split(r"[\\/]", word)[-1].lower()
    return base[:-4] if base.endswith(".exe") else base


def strip_prefix(words: list[str]) -> list[str]:
    """環境変数の代入（FOO=bar）とラッパー（env・command 等）を先頭から除く。"""
    i = 0
    while i < len(words):
        if re.match(r"^[A-Za-z_][A-Za-z0-9_]*=", words[i]) or program_name(words[i]) in WRAPPERS:
            i += 1
            continue
        break
    return words[i:]


def current_branch(directory: str) -> str:
    try:
        result = subprocess.run(["git", "-C", directory, "branch", "--show-current"], capture_output=True,
                                text=True, timeout=10, check=False)
    except (OSError, subprocess.SubprocessError):
        return ""
    return result.stdout.strip() if result.returncode == 0 else ""


def commit_options(args: list[str]) -> set[str]:
    """git commit の引数から、値として消費されないオプションの集合を返す（短いオプションの束を展開する）。"""
    flags: set[str] = set()
    i = 0
    while i < len(args):
        arg = args[i]
        if arg == "--":
            break
        if arg.startswith("--"):
            name = arg.split("=", 1)[0]
            flags.add(name)
            if name in COMMIT_LONG_WITH_VALUE and "=" not in arg:
                i += 1
        elif arg.startswith("-") and len(arg) > 1:
            for k, c in enumerate(arg[1:]):
                if c in COMMIT_SHORT_WITH_VALUE:
                    if k == len(arg) - 2:
                        i += 1
                    break
                flags.add("-" + c)
        i += 1
    return flags


def check_git(args: list[str], cwd: str, state: dict) -> str | None:
    directory = cwd
    i = 0
    while i < len(args) and args[i].startswith("-"):
        opt = args[i].split("=", 1)[0]
        if opt == "-C" and i + 1 < len(args):
            directory = os.path.join(directory, args[i + 1])
        if opt in GIT_GLOBAL_WITH_VALUE and "=" not in args[i]:
            i += 1
        i += 1
    if i >= len(args):
        return None
    sub, rest = args[i], args[i + 1:]

    if sub == "commit":
        options = commit_options(rest)
    else:
        options = {a.split("=", 1)[0] for a in rest if a.startswith("-")}

    if sub == "push":
        return "git push はユーザーが実行する（guardrails 5.3・12.2）。コマンド例の提示にとどめること"
    if sub == "rebase":
        return "git rebase は公開済みコミットの改変にあたるため禁止（guardrails 12.2）。修正コミットを積むこと"
    if sub == "reset" and "--hard" in options:
        return "git reset --hard は禁止（guardrails 12.2）"
    if "--no-verify" in options or (sub == "commit" and "-n" in options):
        return "--no-verify など検証をバイパスするオプションは禁止（guardrails 12.2）"
    if any(o.startswith("--force") for o in options):
        return "--force 系のオプションは禁止（guardrails 12.2 の push・改変・削除の禁止を補う）"
    if sub == "commit" and "--amend" in options:
        return "git commit --amend は禁止（guardrails 12.2）。修正コミットを積むこと"
    if sub == "branch" and ("--delete" in options or any(
            o.startswith("-") and not o.startswith("--") and ("d" in o or "D" in o) for o in options)):
        return "ブランチの削除は禁止（guardrails 12.2）"
    if sub == "tag" and ("--delete" in options or any(
            o.startswith("-") and not o.startswith("--") and "d" in o for o in options)):
        return "タグの削除は禁止（guardrails 12.2）"
    if sub in ("checkout", "switch"):
        # 同じコマンド内で切り替えた先を覚える（`git checkout main && git commit` を見逃さないため）
        targets = [a for a in rest if not a.startswith("-")]
        if targets:
            state["branch"] = targets[0]
        return None
    if sub in COMMIT_LIKE:
        branch = state.get("branch") or current_branch(directory)
        if branch in DEFAULT_BRANCHES:
            return f"規定ブランチ（{branch}）へのコミットは禁止（guardrails 5.3・12.2）。作業ブランチで行うこと"
    return None


def check_gh(args: list[str]) -> str | None:
    i = 0
    while i < len(args) and args[i].startswith("-"):
        if args[i] in GH_GLOBAL_WITH_VALUE:
            i += 1
        i += 1
    words = args[i:]
    if len(words) >= 2 and words[0] in GH_WRITE:
        group = words[0]
        j = 1
        while j < len(words) and words[j].startswith("-"):
            j += 2 if words[j] in GH_GLOBAL_WITH_VALUE else 1
        if j < len(words) and words[j] in GH_WRITE[group]:
            return (f"gh {group} {words[j]}（PR・Issue の作成と更新）はエージェントから実行しない（guardrails 12.2・"
                    "BL-077）。本文をファイルへ用意し、コマンド例をユーザーへ提示すること")
    return None


def evaluate(command: str, cwd: str, powershell: bool) -> str | None:
    state: dict = {}
    for words in split_commands(tokenize(strip_heredocs(command), powershell)):
        words = strip_prefix(words)
        if not words:
            continue
        name = program_name(words[0])
        reason = None
        if name == "git":
            reason = check_git(words[1:], cwd, state)
        elif name == "gh":
            reason = check_gh(words[1:])
        if reason:
            return reason
    return None


def main() -> int:
    try:
        data = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
        tool = data.get("tool_name", "")
        if tool not in ("Bash", "PowerShell"):
            return 0
        command = (data.get("tool_input") or {}).get("command") or ""
        cwd = data.get("cwd") or os.environ.get("CLAUDE_PROJECT_DIR") or os.getcwd()
        reason = evaluate(command, cwd, tool == "PowerShell")
    except Exception as exc:  # noqa: BLE001 - フックの不具合でツールを止めない（deny は別に効く）
        sys.stderr.buffer.write(f"guard-commands.py の内部エラー（判定をスキップ）: {exc}\n".encode("utf-8"))
        return 1
    if reason:
        sys.stderr.buffer.write(f"[guard-commands] 拒否: {reason}\n".encode("utf-8"))
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
