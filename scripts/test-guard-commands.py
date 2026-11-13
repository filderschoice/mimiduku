"""`.claude/hooks/guard-commands.py` のテスト（BL-077・BL-082）。

フックへ Claude Code と同じ形の JSON を標準入力で渡し、終了コード（2 は拒否、0 は通過）を確かめる。
**禁止コマンドそのものは一切実行しない**（文字列としてフックへ渡すだけ）。規定ブランチ上のコミットの判定のために、
一時ディレクトリへ空のリポジトリを2つ作る（`git init` と `git symbolic-ref` だけを実行する）。

実行方法（リポジトリのルートで）:

    python scripts/test-guard-commands.py

失敗したケースを表示し、1件でも失敗すれば終了コード1で終わる。標準ライブラリのみで動作する。
"""

import base64
import importlib.util
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

HOOK = Path(__file__).resolve().parent.parent / ".claude" / "hooks" / "guard-commands.py"
DENY, ALLOW = 2, 0
ENCODED_PUSH = base64.b64encode("git push".encode("utf-16-le")).decode("ascii")

# (ツール, コマンド, 期待する終了コード)。cwd は作業ブランチ（work）の一時リポジトリ
CASES_WORK: list[tuple[str, str, int]] = [
    # --- 基本形（BL-077 から） ---
    ("Bash", "git push", DENY),
    ("Bash", "git push --force origin x", DENY),
    ("Bash", "git -C . push", DENY),
    ("Bash", "git --git-dir=.git push origin HEAD", DENY),
    ("Bash", "cd sub && git push", DENY),
    ("Bash", "git status; git push", DENY),
    ("Bash", "git send-pack origin HEAD", DENY),
    ("Bash", "git commit -m x --amend", DENY),
    ("Bash", "git commit --amend --no-edit", DENY),
    ("Bash", "git commit -m x --no-verify", DENY),
    ("Bash", "git commit -nm x", DENY),
    ("Bash", "git commit -n -m x", DENY),
    ("Bash", "git merge --no-verify feature", DENY),
    ("Bash", "git reset --hard HEAD~1", DENY),
    ("Bash", "git rebase main", DENY),
    ("Bash", "git branch -d main", DENY),
    ("Bash", "git branch -D feature master", DENY),
    ("Bash", "git branch --delete refs/heads/main", DENY),
    ("Bash", "git branch -D -- main", DENY),
    ("PowerShell", "git branch -D main", DENY),
    ("Bash", "git branch -df feature", DENY),
    ("Bash", "git branch --delete --force feature", DENY),
    ("Bash", "git tag -d v1", DENY),
    ("Bash", "git tag --delete v1", DENY),
    ("Bash", "git add --force build/x", DENY),
    ("Bash", "gh issue comment 1 --body x", DENY),
    ("Bash", "gh -R o/r pr merge 1", DENY),
    # --- 長いオプションの短縮形 ---
    ("Bash", "git commit --am -m x", DENY),
    ("Bash", "git commit --amen", DENY),
    ("Bash", "git commit --no-ver -m x", DENY),
    ("Bash", "git reset --har HEAD", DENY),
    ("Bash", "git branch --del main", DENY),
    ("Bash", "git tag --dele v1", DENY),
    ("Bash", "git add --forc build/x", DENY),
    # --- --gpg-sign が次の語を飲み込まない ---
    ("Bash", "git commit --gpg-sign --amend", DENY),
    # --- ヒアドキュメントの開始行の後続と、ヒアドキュメントの誤認 ---
    ("Bash", "cat <<EOF && git push\nbody\nEOF", DENY),
    ("Bash", "cat <<'EOF' | sh -c 'git push'\nbody\nEOF", DENY),
    ("Bash", 'echo "a << b" && git push', DENY),
    ("Bash", 'git commit -m "x <<EOF"\ngit push', DENY),
    ("Bash", 'cat <<<"text" && git push', DENY),
    ("Bash", "echo $((1<<3))\ngit push", DENY),
    ("Bash", "((x = 1 << 2))\ngit push", DENY),
    ("Bash", "cat <<EOF\n$(git push)\nEOF", DENY),
    ("Bash", "cat <<EOF\n`git push`\nEOF", DENY),
    # --- 制御構文 ---
    ("Bash", "if true; then git push; fi", DENY),
    ("Bash", "if git push; then echo ok; fi", DENY),
    ("Bash", "for b in a; do git push; done", DENY),
    ("Bash", "while false; do git rebase main; done", DENY),
    ("Bash", "! git push", DENY),
    ("Bash", "{ git push; }", DENY),
    ("Bash", "(git push)", DENY),
    # --- オプション付きのラッパー ---
    ("Bash", "env -i git push", DENY),
    ("Bash", "env -u HOME git push", DENY),
    ("Bash", "env FOO=1 git push", DENY),
    ("Bash", "env -S 'git push'", DENY),
    ("Bash", "FOO=1 git push", DENY),
    ("Bash", "command -p git push", DENY),
    ("Bash", "sudo -u root git push", DENY),
    ("Bash", "time -p git push", DENY),
    ("Bash", "timeout 10 git push", DENY),
    ("Bash", "nice -n 5 git push", DENY),
    ("Bash", "xargs -n1 git push", DENY),
    ("Bash", "exec git push", DENY),
    # --- リダイレクト ---
    ("Bash", "git >log.txt push", DENY),
    ("Bash", "git 2>/dev/null push", DENY),
    ("Bash", "git status > out.txt 2>&1 && git push", DENY),
    # --- コマンド置換・別のシェルへ渡す文字列 ---
    ("Bash", 'echo "$(git push)"', DENY),
    ("Bash", 'echo "`git push`"', DENY),
    ("Bash", "echo `git push`", DENY),
    ("Bash", "x=$(git push)", DENY),
    ("Bash", "cat <(git push)", DENY),
    ("Bash", 'bash -c "git push"', DENY),
    ("Bash", "sh -c 'git status && git push'", DENY),
    ("Bash", "bash -lc 'git push'", DENY),
    ("Bash", 'bash -c "bash -c \\"git push\\""', DENY),
    ("Bash", 'eval "git push"', DENY),
    ("Bash", "eval git push", DENY),
    ("Bash", "bash <<'EOF'\ngit status\ngit push\nEOF", DENY),
    ("Bash", 'cmd /c "git push"', DENY),
    ("Bash", 'powershell -NoProfile -Command "git push"', DENY),
    ("Bash", "pwsh -c 'git push'", DENY),
    ("Bash", f"powershell -EncodedCommand {ENCODED_PUSH}", DENY),
    # --- git -c と git config ---
    ("Bash", "git -c core.hooksPath=/dev/null commit -m x", DENY),
    ("Bash", "git -c core.hookspath= commit -m x", DENY),
    ("Bash", "git -c alias.p=push p", DENY),
    ("Bash", "git -c 'alias.x=!git push' x", DENY),
    ("Bash", "git -c alias.ci='commit --amend' ci", DENY),
    ("Bash", "git --config-env=alias.p=ENVVAR p", DENY),
    ("Bash", "git config alias.p push", DENY),
    ("Bash", "git config --global core.hooksPath /tmp/hooks", DENY),
    ("Bash", "git config set alias.p push", DENY),
    # --- ブランチ・タグの上書き ---
    ("Bash", "git branch -f feature HEAD~1", DENY),
    ("Bash", "git branch -M old new", DENY),
    ("Bash", "git tag -f v1", DENY),
    # --- 規定ブランチへ切り替えてからのコミット ---
    ("Bash", "git checkout main && git commit -m x", DENY),
    ("Bash", "git switch main && git commit -m x", DENY),
    ("Bash", "git checkout main -- && git commit -m x", DENY),
    # --- gh の書き込み ---
    ("Bash", "gh pr update-branch 1", DENY),
    ("Bash", "gh pr close 1", DENY),
    ("Bash", "gh pr reopen 1", DENY),
    ("Bash", "gh pr comment 1 --body x", DENY),
    ("Bash", "gh pr review 1 --approve", DENY),
    ("Bash", "gh pr ready 1", DENY),
    ("Bash", "gh issue create --title x --body x", DENY),
    ("Bash", "gh issue edit 1 --body x", DENY),
    ("Bash", "gh release create v1 --notes x", DENY),
    ("Bash", "gh release edit v1 --draft=false", DENY),
    ("Bash", "gh release delete v1 --yes", DENY),
    ("Bash", "gh workflow run ci.yml", DENY),
    ("Bash", "gh api repos/o/r/issues -f title=x", DENY),
    ("Bash", "gh api -X POST repos/o/r/issues", DENY),
    ("Bash", "gh api --method=PATCH repos/o/r", DENY),
    ("Bash", "gh api -XDELETE repos/o/r/git/refs/heads/x", DENY),
    ("Bash", "gh api repos/o/r/contents/x --input body.json", DENY),
    ("Bash", "gh api graphql -f query='query{viewer{login}}'", DENY),  # 承知している誤止め（POST で送るため）
    # --- PowerShell ---
    ("PowerShell", "git push", DENY),
    ("PowerShell", "& git.exe push", DENY),
    ("PowerShell", '& "C:\\Program Files\\Git\\cmd\\git.exe" push', DENY),
    ("PowerShell", "$out = git push 2>&1", DENY),
    ("PowerShell", "if ($true) { git push }", DENY),
    ("PowerShell", 'Invoke-Expression "git push"', DENY),
    ("PowerShell", "iex 'git push'", DENY),
    ("PowerShell", 'cmd /c "git status && git push"', DENY),
    ("PowerShell", 'echo "$(git push)"', DENY),
    ("PowerShell", "git commit -m x --amend", DENY),
    ("PowerShell", "gh pr merge 1", DENY),
    ("PowerShell", "gh issue create --title x", DENY),
    ("PowerShell", '$msg = @"\n$(git push)\n"@', DENY),
    ("PowerShell", "$msg = @'\nbody\n'@\ngit push", DENY),
    ("PowerShell", "bash -c 'git push'", DENY),

    # ===== 通すもの =====
    ("Bash", "git status", ALLOW),
    ("Bash", "git commit -m x", ALLOW),
    ("Bash", 'git commit -m "do not git push or rebase, avoid --no-verify and --amend"', ALLOW),
    ("Bash", "git commit -m \"$(cat <<'EOF'\nfeat: x (BL-082)\n\npush rebase --amend --no-verify\n\"quoted\" )\nEOF\n)\"",
     ALLOW),
    ("Bash", "git commit -F - <<'EOF'\ngit push\nEOF", ALLOW),
    ("Bash", "cat <<'EOF' > f.txt\n`git push`\n$(git push)\nEOF", ALLOW),
    ("Bash", "cat <<-'EOF'\n\tgit push\n\tEOF\ngit status", ALLOW),
    ("Bash", 'echo "use \\`git push\\` manually"', ALLOW),
    ("Bash", "echo 'git push' && echo \"git rebase\"", ALLOW),
    ("Bash", "# git push\ngit status", ALLOW),
    ("Bash", "git log --format=%H -5", ALLOW),
    ("Bash", "git log --oneline main..HEAD", ALLOW),
    ("Bash", "git log origin/HEAD..HEAD --oneline", ALLOW),
    ("Bash", "git diff main...HEAD --stat", ALLOW),
    ("Bash", "git branch --show-current", ALLOW),
    ("Bash", "git branch -a", ALLOW),
    ("Bash", "git branch -vv", ALLOW),
    ("Bash", "git branch feature", ALLOW),
    ("Bash", "git branch -m old new", ALLOW),
    # --- 規定ブランチ以外のローカルブランチの削除は通す（BL-087） ---
    ("Bash", "git branch -d feature", ALLOW),
    ("Bash", "git branch -D feature/a docs/b", ALLOW),
    ("Bash", "git branch --delete feature", ALLOW),
    ("Bash", "git branch --del feature", ALLOW),
    ("Bash", "git branch -D maintenance", ALLOW),
    ("PowerShell", "git branch -D feature", ALLOW),
    ("Bash", 'git tag -a v1 -m "msg -d -f"', ALLOW),
    ("Bash", "git tag -l", ALLOW),
    ("Bash", "git checkout main -- docs/x.md && git commit -m x", ALLOW),
    ("Bash", "git checkout main -- a b && git commit -m x", ALLOW),
    ("Bash", "git checkout main docs/x.md && git commit -m x", ALLOW),
    ("Bash", "git checkout -b feature/x && git commit -m x", ALLOW),
    ("Bash", "git show main:README.md", ALLOW),
    ("Bash", "git stash push -m x", ALLOW),
    ("Bash", "git reset --soft HEAD~1", ALLOW),
    ("Bash", "git reset HEAD file", ALLOW),
    ("Bash", "git commit --allow-empty -m x", ALLOW),
    ("Bash", "git commit --no-edit --no-gpg-sign -m x", ALLOW),
    ("Bash", 'git commit -m "-n"', ALLOW),
    ("Bash", "git commit -m --amend", ALLOW),
    ("Bash", "git -c core.quotepath=false log", ALLOW),
    ("Bash", "git config --get core.hooksPath", ALLOW),
    ("Bash", "git config alias.p", ALLOW),
    ("Bash", "git config --get-regexp alias", ALLOW),
    ("Bash", "git config user.name", ALLOW),
    ("Bash", "git add -A && python scripts/scan-secrets.py", ALLOW),
    ("Bash", "echo $((1<<3))", ALLOW),
    ("Bash", 'echo "a << b"', ALLOW),
    ("Bash", 'cat <<<"push"', ALLOW),
    ("Bash", "python - <<'PYEOF'\nprint('git push')\nPYEOF", ALLOW),
    ("Bash", "./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain", ALLOW),
    ("Bash", 'npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml', ALLOW),
    ("Bash", "python scripts/validate-records.py", ALLOW),
    ("Bash", "if git diff --quiet; then echo clean; fi", ALLOW),
    ("Bash", "env -i git status", ALLOW),
    ("Bash", "sudo -u root git log", ALLOW),
    ("Bash", 'bash -c "git status"', ALLOW),
    ("Bash", "sh scripts/x.sh push", ALLOW),
    ("Bash", 'eval "git status"', ALLOW),
    ("Bash", 'cmd /c "git status"', ALLOW),
    ("Bash", "date \"+%Y-%m-%d %H:%M\"", ALLOW),
    # Skill dependabot-alert-triage・pr-create が使う gh の読み取り
    ("Bash", "gh --version && git remote -v", ALLOW),
    ("Bash", "gh api \"repos/o/r/dependabot/alerts?state=open&per_page=100\" --paginate \\\n"
             "  -q '.[] | [.number,.security_advisory.severity,.dependency.package.name] | @tsv' > alerts.tsv", ALLOW),
    ("Bash", "gh pr view 12 --json url,title,body -q .url", ALLOW),
    ("Bash", "gh api -X GET search/issues -f q=repo:o/r", ALLOW),
    ("Bash", "gh api --method GET repos/o/r", ALLOW),
    ("Bash", "gh api repos/o/r/pulls/1", ALLOW),
    ("Bash", "gh pr list; gh pr diff 1; gh pr checks 1; gh pr status", ALLOW),
    ("Bash", "gh issue view 1 && gh issue list", ALLOW),
    ("Bash", "gh release view v1 && gh release list && gh release download v1", ALLOW),
    ("Bash", "gh run view 1 && gh run list && gh workflow view ci && gh workflow list", ALLOW),
    # PR の作成・本文の編集はエージェントへ許可済み（自律ループ実行モード中は手順で実行しない）
    ("Bash", "gh pr create --base main --title x --body-file f.md", ALLOW),
    ("Bash", "gh pr new --title x --body-file f.md", ALLOW),
    ("Bash", "gh -R o/r pr edit 12 --body-file f.md", ALLOW),
    ("PowerShell", "gh pr create --base main --title x --body-file f.md", ALLOW),
    ("PowerShell", "gh pr edit 12 --body-file f.md", ALLOW),
    ("PowerShell", "git status", ALLOW),
    ("PowerShell", 'git commit -m "push `git rebase` --amend"', ALLOW),
    ("PowerShell", "$msg = @'\ngit push\n$(git push)\n'@\ngit commit -m $msg", ALLOW),
    ("PowerShell", "Get-ChildItem | Select-Object Name", ALLOW),
    ("PowerShell", "& git.exe log --oneline", ALLOW),
    ("PowerShell", "gh pr view 1", ALLOW),
    ("PowerShell", "./gradlew assembleDebug", ALLOW),
    ("Read", "git push", ALLOW),  # Bash・PowerShell 以外は対象外
]

