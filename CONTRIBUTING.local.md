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
| `.claude/agents/` | Claude Code のサブエージェント定義（読み取り中心。git の操作はしない） | `quality-gate-runner.md`（品質ゲートの実行と合否の要約。haiku）、`diff-reviewer.md`（本アプリ固有の観点での差分点検。sonnet） |
| `.claude/hooks/` | `.claude/settings.json` から呼ぶ Claude Code の hooks（Python・標準ライブラリのみ） | `guard-commands.py`（禁止操作の拒否）、`validate-records-after-edit.py`（記録ファイル編集後の検証）。説明は `CLAUDE.md`「記録ファイルの権限設定と禁止操作の強制」 |

## その他の開発プロセス

### 配布物への固有の追記

`docs/guidelines/ADOPTION.md`「3. 適用方式」で取り込む配布物（配布スキルと `.claude/settings.json`）のうち、次の箇所は本リポジトリ固有の追記です。
配布元から取り込むときは配布元版で上書きせず、これらの追記を残すようにマージします。

| ファイル | 固有の追記 | 理由 |
| --- | --- | --- |
| `.claude/skills/autonomous-loop/SKILL.md` | イテレーション手順7の `scripts/scan-secrets.py` の実行（BL-064） | 秘密情報スキャンの手段がリポジトリ固有のため |
| `.claude/settings.json`（マージ対象） | 品質ゲートの `permissions.allow`、`permissions.deny`、`hooks` | ADOPTION.md は `permissions.allow` のマージだけを述べるため、deny と hooks も残すことを明記する（BL-077） |
| `.claude/skills/pr-create/SKILL.md` | 手順1・4の「本リポジトリ固有」の箇条（`gh pr create` 等はユーザーが実行する） | `.claude/settings.json` の deny と hooks で拒否しているため（BL-077） |
