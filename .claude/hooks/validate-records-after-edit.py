"""PostToolUse フック: 記録ファイルを編集した直後に `scripts/validate-records.py` を実行する（BL-077）。

対象は `docs/records/managed/` 配下の BACKLOG.md・EXECUTE.md・DESIGN.md を Edit・Write したときだけで、
それ以外のファイルの編集では検証を走らせずに終了する（毎回の編集に重い処理を足さないため）。

検証に失敗したら、validate-records.py の出力を標準エラーへ出して終了コード2で終わる。PostToolUse では
編集は取り消されないが、標準エラーが Claude へ伝わり、YAML の破損や `依存` の不整合をその場で直せる。
複数回の編集で整える途中（項目を消した直後で、他の `依存` をまだ直していない等）でも失敗として伝わるため、
一連の編集を終えた時点の結果で判断する。標準ライブラリのみで動作する（検証自体は PyYAML が必要）。
"""

import json
import os
import re
import subprocess
import sys

TARGET = re.compile(r"(^|/)docs/records/managed/(BACKLOG|EXECUTE|DESIGN)\.md$")


def main() -> int:
    try:
        data = json.loads(sys.stdin.buffer.read().decode("utf-8") or "{}")
    except ValueError:
        return 0
    if data.get("tool_name") not in ("Edit", "Write", "MultiEdit"):
        return 0
    path = ((data.get("tool_input") or {}).get("file_path") or "").replace("\\", "/")
    if not TARGET.search(path):
        return 0
    root = os.environ.get("CLAUDE_PROJECT_DIR") or os.path.dirname(os.path.dirname(os.path.dirname(
        os.path.abspath(__file__))))
    try:
        result = subprocess.run([sys.executable, os.path.join("scripts", "validate-records.py")], cwd=root,
                                capture_output=True, timeout=60, check=False,
                                env={**os.environ, "PYTHONIOENCODING": "utf-8"})
    except (OSError, subprocess.SubprocessError) as exc:
        sys.stderr.buffer.write(f"validate-records.py を実行できなかった: {exc}\n".encode("utf-8"))
        return 1
    if result.returncode == 0:
        return 0
    output = (result.stdout + result.stderr).decode("utf-8", errors="replace").strip()
    message = ("[validate-records-after-edit] 記録ファイルの検証に失敗した（docs/records/spec/FORMAT.md を参照して直すこと）\n"
               + output + "\n")
    sys.stderr.buffer.write(message.encode("utf-8"))
    return 2


if __name__ == "__main__":
    sys.exit(main())