# cwd が規定ブランチ（main）の一時リポジトリ
CASES_MAIN: list[tuple[str, str, int]] = [
    ("Bash", "git commit -m x", DENY),
    ("Bash", "git merge feature", DENY),
    ("Bash", "git merge --ff-only --no-ff feature", DENY),
    ("Bash", "git merge --ff-only --ff feature", DENY),
    ("Bash", "git merge --no-ff feature", DENY),
    ("Bash", "git merge --ff feature", DENY),
    ("Bash", "git merge --ff-only --force origin/main", DENY),
    ("Bash", "git merge --ff-only origin/main", ALLOW),
    ("Bash", "git merge --no-ff --ff-only origin/main", ALLOW),
    ("Bash", "git merge --ff-o origin/main", ALLOW),
    ("Bash", "git merge -q --ff-only origin/main && git branch -vv", ALLOW),
    ("PowerShell", "git merge --ff-only origin/main", ALLOW),
    ("Bash", "git cherry-pick abc", DENY),
    ("PowerShell", "git commit -m x", DENY),
    ("Bash", "git status && git log", ALLOW),
    ("Bash", "git checkout -b feature/x && git commit -m x", ALLOW),
    ("Bash", "git switch -c feature/x && git commit -m x", ALLOW),
]


def make_repo(root: Path, branch: str) -> Path:
    path = root / branch
    path.mkdir()
    subprocess.run(["git", "init", "-q", str(path)], check=True)
    subprocess.run(["git", "-C", str(path), "symbolic-ref", "HEAD", f"refs/heads/{branch}"], check=True)
    return path


