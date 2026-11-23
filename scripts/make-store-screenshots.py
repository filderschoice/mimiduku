"""実機のスクリーンショットから、Play 掲載用の画像（説明文つき）とサイト用の画像（説明文なし）を作る。

使い方（Pillow が必要）:
    python scripts/make-store-screenshots.py <素材のフォルダ>

素材のフォルダには、実機で撮った生のスクリーンショット（Pixel 8 Pro の 1344x2992）を次の名前で置く。
    home_raw.png     ホーム（見張っています）
    warn_raw.png     偽の警告を検知した画面（警告バナーが出ている）
    external_raw.png 勝手な外部移動の確認バナー（電卓の前面でタップなしにブラウザを開く）
    log_raw.png      検知ログ
    settings_raw.png 設定
    badge_idle_raw.png  バッジを表示した状態で、警告の無いページ（バッジが眠っている）
    badge_alert_raw.png バッジを表示した状態で、警告を検知した直後（バッジの目が開く）

生の画像は、ステータスバーに個人の通知アイコンが写るため、リポジトリへは入れない（素材のフォルダは作業用の場所に置く）。
このスクリプトが上下のバー（ステータスバー・ナビゲーションバー）を切り落とす。

出力:
    docs/images/play/screenshots/<掲載順の番号>-<名前>.png  1080x2160（Play の画像の縦横比 2:1 以内）。上に短い説明文
    site/assets/screenshots/<名前>.webp   幅 540。説明文なし（badge_* は site/assets/screenshots/badge-*.webp でサイトのみ）
"""

from __future__ import annotations

import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
PLAY_DIR = ROOT / "docs" / "images" / "play" / "screenshots"
SITE_DIR = ROOT / "site" / "assets" / "screenshots"

# 1344x2992 の画面から切り落とす上下の幅（ステータスバーとジェスチャーナビのバー）
CROP_TOP = 140
CROP_BOTTOM = 2900

NAVY = (30, 58, 95)
AMBER = (255, 160, 0)
WHITE = (255, 255, 255)
SUB = (205, 218, 235)

CANVAS = (1080, 2160)
SHOT_WIDTH = 900
SHOT_TOP = 380

# 名前、説明文（短い見出し、補足）
# 掲載順（Play のファイル名の番号になる）
SHOTS = [
    ("home", "悪質な広告に、先に気づく", "子供の端末を、やさしく見守ります"),
    ("warn", "偽の警告に気づいて知らせます", "「ウイルスに感染」などの文言を見つけます"),
    ("external", "勝手な移動に気づいて確認します", "「もどる」か「このまま進む」を選べます"),
    ("log", "気づいた記録が残ります", "日時・アプリ名・種類だけを端末の中に保存"),
    ("settings", "こども・おとなモードと保護者 PIN", "使う人に合わせて知らせ方を選べます"),
]

# サイト専用（Play 用の説明文つき画像は作らない）。バッジを表示した状態で、ブラウザの上に出るバッジを撮る
SITE_ONLY = ["badge_idle", "badge_alert"]

FONT_CANDIDATES_BOLD = [
    "C:/Windows/Fonts/YuGothB.ttc",
    "C:/Windows/Fonts/meiryob.ttc",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
    "/System/Library/Fonts/ヒラギノ角ゴシック W6.ttc",
]
FONT_CANDIDATES_REG = [
    "C:/Windows/Fonts/YuGothM.ttc",
    "C:/Windows/Fonts/meiryo.ttc",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    "/System/Library/Fonts/ヒラギノ角ゴシック W4.ttc",
]


def load_font(candidates: list[str], size: int) -> ImageFont.FreeTypeFont:
    for path in candidates:
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    raise SystemExit("日本語フォントが見つかりません。FONT_CANDIDATES に追加してください")


def trimmed(src: Path) -> Image.Image:
    im = Image.open(src).convert("RGB")
    if im.size != (1344, 2992):
        raise SystemExit(f"{src.name}: 1344x2992 ではありません（{im.size}）")
    return im.crop((0, CROP_TOP, im.width, CROP_BOTTOM))


def rounded_top(im: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width - 1, im.height + radius), radius, fill=255)
    out = im.convert("RGBA")
    out.putalpha(mask)
    return out


def centered(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.FreeTypeFont, y: int, fill) -> None:
    width = draw.textlength(text, font=font)
    draw.text(((CANVAS[0] - width) / 2, y), text, font=font, fill=fill)


def make_play(shot: Image.Image, title: str, sub: str) -> Image.Image:
    canvas = Image.new("RGB", CANVAS, NAVY)
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((CANVAS[0] // 2 - 40, 300, CANVAS[0] // 2 + 40, 308), fill=AMBER)
    title_font = load_font(FONT_CANDIDATES_BOLD, 70)
    # 長い見出しは枠に収まるまで文字を小さくする
    size = 70
    while draw.textlength(title, font=title_font) > CANVAS[0] - 100 and size > 40:
        size -= 2
        title_font = load_font(FONT_CANDIDATES_BOLD, size)
    centered(draw, title, title_font, 130, WHITE)
    centered(draw, sub, load_font(FONT_CANDIDATES_REG, 38), 230, SUB)
    height = round(shot.height * SHOT_WIDTH / shot.width)
    scaled = rounded_top(shot.resize((SHOT_WIDTH, height), Image.LANCZOS), 48)
    canvas.paste(scaled, ((CANVAS[0] - SHOT_WIDTH) // 2, SHOT_TOP), scaled)
    return canvas


def make_site(shot: Image.Image) -> Image.Image:
    height = round(shot.height * 540 / shot.width)
    return shot.resize((540, height), Image.LANCZOS)


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    src_dir = Path(sys.argv[1])
    PLAY_DIR.mkdir(parents=True, exist_ok=True)
    SITE_DIR.mkdir(parents=True, exist_ok=True)
    for number, (name, title, sub) in enumerate(SHOTS, start=1):
        shot = trimmed(src_dir / f"{name}_raw.png")
        make_play(shot, title, sub).save(PLAY_DIR / f"{number:02d}-{name}.png", optimize=True)
        make_site(shot).save(SITE_DIR / f"{name}.webp", quality=88, method=6)
        print(f"{name}: ok")
    for name in SITE_ONLY:
        make_site(trimmed(src_dir / f"{name}_raw.png")).save(
            SITE_DIR / f"{name.replace('_', '-')}.webp", quality=88, method=6
        )
        print(f"{name}: ok（サイトのみ）")


if __name__ == "__main__":
    main()
