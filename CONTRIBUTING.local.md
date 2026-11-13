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
| `.claude/agents/` | Claude Code のサブエージェント定義（コミットするのは backlog-implementer だけで、自律ループ実行モード中に限る） | `quality-gate-runner.md`（品質ゲートの実行と合否の要約。haiku）、`diff-reviewer.md`（本アプリ固有の観点での差分点検。sonnet）、`backlog-implementer.md`（BACKLOG のタスクの実装・記録・1タスク1コミット。inherit） |
| `.claude/hooks/` | `.claude/settings.json` から呼ぶ Claude Code の hooks（Python・標準ライブラリのみ） | `guard-commands.py`（禁止操作の拒否。テストは `scripts/test-guard-commands.py`）、`validate-records-after-edit.py`（記録ファイル編集後の検証）。説明は `CLAUDE.md`「記録ファイルの権限設定と禁止操作の強制」と本ファイル「禁止操作の hook」 |

## その他の開発プロセス

### CI を導入しない

品質ゲートを自動実行する CI（GitHub Actions など）は導入しません（BL-076 のユーザーの決定。GitHub Actions のコストを
かけず、ローカルでの品質ゲートの実行で対応する）。`.github/workflows/` は置きません。

- そのため、PR 作成前のフル検証（`CLAUDE.md` の品質ゲート表の段階「最後」の全行）が、main へ入る前の唯一の関門です。
  PR を作成・更新する前に必ず実行し、結果を PR 本文のチェックリストへ反映します（Skill `pr-create` 手順3）。
  自律ループ実行モードでは、ループの最後のコミットの状態で全ゲートを実行します（Skill `autonomous-loop` の完了条件）
- 人が手元でコミット・マージするときも、同じ段階「最後」のコマンドを実行します
- 失う保証: 手元の環境（JDK・Android SDK・Gradle のキャッシュ）に依存した成功を検出できない。実行を忘れた変更を
  機械的に止める仕組みは無い（PR のレビューで品質ゲートの結果を確かめる）
- 依存の脆弱性の確認（`CLAUDE.md` の脆弱性チェック行）は、リポジトリの公開後に GitHub の Dependabot を使う想定のままで、
  CI の導入とは別に判断する

### 禁止操作の hook

`.claude/hooks/guard-commands.py` は、エージェントの**誤操作を止める安全網**です（BL-077・BL-082）。意図的な回避まで防ぐ
サンドボックスではありません（`permissions.deny` も同じ）。普通に書かれ得る形は確実に判定し、判定しきれない形は次の
「限界」として扱います。拒否するものと判定する書き方の一覧は hook の冒頭コメントが正本です。

- 判定する書き方: git の長いオプションの短縮形（`--am`・`--no-ver` 等）、制御構文（`if`・`do`・`!` の後）、オプション付きの
  ラッパー（`env -i`・`sudo -u x`・`time -p`・`timeout` 等）、リダイレクト、`$(...)`・バッククォート（ダブルクォートと引用符なしの
  ヒアドキュメント本文の中を含む）、`bash -c`・`sh -c`・`eval`・`powershell -Command`・`Invoke-Expression`・`cmd /c` の引数と
  `bash <<EOF` の本文（同じ判定へ再帰的に通す）、`git -c core.hooksPath=…`・`git -c alias.*`（展開して判定）
- 限界（判定しない）: 変数・展開を経由する名前（`$GIT push`、`git $(echo push)`）、標準入力やファイルから読まれる引数と
  スクリプト（`echo push | xargs git`、`echo "git push" | bash`、`bash script.sh`）、gitconfig に定義済みのエイリアスと
  gh のエイリアス・拡張、`Start-Process`・`find -exec` など別プログラム経由の起動
- 通すもの: `gh pr create`（別名 `new`）と `gh pr edit`。PR の作成と本文の編集はエージェントへ許可している（ユーザーの決定）。
  自律ループ実行モード中は guardrails 12.2 で禁止のままで、hook はモードを判別できないため Skill `autonomous-loop`・
  `pr-create` の手順で守る。PR のマージ・クローズ・再開・コメント・レビュー・ready・update-branch と Issue の書き込みは拒否する
- 承知している誤止め: `gh api graphql` は読み取りでも POST で本文を送るため拒否する（読み取りは REST の GET で行う）
- 判定中に例外が起きた場合は、コマンドに禁止操作の語（push・amend・commit・gh など）が含まれるときだけ「hook の判定に失敗
  したので安全側で止めた」と返して拒否し、それ以外は通す（hook の不具合で全コマンドが止まらないため）

テストは、hook へ JSON を標準入力で渡して終了コードを確かめます。禁止コマンドそのものは実行しません（一時ディレクトリに
空のリポジトリを2つ作るだけ）。hook を変えたら実行し、見つかった書き方はケースとして足します。

```bash
python scripts/test-guard-commands.py
```

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

`docs/guidelines/ADOPTION.md`「3. 適用方式」で取り込む配布物（配布スキル・`.claude/settings.json`・`scripts/`）のうち、次の箇所は本リポジトリ固有の追記です。
配布元から取り込むときは配布元版で上書きせず、これらの追記を残すようにマージします。

| ファイル | 固有の追記 | 理由 |
| --- | --- | --- |
| `.claude/skills/autonomous-loop/SKILL.md` | イテレーション手順7の `scripts/scan-secrets.py` の実行（BL-064） | 秘密情報スキャンの手段がリポジトリ固有のため |
| `.claude/skills/autonomous-loop/SKILL.md` | イテレーション手順4・5と完了条件の「段階」の扱い（BL-079）、節「オーケストレータとサブエージェントの分担」（BL-068）、手順2の「同じコミットで削除するタスクは `進行中` への更新を省いてよい」（BL-083） | 品質ゲートの段階を CLAUDE.md で定めているため。定めの無い配布先では全ゲートになる書き方にしてある |
| `.claude/settings.json`（マージ対象） | 品質ゲートの `permissions.allow`、`permissions.deny`、`hooks` | ADOPTION.md は `permissions.allow` のマージだけを述べるため、deny と hooks も残すことを明記する（BL-077） |
| `.claude/skills/autonomous-loop/SKILL.md` | 終了時の報告の「ループ中は PR を作成・更新しない」段落 | `.claude/settings.json` と hook が `gh pr create` / `gh pr edit` を通しており、モードを判別できないため手順で守る |
| `.claude/skills/pr-create/SKILL.md` | 手順1の、settings.json と hook が `gh pr create` / `gh pr edit` を通しループ中は手順で止める旨の記述 | PR の作成・本文の編集をエージェントへ許可したため（それ以外の gh の書き込みは拒否のまま） |
| `.claude/skills/pr-create/SKILL.md` | 手順3の「本リポジトリ固有」の段落（PR の作成・更新の前に段階「最後」の全行を実行する） | CI を導入しないため、PR 作成前のフル検証が唯一の関門になる（BL-076） |
| `scripts/validate-records.py` | `区分` の許容値、BACKLOG の `id` の重複、EXECUTE.md の必須キー・`date` の形式・配列で書くキーの検査（BL-069） | FORMAT.md の定義のうち配布元版が検査していなかったものを補うため。配布元版が同じ検査を持つようになったら配布元版へ寄せる |