def run_hook(tool: str, command: str, cwd: Path) -> tuple[int, str]:
    payload = json.dumps({"tool_name": tool, "tool_input": {"command": command}, "cwd": str(cwd)})
    result = subprocess.run([sys.executable, str(HOOK)], input=payload.encode("utf-8"), capture_output=True,
                            timeout=60, check=False)
    return result.returncode, result.stderr.decode("utf-8", errors="replace").strip()


def check_fallback() -> list[str]:
    """判定中の例外と、JSON が読めない場合の扱い（フックを読み込んで evaluate を差し替える）。"""
    sys.dont_write_bytecode = True  # .claude/hooks/__pycache__ を作らない
    spec = importlib.util.spec_from_file_location("guard_commands", HOOK)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)

    def broken(*_args, **_kwargs):
        raise ValueError("テスト用の例外")

    module.evaluate = broken
    failures = []
    expectations = [
        (json.dumps({"tool_name": "Bash", "tool_input": {"command": "git push"}}).encode(), DENY),
        (json.dumps({"tool_name": "Bash", "tool_input": {"command": "git commit -m x"}}).encode(), DENY),
        (json.dumps({"tool_name": "PowerShell", "tool_input": {"command": "gh pr view 1"}}).encode(), DENY),
        (json.dumps({"tool_name": "Bash", "tool_input": {"command": "ls -la"}}).encode(), ALLOW),
        (b"{not json", 1),
    ]
    for raw, expected in expectations:
        code, message = module.decide(raw)
        if code != expected:
            failures.append(f"例外時 期待{expected} 実際{code}: {raw!r} {message}")
        elif expected == DENY and "安全側" not in message:
            failures.append(f"例外時の理由に「安全側」が無い: {message}")
    return failures


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    root =Path(tempfile.mkdtemp(prefix="guard-test-"))
    failures: list[str] = []
    total = 0
    try:
        work = make_repo(root, "work")
        main_repo = make_repo(root, "main")
        for cases, cwd in ((CASES_WORK, work), (CASES_MAIN, main_repo)):
            for tool, command, expected in cases:
                total += 1
                code, stderr = run_hook(tool, command, cwd)
                if code != expected:
                    failures.append(f"[{tool}] 期待{expected} 実際{code}: {command!r} {stderr}")
        # 別ディレクトリの規定ブランチを -C で指定する形
        total += 1
        code, stderr = run_hook("Bash", f'git -C "{main_repo.as_posix()}" commit -m x', work)
        if code != DENY:
            failures.append(f"-C で main のリポジトリを指定したコミットが通った: {code} {stderr}")
        fallback = check_fallback()
        total += 5
        failures.extend(fallback)
    finally:
        shutil.rmtree(root, ignore_errors=True)
    deny = sum(1 for cases in (CASES_WORK, CASES_MAIN) for c in cases if c[2] == DENY) + 1 + 3
    print(f"guard-commands テスト: {total}件（拒否を期待 {deny}件・通過を期待 {total - deny}件）、失敗 {len(failures)}件")
    for failure in failures:
        print("  失敗:", failure)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
