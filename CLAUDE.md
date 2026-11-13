# CLAUDE.md

Claude Code 固有の差分と追加規約です。全エージェント共通規約の正本（`.github/copilot-instructions.md`）と
ガードレールは下記の `@import` で同時に読み込まれます。共通規約と矛盾する場合は本ファイルを優先します。
その他の参照先と読むタイミングは、共通規約「参照するドキュメント」を参照してください。

@rules/guardrails-unified.v1.md
@.github/copilot-instructions.md

## 共通規約に対する Claude Code 固有の差分（MUST）

| 項目 | Claude Code での差分 |
| --- | --- |
| git操作 | 自律ループ実行モード中の作業ブランチへの `git add` / `git commit` のみ実行してよい。`git push` はモードを問わず常にユーザーが実行する |
| PR説明文・コードレビュー・PR作成 | `.github/instructions/pr.instructions.md` を自動適用する機構が無いため、Skill `pr-create` の手順で同ファイルを**明示的に読んでから**従う |

## 本リポジトリの品質ゲート定義（MUST）

全エージェント共通で使用する正本です。`区分: 人手検証` のBACKLOGタスクは合否判定から除外します。

<!-- 導入時の記入方法（本ファイルは常時読み込まれるため、導入完了後はこのコメントごと削除してよい）:
- 本リポジトリの技術スタックへ合わせて下表を書き換える。表のプレースホルダを残さないこと、括弧内の例を削除する
  ことが導入の完了条件（`docs/guidelines/ADOPTION.md`「2. クイックスタート」）。
- 該当するゲートが無い場合も行は削除せず、コマンド欄へ `(対象外)`、合否基準欄へ理由を書く。
  「実行コードを持たないため」のように現状の事実として書き、未確認のまま空欄にしない。
- ローカル実行手順が未定義のゲートは「未定義（未確認）」と明記する。推測でコマンドを書かない。
-->

| ゲート | 段階 | コマンド | 合否基準 |
| --- | --- | --- | --- |
| コードスタイル（ktlint） | 最後 | `./gradlew ktlintCheck` | 終了コード0 |
| 静的解析（detekt） | 最後 | `./gradlew detekt` | 終了コード0（`config/detekt/detekt.yml` の `maxIssues: 0`） |
| 軽い検証（Gradle） | コード | `./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain` | 終了コード0、かつ変更したコードのコンパイルとテストのタスクが実行されている（確かめ方は下記の参照先） |
| Android Lint | 最後 | `./gradlew lintGithubDebug lintPlayDebug` | 終了コード0（GitHub 版・Play 版の両方） |
| 型検査・ビルド | 最後 | `./gradlew assembleDebug` | 終了コード0（全ての版の debug をビルドする） |
| 単体テスト | 最後 | `./gradlew testGithubDebugUnitTest testPlayDebugUnitTest` | 全件成功（両方の版） |
| 脆弱性チェック | － | （未導入。リポジトリの公開後に GitHub の Dependabot で依存の脆弱性を確認する想定） | 導入した場合は本行を実コマンドへ更新する |
| Markdown の静的解析 | 毎回 | `npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml` | `Summary: 0 issues`、かつ出力の `Linting: N files` が `git ls-files --cached --others --exclude-standard "*.md"` の件数と一致（対象漏れの検出） |
| 記録ファイルの検証 | 毎回 | `python scripts/validate-records.py`（PyYAMLが必要） | 終了コード0 |
| 秘密情報スキャン | 毎回 | `git add` の後に `python scripts/scan-secrets.py`（ステージ済みの差分を検査） | 終了コード0 |
| 秘密情報スキャン（ブランチ全体） | 最後 | `python scripts/scan-secrets.py --range main...HEAD`（`git add` しないフル検証でコミット済みの差分を検査） | 終了コード0 |

段階: `毎回` は全イテレーション、`コード` は Kotlin・Gradle（`mobile/`・`*.gradle.kts`・`gradle/`・`config/`）を
変えたイテレーションで `毎回` に追加、`最後` は自律ループの最後と PR 作成前で、軽い検証を除く全行を実行する
（対話実行での確認も全行が既定）。実測値・up-to-date の確かめ方・失う保証は `CONTRIBUTING.local.md`「品質ゲートの段階」。

## 記録ファイルの権限設定と禁止操作の強制（MUST）

`.claude/settings.json` の内容です（BL-077。変更は確認を挟む高リスク操作として扱う）。

- `permissions.allow`: `docs/records/managed/` 配下の3ファイルの編集と、上表の品質ゲートのコマンド（表の文字列と完全一致の
  ときだけ。オプションやパイプを足すと確認を挟む）。それ以外の編集・コマンドは確認を挟む
- `permissions.deny`: guardrails 12.2 の禁止操作（push・amend・rebase・reset --hard・ブランチとタグの削除・`--no-verify`・
  gh による PR と Issue の作成・更新）。Bash と PowerShell の両方。PR の作成・更新はユーザーが実行する（Skill `pr-create`）
- hooks: PreToolUse の `.claude/hooks/guard-commands.py` が deny の先頭一致で漏れる書き方（`git -C . push` 等）、
  規定ブランチ上でのコミット、gh の書き込み（`gh api` の POST 等）を語として判定して拒否する。誤操作の安全網であり
  サンドボックスではない（限界とテストは `CONTRIBUTING.local.md`「禁止操作の hook」）。
  PostToolUse の `.claude/hooks/validate-records-after-edit.py` が
  記録ファイルの編集後に `scripts/validate-records.py` を実行し、失敗を理由つきで返す

<!-- 配布バンドルの .claude/settings.json は記録ファイル関連の許可のみを含む。本リポジトリ固有の許可を追加する
場合は、確認を挟まずに編集させてよい範囲かを判断してから permissions.allow へ追記する。 -->

## 自律ループ実行モード（Loop Engineering）

人の応答を待たずに複数イテレーションを連続実行する、Claude Code 固有の運用モードです（Copilot は対象外）。

<!-- 本モードを採用しない場合は、本節と .claude/skills/autonomous-loop/ を削除する
（採否の判断は docs/guidelines/ADOPTION.md「1.2 自律ループ実行モードの採否」）。 -->

- **適用条件（MUST）**: ユーザーが開始を明示的に指示していること。指示されたスコープ内でのみ有効で、
  ループ終了と同時に通常の対話モードへ戻る。満たさない場合は `git add` / `git commit` を実行してはならない
- **起動方法（MUST）**: Skill `autonomous-loop` を起動し、その手順に従う。手順を記憶や推測で代用しない
- 統制要件（禁止操作・停止条件・秘密情報の取り扱い）は常時読み込みの `rules/guardrails-unified.v1.md`
  セクション12、ブランチ・コミット規約は `CONTRIBUTING.md`「自律ループ実行モードのブランチ・コミット規約」が正本

## 保守

共通規約の変更は `.github/copilot-instructions.md` へ、Claude Code 固有の変更は本ファイルへ反映します
（役割分担は `CONTRIBUTING.md`「エージェント指示ファイルの構成規約」）。

<!-- 本ファイルは配布元の templates/CLAUDE-template.md を基にしている。配布元の更新へ追従する場合は、
CHANGELOG.md で差分を確認し、本リポジトリ固有に書き換えた節（品質ゲート定義など）を上書きしないようマージする。 -->
