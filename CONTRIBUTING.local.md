# コントリビューションガイド（リポジトリ固有）

本ファイルは `CONTRIBUTING.md`（配布元から取り込むたびに上書きされる共通規約）に対する、本リポジトリ固有の
具体化と追加です。ガードレール（`rules/`）と共通規約の禁止事項は緩められません。

## 規定ブランチ

- 規定ブランチ: `main`

## レビュー責任者

`CONTRIBUTING.md`「レビュー要件」の各担当に該当する担当者・チームです。`.github/CODEOWNERS` と一致させます。

| 担当 | 担当者・チーム |
| --- | --- |
| セキュリティ担当 | `@filderschoice` |
| プライバシー担当 | `@filderschoice` |
| プロダクトオーナー | `@filderschoice` |

## 自律ループ実行モードの採否

- 採否: 採用（`docs/guidelines/ADOPTION.md`「1.2 自律ループ実行モードの採否」の4資産がそろっている）

## 例外の追跡

- 追跡場所: `docs/records/managed/BACKLOG.md` へ理由・責任者・期限を付けて登録する

## ドキュメント構成（追加分）

`CONTRIBUTING.md`「ドキュメント構成」の表に無い、本リポジトリ固有のディレクトリです。

| パス | 役割 | 詳細 |
| --- | --- | --- |
| `docs/design/` | アプリの設計資料（MVP 設計・配布構成・UI デザイン設計書） | `MVP_DESIGN.md`、`DISTRIBUTION.md`、`UI_DESIGN.html` |
| `docs/` 直下 | 利用者・検証・ストア掲載向けの文書 | `PRIVACY.md`、`TESTING.md`、`PLAY_LISTING.md` |

## その他の開発プロセス

なし
