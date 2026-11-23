# アプリ紹介ページ（site/）

みみづくを紹介する静的な 1 ページです。隣のリポジトリ ikumemo と sesami-wear の `site/` を雛形にしています。
ビルドは不要で、`index.html` をブラウザーで開くだけで表示できます。

## 構成

| ファイル | 内容 |
| --- | --- |
| `index.html` | 紹介ページ。文言は `README.md`（できること・プライバシー）と `docs/PLAY_LISTING.md` から取っている |
| `style.css` | 共通のスタイル。色はアイコンの紺・茶色・琥珀色から取り、ライト／ダークは OS の設定（`prefers-color-scheme`）に従う |
| `app.json` | アプリの概要（`schema: app-portfolio.v1`、ikumemo・sesami-wear と同じ形式） |
| `assets/icon.svg` | アプリのアイコン。`ic_launcher_foreground.xml` のパスと背景色（`ic_launcher_background.xml`）を写したもの。アイコンを変えたら合わせて直す |
| `assets/screenshots/` | 実機のスクリーンショット（ホーム・偽の警告・検知ログ・設定） |
| `assets/how-it-works.svg` | 仕組みの図解。実画面ではなく図解であることを `alt` に書いている |
| `.nojekyll` | GitHub Pages に Jekyll の変換をさせないための空ファイル |

## 方針

- HTML と CSS だけで作る。JavaScript・外部フォント・外部 CDN・アクセス解析を使わない。
- リンクは相対パスか GitHub の絶対 URL にし、`site/` の外を相対で参照しない。
- 機能・版・動作環境・配布状況を変えたときは、`index.html` と `app.json` も `README.md` と `docs/PLAY_LISTING.md` に合わせて直す。
- `app.json` の `status` は `development`（GitHub での配布のみ）。Play で公開したら `released` にし、`links.download` を Play の公開ページへ変える。
- 実機のスクリーンショット（`assets/screenshots/*.webp`、幅 540px）は `scripts/make-store-screenshots.py` で作る。実機の生の画像は
  ステータスバーに個人の通知アイコンが写るためリポジトリへ入れず、スクリプトが上下のバーを切り落とす。同じスクリプトが Play 用の
  説明文つきの画像（`docs/images/play/`）も作る。画面のギャラリーは同じ縦横比の画像だけを並べる。
  いまの画像は GitHub 版の release で撮ったもので、Play 版での撮り直しは BACKLOG の BL-100。「(debug)」の表示や個人の通知が写った画像は使わない。

## GitHub Pages での公開

`site/` の中身だけを `gh-pages` ブランチへ切り出して配信します（ikumemo・sesami-wear と同じ方法）。
`git push` は人が実行します（エージェントは実行しません）。

1. `site/` の変更を `main` へマージしたあと、`main` を最新にして `gh-pages` ブランチを作って push する。

   ```bash
   git checkout main
   git pull
   git subtree push --prefix site origin gh-pages
   ```

2. GitHub のリポジトリの Settings → Pages で、Source を「Deploy from a branch」、Branch を `gh-pages` の `/ (root)` にして保存する。
3. 数分後に <https://filderschoice.github.io/mimiduku/> を開き、アイコンと図解が表示されることを確かめる。

更新するときは、`main` で同じ `git subtree push` を実行します。`gh-pages` は直接編集しないでください
（早送りにならず push が失敗します）。失敗したときは `gh-pages` を `site/` の履歴で作り直します。

```bash
git push origin "$(git subtree split --prefix site main)":refs/heads/gh-pages --force
```
