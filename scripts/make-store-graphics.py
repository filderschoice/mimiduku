"""Play Console 用の画像（アプリのアイコン 512x512、フィーチャー グラフィック 1024x500）を作る。

使い方（Pillow と、ヘッドレスで動く Edge または Chrome が必要）:
    python scripts/make-store-graphics.py

出力（docs/images/play/）:
    icon-512.png             512x512 の PNG。site/assets/icon.svg を描いたもの。角は丸めない（Play が丸める）
    feature-graphic.png      1024x500 の PNG。紺の背景にアイコンとアプリ名・説明文
アイコンを変えたら site/assets/icon.svg を直してから、このスクリプトを再実行する。
"""

from __future__ import annotations

import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ICON_SVG = ROOT / "site" / "assets" / "icon.svg"
OUT = ROOT / "docs" / "images" / "play"
BROWSERS = [
    "C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe",
    "C:/Program Files/Google/Chrome/Application/chrome.exe",
    "/usr/bin/chromium",
    "/usr/bin/google-chrome",
]
# アイコンは適応型アイコンの安全領域のぶん余白が広いため、ストア用は中央を拡大して使う
VIEW = "18 18 72 72"
NAVY = (30, 58, 95)
AMBER = (255, 160, 0)
WHITE = (255, 255, 255)
SUB = (205, 218, 235)
FONTS_BOLD = ["C:/Windows/Fonts/YuGothB.ttc", "C:/Windows/Fonts/meiryob.ttc", "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"]
FONTS_REG = ["C:/Windows/Fonts/YuGothM.ttc", "C:/Windows/Fonts/meiryo.ttc", "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"]


def font(candidates: list[str], size: int) -> ImageFont.FreeTypeFont:
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    raise SystemExit("日本語フォントが見つかりません")


def render_icon(size: int) -> Image.Image:
    browser = next((b for b in BROWSERS if Path(b).exists()), None)
    if browser is None:
        raise SystemExit("Edge または Chrome が見つかりません")
    svg = ICON_SVG.read_text(encoding="utf-8")
    html = (f"<html><body style='margin:0;background:#fff'><div style='width:{size}px;height:{size}px'>"
            f"{svg.replace('viewBox=\"0 0 108 108\" width=\"108\" height=\"108\"', f'viewBox=\"{VIEW}\" width=\"{size}\" height=\"{size}\"')}</div></body></html>")
    with tempfile.TemporaryDirectory() as tmp:
        page = Path(tmp) / "icon.html"
        shot = Path(tmp) / "icon.png"
        page.write_text(html, encoding="utf-8")
        subprocess.run(
            [browser, "--headless", "--disable-gpu", "--hide-scrollbars", f"--window-size={size},{size}",
             f"--screenshot={shot}", page.as_uri()],
            check=True, capture_output=True,
        )
        return Image.open(shot).convert("RGB").crop((0, 0, size, size))


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    icon = render_icon(512)
    icon.save(OUT / "icon-512.png")

    canvas = Image.new("RGB", (1024, 500), NAVY)
    draw = ImageDraw.Draw(canvas)
    mark = icon.resize((300, 300), Image.LANCZOS)
    canvas.paste(mark, (90, 100))
    draw.text((430, 150), "みみづく", font=font(FONTS_BOLD, 96), fill=WHITE)
    draw.rectangle((434, 275, 514, 283), fill=AMBER)
    draw.text((430, 305), "偽の警告・勝手な移動に", font=font(FONTS_REG, 36), fill=SUB)
    draw.text((430, 355), "気づいて知らせる見張り番", font=font(FONTS_REG, 36), fill=SUB)
    canvas.save(OUT / "feature-graphic.png")
    print("作成:", OUT / "icon-512.png", OUT / "feature-graphic.png")


if __name__ == "__main__":
    sys.exit(main())
