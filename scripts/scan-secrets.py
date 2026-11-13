"""コミット前の秘密情報スキャン（品質ゲート）。

guardrails 12.1 の「コミット前に秘密情報スキャンを実行し、資格情報の混入がないことを確認する」を
目視ではなく機械的に行う。既定ではステージ済みの差分（`git diff --cached`）の追加行と、
追加・変更・改名されたファイルのパスを検査する。

検出するもの:

- 秘密鍵の PEM ヘッダ（BEGIN ... PRIVATE KEY の行）
- AWS のアクセスキー ID の形式、`aws_secret_access_key` への代入
- GitHub のトークンの形式（`ghp_` などの接頭辞つき、fine-grained の `github_pat_`）
- Google の API キーの形式（`AIza` で始まる39文字）
- storePassword・keyPassword・password への文字列リテラルの代入
  （設定ファイル形式の拡張子では、引用符の無い値の代入も対象）
- ファイル名が `.jks`・`.keystore`・`local.properties`・`.env`・`.env.*` のファイル（`.gitignore` と同じ対象）

検出したらファイル名・行番号・規則名を出力し、値そのものは出力しない（ログへの再露出を防ぐ）。
正規表現は文字クラスや連結で組み立てており、本スクリプト自身の追加行には一致しない。

使い方:

    git add <ファイル>
    python scripts/scan-secrets.py                      # ステージ済みの差分を検査する
    python scripts/scan-secrets.py --include-unstaged   # 未ステージの変更と未追跡ファイルも含める
    python scripts/scan-secrets.py --range main...HEAD  # コミット範囲（ブランチ全体）を検査する

`--range` は `git diff <範囲>` に渡す（`main...HEAD` は main との分岐点から HEAD までの差分）。`git add` できない
品質ゲートの実行担当（quality-gate-runner）がフル検証でブランチ全体を確かめるときに使う（BL-083）。

終了コードは、検出なしで0、検出ありで1、git の実行に失敗したら2。標準ライブラリのみで動作する。
誤検出した場合は、値を環境変数や `local.properties` から読む形へ書き換えるのが原則（本スクリプトに除外指定の仕組みは無い）。
"""

import argparse
import io
import os
import re
import subprocess
import sys

# 内容の規則（規則名, 正規表現）。値を出力しないため、規則名だけで何が見つかったか分かる名前にする。
CONTENT_RULES = [
    ("秘密鍵のPEMヘッダ", re.compile("-{5}BEGIN [A-Z0-9 ]*PRIVATE KEY( BLOCK)?-{5}")),
    ("AWSアクセスキーID", re.compile(r"\b(AKIA|ASIA)[0-9A-Z]{16}\b")),
    ("AWSシークレットキーの代入", re.compile(r"(?i)aws_secret_access_key\s*[:=]\s*\S")),
    ("GitHubトークン", re.compile(r"\bgh[pousr]_[A-Za-z0-9]{36,}\b")),
    ("GitHubトークン(fine-grained)", re.compile(r"\bgithub_pat_[A-Za-z0-9_]{22,}")),
    ("GoogleのAPIキー", re.compile(r"\bAIza[0-9A-Za-z_\-]{35}")),
    (
        "パスワードへのリテラル代入",
        re.compile(r"""(?i)\b(store|key)?password["']?\s*[:=]\s*["'][^"'\s]+["']"""),
    ),
]

# 設定ファイル形式では値に引用符が付かないため、引用符の無い代入も検出する。
# Kotlin 等のソースで `password = 変数` を誤検出しないよう、対象を拡張子で絞る。
CONFIG_SUFFIXES = (".properties", ".ini", ".cfg", ".conf", ".toml", ".yml", ".yaml", ".env")
UNQUOTED_PASSWORD = re.compile(r"""(?i)^\s*(store|key)?password\s*[:=]\s*[^\s"'$%{]""")

# ファイル名の規則。`.gitignore` の資格情報の節と揃える。
FILE_NAME_RULES = [
    ("鍵ストアのファイル", re.compile(r"\.(jks|keystore)$", re.I)),
    ("local.properties", re.compile(r"^local\.properties$")),
    ("環境変数ファイル", re.compile(r"^\.env(\..+)?$")),
]

HUNK_HEADER = re.compile(r"^@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")


