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

### 品質ゲートの段階

`CLAUDE.md`「本リポジトリの品質ゲート定義」の「段階」列の根拠と運用です（BL-079）。毎イテレーションのフル検証は時間が
かかるため、自律ループでは途中のイテレーションを軽い検証にとどめ、全ゲートはループの最後と PR 作成前に1回実行します。

実測値（2026-11-13、Windows・Gradle デーモン起動済み・増分ビルド、`--rerun-tasks` なし。main の Kotlin ファイルへ
コメント1行を足した直後に各コマンドを単独で実行）:

| コマンド | 変更なし | 変更直後 |
| --- | --- | --- |
| `./gradlew ktlintCheck` | 4.5秒 | 11.3秒 |
| `./gradlew detekt` | 3.8秒 | 4.4秒 |
| `./gradlew lintGithubDebug lintPlayDebug` | 4.1秒 | 33.3秒 |
| `./gradlew assembleDebug` | 5.6秒 | 12.9秒 |
| `./gradlew testGithubDebugUnitTest testPlayDebugUnitTest` | 3.6秒 | 21.3秒 |
| `./gradlew testGithubDebugUnitTest`（GitHub 版のみ） | 3.1秒 | 5.8秒 |
| 軽い検証（`--continue ktlintCheck detekt testGithubDebugUnitTest`、1回の起動） | － | 12.2〜20.6秒 |

参考: テストを強制再実行した場合（`--rerun`）は GitHub 版 10.1秒・Play 版 11.1秒、`lintGithubDebug --rerun` は 23.3秒。
変更直後のフル検証は合計約83秒で、支配的なのは Android Lint（約33秒）、次いで両方の版の単体テスト（約21秒）。

振り分けの理由:

- ktlint と detekt は軽く、指摘は後から直すほど差分が広がるため毎回のコード変更で実行する
- 単体テストは GitHub 版だけにする。GitHub 版は `mobile/src/main` に加えて `mobile/src/github` もコンパイルするため、
  1つの版でコンパイルとテストの対象が最も広い。コンパイルエラーもこのタスクで検出される
- Android Lint と assembleDebug（リソース・マニフェスト・Play 版のビルド）は最後に回す。所要時間の半分近くを占める
- 3つを1回の Gradle 起動にまとめるのは、構成フェーズを1回で済ませるため。`--continue` で全タスクの失敗をまとめて出す

軽い検証で変更が実際にコンパイル・テストされたことの確かめ方（up-to-date の素通りを見逃さない）:

1. 出力（`--console=plain`）の `> Task :mobile:compileGithubDebugKotlin` 行（テストを変えたなら
   `compileGithubDebugUnitTestKotlin` 行）に `UP-TO-DATE` が付いていないことを確かめる。付いていれば、その変更は
   前回の実行で既にコンパイル済みか、GitHub 版の対象外（Play 版専用の分岐など）なので、後者なら
   `./gradlew testPlayDebugUnitTest` を追加で実行する
2. `> Task :mobile:testGithubDebugUnitTest` 行に `UP-TO-DATE` が付いている場合、コンパイル結果のクラスが変わらなかった
   （コメントのみの変更など）ことを意味する。動作を変えたつもりなら `./gradlew testGithubDebugUnitTest --rerun` で再実行する

失う保証（承知のうえで採用する）:

- 途中のコミットはフル検証済みではない。途中のコミットへ戻ると、Android Lint の指摘や Play 版だけのビルド失敗が
  含まれている可能性がある
- 版の差（Play 版だけの失敗）、Android Lint の指摘、リソースとマニフェストの不整合は、ループの最後まで見つからない
- `commit --amend`・`rebase` は禁止のため、最後に見つかった失敗は修正コミットを積んで直す（原因のコミットは書き換えない）。
  最後のフル検証での失敗も品質ゲートの失敗で、同一原因で3回連続して解消できなければループを止める（guardrails 12.3）

### 配布物への固有の追記

`docs/guidelines/ADOPTION.md`「3. 適用方式」で取り込む配布物（配布スキルと `.claude/settings.json`）のうち、次の箇所は本リポジトリ固有の追記です。
配布元から取り込むときは配布元版で上書きせず、これらの追記を残すようにマージします。

| ファイル | 固有の追記 | 理由 |
| --- | --- | --- |
| `.claude/skills/autonomous-loop/SKILL.md` | イテレーション手順7の `scripts/scan-secrets.py` の実行（BL-064） | 秘密情報スキャンの手段がリポジトリ固有のため |
| `.claude/skills/autonomous-loop/SKILL.md` | イテレーション手順4・5と完了条件の「段階」の扱い（BL-079） | 品質ゲートの段階を CLAUDE.md で定めているため。定めの無い配布先では全ゲートになる書き方にしてある |
| `.claude/settings.json`（マージ対象） | 品質ゲートの `permissions.allow`、`permissions.deny`、`hooks` | ADOPTION.md は `permissions.allow` のマージだけを述べるため、deny と hooks も残すことを明記する（BL-077） |
| `.claude/skills/pr-create/SKILL.md` | 手順1・4の「本リポジトリ固有」の箇条（`gh pr create` 等はユーザーが実行する） | `.claude/settings.json` の deny と hooks で拒否しているため（BL-077） |