def run_git(args: list[str]) -> str:
    """git を実行して標準出力を返す。日本語のパスをそのまま得るため core.quotepath を切る。"""
    result = subprocess.run(
        ["git", "-c", "core.quotepath=false", *args],
        capture_output=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        # git の使い方の全文が続くことがあるため、先頭の1行だけを出す。
        first_line = (result.stderr.strip().splitlines() or ["(出力なし)"])[0]
        raise RuntimeError(f"git {' '.join(args)} が失敗しました: {first_line}")
    return result.stdout


def is_config_file(path: str) -> bool:
    name = os.path.basename(path).lower()
    return name.endswith(CONFIG_SUFFIXES) or name.startswith(".env")


def scan_line(path: str, line_no: int, text: str, findings: list[str]) -> None:
    for rule_name, pattern in CONTENT_RULES:
        if pattern.search(text):
            findings.append(f"{path}:{line_no}: {rule_name}")
    if is_config_file(path) and UNQUOTED_PASSWORD.search(text):
        findings.append(f"{path}:{line_no}: パスワードへの代入（設定ファイル）")


def scan_diff(diff_text: str, findings: list[str]) -> None:
    """unified diff（-U0）の追加行を検査する。行番号は変更後のファイルでの位置。"""
    path = None
    line_no = 0
    for line in diff_text.splitlines():
        if line.startswith("+++ "):
            target = line[4:]
            path = None if target == "/dev/null" else target[2:] if target.startswith("b/") else target
            continue
        if line.startswith("--- "):
            continue
        header = HUNK_HEADER.match(line)
        if header:
            line_no = int(header.group(1))
            continue
        if path is None:
            continue
        if line.startswith("+"):
            scan_line(path, line_no, line[1:], findings)
            line_no += 1
        elif line.startswith(" "):
            line_no += 1


def scan_untracked_file(path: str, findings: list[str]) -> None:
    try:
        with open(path, encoding="utf-8", errors="replace") as handle:
            for index, text in enumerate(handle, start=1):
                scan_line(path, index, text.rstrip("\n"), findings)
    except OSError as error:
        findings.append(f"{path}: 読み込めませんでした（{error.strerror}）")


def scan_file_names(paths: list[str], findings: list[str]) -> None:
    for path in paths:
        name = os.path.basename(path)
        for rule_name, pattern in FILE_NAME_RULES:
            if pattern.search(name):
                findings.append(f"{path}: {rule_name}（バージョン管理へ入れないファイル名）")


def main() -> int:
    parser = argparse.ArgumentParser(description="コミット前の秘密情報スキャン")
    parser.add_argument(
        "--include-unstaged",
        action="store_true",
        help="未ステージの変更（HEAD との差分）と未追跡ファイルも検査する",
    )
    parser.add_argument(
        "--range",
        metavar="範囲",
        help="ステージ済みの差分の代わりに、コミット範囲（例 main...HEAD）の差分を検査する",
    )
    options = parser.parse_args()
    if options.range is not None and (options.include_unstaged or not options.range or options.range.startswith("-")):
        print("--range にはコミット範囲（例 main...HEAD）を指定し、--include-unstaged とは併用しない", file=sys.stderr)
        return 2

    findings: list[str] = []
    diff_base = ["diff", "--no-color", "--no-ext-diff", "--diff-filter=ACMR"]
    if options.range is not None:
        diff_base += [options.range]
    else:
        diff_base += ["HEAD"] if options.include_unstaged else ["--cached"]
    try:
        scan_diff(run_git([*diff_base, "-U0", "--"]), findings)
        changed = run_git([*diff_base, "--name-only", "--"]).splitlines()
        if options.include_unstaged:
            untracked = run_git(["ls-files", "--others", "--exclude-standard"]).splitlines()
            for path in untracked:
                scan_untracked_file(path, findings)
            changed += untracked
        scan_file_names(changed, findings)
    except RuntimeError as error:
        print(error, file=sys.stderr)
        return 2

    if findings:
        print(f"秘密情報の疑い: {len(findings)} 件（値は表示しません）")
        for finding in findings:
            print(f"  {finding}")
        return 1
    if options.range is not None:
        scope = f"コミット範囲 {options.range} "
    else:
        scope = "ステージ済み・未ステージ・未追跡" if options.include_unstaged else "ステージ済み"
    print(f"秘密情報スキャン: 検出なし（対象: {scope}の変更）")
    return 0


if __name__ == "__main__":
    # Windows のコンソールでも日本語を出せるよう、標準出力を UTF-8 に固定する。
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")
    sys.exit(main())
