# 変更履歴

このリポジトリの主要な変更は本ファイルに記録します。

## Unreleased

- BL-096・BL-052 を完了として BACKLOG から削除した。ユーザーが BL-096 の残り（Private vulnerability reporting の
  報告画面の確認、BL-052 の判断）を確認済みと報告し、BL-052 も完了とした。BL-096 に依存していた BL-057・BL-031 の
  依存を解除した（Public 化が済んだため）。`docs/TESTING.md` U7 と Skill `mimiduku-device-verification` の
  BL-052 への言及も直した。アプリのコード・文字列・Play 申告は変えていない

- BL-096（確認の途中）: リポジトリの description と topics（6件）を `gh repo edit` で設定し、読み取りで反映を確認した。作業ブランチの PR（#14〜#19）は
  すべてマージ済み。BL-096 の残りは、Private vulnerability reporting の報告画面の確認と BL-052 の判断（人が行う）に絞った。
  アプリのコード・文字列・Play 申告は変えていない

- ユーザーが `.claude/settings.json` の `permissions.deny` から gh を外したのに合わせ、`.claude/hooks/guard-commands.py` の gh の判定
  （GH_WRITE・`gh api` の書き込みと BL-098 の例外）を削除し、gh を止めないようにした（今後も gh での GitHub 連携を行うため）。
  `settings.json` の `deny` 末尾に残っていた余分なカンマ（JSON として不正で、設定全体が読まれなくなる）も直した。
  テストの gh の拒否ケースを通過の期待へ変え（全249件成功）、`CLAUDE.md`・`CONTRIBUTING.local.md`・Skill `autonomous-loop`・`pr-create` の説明を直した。
  guardrails 12.2 は変えていない（自律ループ実行モード中の禁止は Skill の手順で守る）

- BL-096（確認の途中）: 公開後の確認を済ませた。GitHub が Apache-2.0 と認識すること、リポジトリが PUBLIC であること、Private vulnerability
  reporting・Dependabot alerts と security updates・Secret scanning・Push protection が有効であること（読み取りで確認）、README・SECURITY.md・
  docs/PRIVACY.md・アプリの SUPPORT_URL の各 URL が未認証で HTTP 200 になることを BL-096 の根拠へ記録した。残りは description・homepage・topics の設定、
  報告画面の確認、BL-052 の判断（人が行う）。アプリのコード・文字列・Play 申告は変えていない

- BL-096（確認の途中）: `LICENSE` の本文を公式の Apache-2.0 全文（LICENSE-2.0.txt）と照合し、11358 バイト・SHA-256 とも一致して差分が
  無いことを確かめた。BL-096 を「進行中」にして結果を根拠へ追記した（残りは GitHub のライセンス認識の確認と Public 切り替え以降で、人が行う）。
  アプリのコード・文字列・Play 申告は変えていない

- BL-098: Public 化後のリポジトリ設定をユーザーの指示でエージェントが行えるよう、`.claude/hooks/guard-commands.py` が通す `gh api` の書き込みを
  セキュリティ機能を有効にする4形（PUT の private-vulnerability-reporting・vulnerability-alerts・automated-security-fixes と、PATCH の
  `security_and_analysis[...][status]=enabled`）に限って足した。無効化・`--input`・他のフィールドやエンドポイントは拒否のまま。
  テスト13件を足し（全249件成功）、`CLAUDE.md` と `CONTRIBUTING.local.md` の説明を直した。guardrails 12.2 は変えていない
  （自律ループ実行モード中の禁止は Skill `autonomous-loop` の手順で守る）。`.claude/settings.json` の deny は変えていない
  （`gh api` はもともと deny に無く、hook だけが止めていた）

- Android Lint が追跡対象外の `local.properties`（端末ごとの SDK パスと release 署名の設定）まで検査し、`c\:/…` と書いても `PropertyEscape` の
  エラーで `lintGithubDebug`・`lintPlayDebug` が失敗する問題を直した。`mobile/lint.xml` を追加し、`local.properties` を `PropertyEscape` の
  検査から外した（品質ゲートが端末ごとの書き方に左右されないようにする）。アプリのコード・依存・CI/CD 定義は変えていない

- BL-097（確認の途中）: 「未確認」の34件のライセンスを、ローカルの Gradle キャッシュ（POM と metadata-2.107 の descriptors）で再度確かめたが、
  記載が無く確かめられなかった。ネットワーク取得と品質ゲート表に無い Gradle タスクは実行していないため、34件は「未確認」のままとし、
  BL-097 を「要確認」にした（人の許可か結果の連絡で再開できる）。`docs/THIRD_PARTY_LICENSES.md` に確認した範囲を追記した

- BL-093: 同梱する OSS のライセンス一覧 `docs/THIRD_PARTY_LICENSES.md` を追加し、README のライセンス節からリンクした。release ビルドの
  依存（88件。GitHub 版・Play 版で同一）を AGP の依存レポート（`dependencies.pb`）から取り、ローカルの Gradle キャッシュの POM で確かめた。
  確かめられたものはすべて Apache-2.0、NOTICE ファイルを持つ依存は無く（`core`・`core-ktx` が LICENSE.txt を含むのみ）、追加で載せる
  NOTICE は無い。統括モジュール34件（直接の依存の material3 を含む）は POM がキャッシュに無く「未確認」とし、確かめる作業を BL-097 に起票した
  （依存の解決の Gradle タスクは品質ゲートの表に無いため実行していない）。アプリ内の表示は出さない（BL-094 の A）。依存の追加・変更は無い

- BL-092: README を外部の読者向けに直した。前提条件の「Google Play では配布しません」を「現在は GitHub からの配布が前提で、Google Play 版は
  申請を予定している（公開はまだ）」に改め、「不具合・要望・脆弱性」の節（Issues と SECURITY.md への案内）を足し、「開発の進め方」に
  `CONTRIBUTING.md`・`rules/`・`.claude/`・`docs/records/` は開発に使う AI エージェント向けの規約と記録であることの説明を足した（内部文書は
  公開範囲に残す。BL-095 のユーザーの決定）。README の相対リンクはすべて実在するファイルを指すことを確かめた。`CONTRIBUTING.md`・
  guardrails・`CLAUDE.md`・`.claude/` は変更していない

- ユーザーの決定を BACKLOG へ反映した。BL-094（アプリ内のライセンス表示）は A（出さない。リポジトリの LICENSE と docs の一覧だけ）、
  BL-095（公開範囲）は内部文書を残し README で説明する方向（A）に決まったため、2件を削除した。依存の NOTICE の扱いの確認は BL-093 の
  完了条件へ引き継いだ。GitHub のメール非公開設定の確認は不要とされたため BL-096 の確認項目から外した（他の項目は変えていない）。
  アプリのコード・文字列・Play 申告は変えていない

- BL-091: `SECURITY.md`（対象の版、脆弱性は公開の Issue に書かず GitHub の Private vulnerability reporting で知らせること、応答の期限を
  約束しないこと）と、`.github/ISSUE_TEMPLATE/` の不具合報告・要望（いずれも YAML のフォーム。不具合報告は端末名・Android の版・アプリの版と
  配布の版・再現手順と、個人情報を含めない注意書き）・`config.yml`（空の Issue を無効にし、脆弱性は SECURITY.md へ案内）を追加した。
  窓口は GitHub の機能だけで、個人のメールアドレスは載せていない。Private vulnerability reporting の有効化は GitHub の設定で
  人が行う（BL-096）。既存の `.github/` のファイルと CI/CD 定義は変えていない

- BL-090: `docs/PRIVACY.md` の制定日（2026-11-22）と 8章「問い合わせ先」（GitHub の Issues。公開されるため個人情報を書かないよう添えた。
  個人のメールアドレスは載せない）を記入し、`docs/PLAY_LISTING.md` のポリシー URL を
  `https://github.com/filderschoice/mimiduku/blob/main/docs/PRIVACY.md` にした。読み取る情報・使い道・保存・送信の記述、開示文
  （`disclosure_body`）、`ConsentStore.CURRENT_VERSION` は変えていない。この URL はリポジトリの Public 化（BL-096）まで開けない。
  Play のポリシー URL として受け付けられるかは未確認で、BL-031 の申請時に確かめる

- BL-089: リポジトリのルートに Apache-2.0 の `LICENSE` を追加し、README にライセンスの節（Apache License 2.0・Copyright 2026
  filderschoice）を設けた（ユーザーの決定による。著作権者は公開済みの GitHub アカウント名で、実名・メールは載せない）。`LICENSE` の本文は
  記憶から書かず、ローカルの Gradle キャッシュにある AGP 8.13.0 の aapt2 同梱の `NOTICE`（`transforms/.../aapt2-8.13.0-13719691-windows/NOTICE`）の
  7〜208行目（Apache License 2.0 の本文と末尾の APPENDIX）を改変せずに写した。条項の本文は、同キャッシュの AndroidX の `LICENSE.txt` と
  Python の pip 同梱の `LICENSE.APACHE`（どちらも APPENDIX なし）の本文と差分なしで一致することも確かめた。apache.org の原文との照合は
  BL-096 で人が行う。本アプリ自身の NOTICE ファイルは作らない

- リポジトリの Public 化と Play 公開に向けた対応を BACKLOG へ起票した（自律ループの第1イテレーション）。自動で進めるものは
  BL-089（Apache-2.0 の LICENSE と README のライセンス節）、BL-090（docs/PRIVACY.md の制定日・問い合わせ先と docs/PLAY_LISTING.md の
  ポリシー URL）、BL-091（SECURITY.md と Issue テンプレート）、BL-092（README の Play 配布の記述と外部向けの「開発の進め方」）、
  BL-093（同梱する OSS のライセンス一覧）。要確認は BL-094（アプリ内のライセンス表示）と BL-095（内部文書の公開範囲）、人手検証は
  BL-096（push・メール非公開設定・Public 化・GitHub のセキュリティ機能の有効化・公開後のリンク確認）。BL-031 と BL-057 は公開した
  ポリシーの URL が前提のため、依存に BL-096 を足した。`.claude/hooks/__pycache__/` は既に `.gitignore` の対象で追跡されていないため
  起票しなかった

- BL-085（release 署名と版管理の実鍵での確認）が完了した。ユーザーが作った鍵（PKCS12・別名 `mimiduku-release`・リポジトリの外に保管）で
  `scripts/release-build.bat` を実行し、GitHub 版の APK と Play 版の AAB が成功した。両方の証明書 SHA-256 は一致し、ユーザーが鍵ストアの
  指紋と照合して一致を確かめた。GitHub 版の APK を Pixel 8 Pro へ入れて起動を確認した（クラッシュなし）。`scripts/version.properties` の
  `VERSION_CODE` を 1 から 2 へ上げた（記録は成功したビルドだけが更新する）。鍵と `local.properties` は追跡されていない。BL-031 の
  依存が外れた。初回のビルドは、`local.properties` の別名と鍵ストアの別名の食い違いで署名に失敗したが、鍵を作り直して解消した
  （原因はユーザー側の設定で、スクリプトの不具合ではない）
- README「リリースビルド」の署名確認の手順を直した。`keytool -printcert -jarfile` は APK では何も出力しない（minSdk 29 のため v2
  署名のみで、v1 署名が無い）ため、APK は `apksigner verify --print-certs -v` で確かめる手順を足した
- ガードレール配布元 copilot-rules を 0.30.0 へ追従した。配布元版で置き換えたのは `rules/guardrails-unified.v1.md`
  （セクション12.7「サブエージェントへの委任」の追加。規範本文は配布元の正本で、本リポジトリでの書き換えは無い）、`CONTRIBUTING.md`、
  `docs/guidelines/ADOPTION.md`、`scripts/scan-secrets.py`（検出規則の拡充・`--require-changes`・UTF-8 出力。本リポジトリ由来の
  規則はすべて配布元版に含まれる）、`scripts/validate-records.py`（本リポジトリで足していた検査が配布元版へ入ったため固有の追記を
  解消。`区分` の許容値は FORMAT.md の表から読む）、`.claude/hooks/validate-records-after-edit.py`、Skill `docs-consistency-review`・
  `loop-orchestration`。サブエージェント定義 `.claude/agents/backlog-planner.md`（計画担当、opus）を新たに配置した
- 固有改変をマージしたもの: Skill `autonomous-loop`・`pr-create` は配布元版を土台に、本リポジトリ固有の段落（ループ中は PR の
  作成・ブランチの削除をしない、CI を導入しないため PR 作成前の全ゲートが唯一の関門）を戻した。サブエージェント定義3件は、
  本アプリ固有の記述（Skill `mimiduku-change` の参照・点検観点・段階と UP-TO-DATE・テスト件数の確かめ方）を残し、12.7 の参照、
  開始指示の原文が無ければコミットしない、`--require-changes`、統制の緩和・守る側の変更・文書の整合の観点を取り込んだ
  （ユーザーの決定「固有の記述を残してマージ」）。`CLAUDE.md` は品質ゲート表の秘密情報スキャンの行を `--require-changes` 付きへ
  改め、ステージしない場面の `--include-unstaged` を足し、自律ループ実行モードの統制要件の行に 12.7 を書き足した
- `.claude/settings.json` を変更した（ユーザーの決定「両方反映」）。`permissions.allow` の秘密情報スキャンを品質ゲート表の新しい
  文字列（`--require-changes`・`--include-unstaged`・`--range main...HEAD --require-changes`。Bash・PowerShell）へ置き換え、
  `hooks.PostToolUse` の記録ファイル検証フックの要素を配布元版（MultiEdit も対象、スクリプトが無いときにエラーを出す起動
  コマンド、timeout 90）にした。`permissions.deny` と PreToolUse の `guard-commands.py` は変えていない
- `CONTRIBUTING.local.md` の構成表と「配布物への固有の追記」を実態へ合わせ、Skill `mimiduku-change` のスキャンの実行例を
  `--require-changes` 付きにした。プロファイルスキルの雛形 `templates/project-profile-SKILL-template.md` は、Skill
  `mimiduku-change`・`mimiduku-device-verification` が同じ役割を担っているため取り込まない。`.github/CODEOWNERS` は配布元の
  プレースホルダのため取り込まない。常時読み込みは `CLAUDE.md` 8,340→8,977 バイト、`rules/guardrails-unified.v1.md`
  7,702→9,162 バイト（12.7 の追加で増加）、`.github/copilot-instructions.md` は変更なし（11,909 バイト）
- 取り込み時の検証（Python の構文チェック）で生成された `__pycache__/` の `.pyc` 3件が誤ってコミットされたため、
  Git の管理から外し、配布元の案内（0.30.0 配布先での対応の2）どおり `.gitignore` へ `__pycache__/` と `*.pyc` を足した

- 規定ブランチ上の `git merge --ff-only` を hook が誤って止める不具合を直した（BL-088。PR #10 のマージ後の整理で発生し、
  起票して同じループで完了）。`.claude/hooks/guard-commands.py` は merge の ff 系オプション（`--ff`・`--no-ff`・`--ff-only`）の
  最後の指定が `--ff-only` のときだけ規定ブランチ上でも通す。オプション無しの merge と、`--ff-only` を後の指定で打ち消す形は
  拒否のまま。hook の冒頭コメントと CONTRIBUTING.local.md「禁止操作の hook」を合わせて直し、hook のテストは CASES_MAIN へ
  拒否 5件・通過 5件を足して 236件（拒否 143・通過 93）がすべて成功

- ローカルブランチの削除をエージェントへ許可した（BL-087。対話実行に限る）。`.claude/settings.json` の `permissions.deny` から
  `git branch -D` / `-d` / `--delete`（Bash・PowerShell）を外し、`.claude/hooks/guard-commands.py` は規定ブランチ（main / master）を
  対象にした削除だけを拒否するようにした（上書きの `-f`・`-M`・`-C`、`--force` 付きの削除、タグの削除は拒否のまま）。自律ループ実行
  モード中は guardrails 12.2 で禁止のままで、hook はモードを判別できないため、Skill `autonomous-loop` の終了時の報告の節に「ループ中は
  ブランチを削除しない」を足した（`rules/` は変えていない）。CLAUDE.md と CONTRIBUTING.local.md（禁止操作の hook・配布物への固有の
  追記）を実態に合わせた。hook のテストは削除を通過側へ移し、規定ブランチの削除と強制削除を拒否側へ足して 226件（拒否 138・
  通過 88）がすべて成功。対象だった2ブランチ（docs/bl-024-play-submit-decision と feature/loop-20261108-bl024-play-decision）は
  ループの終了後に対話実行で、包含を確かめてからエージェントが削除し、BL-087 を閉じた

- ローカルブランチの削除をエージェントへ許可するタスクを起票した（BL-087。ユーザーの決定「ブランチの整理はエージェントで
  実施していい」）。squash マージ済み・内容包含済みと確かめた2ブランチの削除が deny と hook で止まったため。許可は対話実行に限り、
  自律ループ実行モード中は guardrails 12.2 のとおり削除しない既定値とした

- Skill `loop-orchestration` を足した（オーケストレータが作業管理だけを行い、実装・検証・レビューをサブエージェントへ委ねる
  自律ループの進め方。2026-11-13〜14 のループの実績から、委任文の書き方、担当を順番に動かす進め方、ユーザーの判断の組み込み、
  レビューから修正への流れ、利用上限で中断した担当の再開をまとめた）。あわせてモデルの割り当てを「オーケストレータは Opus、
  実装とレビューのサブエージェントは Sonnet、定型の品質ゲート実行は Haiku」に整理し（ユーザーの決定「オーケストレーションは
  Opus、サブエージェントは Sonnet」「コスト面も考慮して最適化してよい」）、`.claude/agents/` の `backlog-implementer` を
  inherit から `model: sonnet` へ変えた。`diff-reviewer` は sonnet、`quality-gate-runner` は haiku のまま。Skill `autonomous-loop` の
  分担の節と CONTRIBUTING.local.md の `.claude/agents/` の行に参照を足した。品質ゲートは Markdown の静的解析のみ実行

- release 版の署名と版の管理を sesami-wear と同じ方式で入れた（BL-075。実施記録は EXECUTE.md）。鍵の作成・保管と release
  ビルドはユーザーが行い、手順は README.md「リリースビルド」、利用者向けの変更点は新しい docs/RELEASE_NOTES.md に記録する。
  鍵ストアの拡張子（.p12・.pfx・.bks）を .gitignore と scripts/scan-secrets.py に足した。実鍵での確認は BL-085（人手検証）
- CI を導入しないと決めた（BL-076 を閉じた。ユーザーの決定「CI を導入しない、GitHub Actionのコストではなく、ローカルでの
  品質ゲートチェックで対応する」）。`.github/workflows/` は作らない。決定と、CI が無い前提で失う保証（手元の環境に依存した成功を
  検出できない、実行忘れを機械的に止められない）を CONTRIBUTING.local.md「CI を導入しない」へ書いた。PR 作成前のフル検証が
  main へ入る前の唯一の関門になるため、Skill `pr-create` 手順3に「PR の作成・更新の前に、PR に含める最後のコミットの状態で
  段階『最後』の全行を実行する」を足した（Skill `autonomous-loop` の完了条件は既に最後のコミットでの全ゲートを定めており変更なし）。
  脆弱性チェック（Dependabot）は公開後の想定のままで、CI とは別に判断する
- PR の作成と本文の編集をエージェントへ許可した（BL-084。ユーザーの決定「PRの作成はエージェントに実施させる」「gh pr edit も
  許可でOK」を起票し、同じイテレーションで完了）。`.claude/settings.json` の `permissions.deny` から `gh pr create *` と
  `gh pr edit *`（Bash・PowerShell）を外し、`.claude/hooks/guard-commands.py` の GH_WRITE の pr から `create`・`new`・`edit` を外した。
  PR のマージ・クローズ・再開・コメント・レビュー・ready・update-branch・lock・revert と、Issue・release・workflow・repo などの書き込み、
  `gh api` の書き込みは拒否のまま。自律ループ実行モード中は guardrails 12.2 で PR の作成・更新が禁止のままで、deny と hook は
  モードを判別できないため、Skill `autonomous-loop` の終了時の報告の節に「ループ中は PR を作成・更新しない」を足して手順で守る
  （`rules/` は変えていない）。Skill `pr-create` は BL-077 で足した「ユーザーが実行する」の記述を外し、エージェントが
  `gh pr create` / `gh pr edit` を実行する形へ戻した（push は引き続きユーザー）。あわせて品質ゲート表の
  `python scripts/scan-secrets.py --range main...HEAD` を `permissions.allow`（Bash・PowerShell、表の文字列と完全一致）へ足した。
  CLAUDE.md と CONTRIBUTING.local.md（禁止操作の hook・配布物への固有の追記）を実態に合わせた。hook のテストは PR の作成・編集を
  通過側へ移し、PR のクローズ・コメント・レビュー・ready と Issue の作成・編集を拒否側へ足して 216件（拒否 134・通過 82）がすべて成功
- サブエージェントの起動確認を終えた（BL-078）。2026-11-13 のセッション（feature/loop-20261113-brushup）で、diff-reviewer・
  quality-gate-runner・backlog-implementer の3つとも Agent ツールから定義名で起動し、定義どおりの報告（重大度つきの指摘、
  ゲートごとの成否表、30行程度の実装報告）を返した（オーケストレータの確認）。frontmatter が効いていることは backlog-implementer
  自身について確かめた。使えるツールは Read・Edit・Write・Bash・Grep・Glob・Skill（と報告の受け渡し）で、Agent ツールは無く
  再委譲できない。model は inherit で、Opus で動いていた（軽量モデルへ固定されていない。呼び出し元のモデルとの一致は内側からは未確認）。quality-gate-runner（haiku）と diff-reviewer（sonnet）の
  実行モデルと、両者で Edit・Write が使えないことは、担当の内側から確かめられないため未確認のまま残した（定義の不具合は見つからず、
  定義は変えていない）。あわせて差分レビューの小さな指摘を直した（BL-083。品質ゲートの段階と mimiduku-change の食い違い、
  backlog-implementer の進行中の省略とテスト件数の集計、ConsentStoreTest の版数の固定、`scripts/scan-secrets.py --range` による
  フル検証でのブランチ全体の検査。実施記録は EXECUTE.md）
- `.claude/hooks/guard-commands.py` のすり抜けと誤止めを直した（BL-082。ループの最後の差分レビューの指摘を起票し、同じ
  イテレーションで完了）。位置づけは誤操作を止める安全網で、サンドボックスではないことを hook の冒頭コメントと
  CONTRIBUTING.local.md「禁止操作の hook」に明記し、判定しきれない形（変数経由、`xargs` へ標準入力で渡す引数、`bash script.sh`、
  gitconfig に定義済みのエイリアス、`Start-Process` など）を限界として書いた。直したもの: (1) ヒアドキュメントを行単位で
  読み捨てていたため、開始行の `<<EOF` より後ろ（`cat <<EOF && git push`）が判定から消え、引用符の中の `<<`・here-string の
  `<<<`・算術の `1<<n` を開始と誤認して以降の全行を読み捨てていた。字句解析の中でヒアドキュメントを扱う方式に改め、本文は
  開始行の改行の後で読む。(2) 先頭語が git でないと判定されなかった。制御構文（`if`・`then`・`do`・`!` 等）、オプション付きの
  ラッパー（`env -i`・`env -u X`・`env -S`・`command -p`・`sudo -u`・`time -p`・`timeout`・`nice`・`xargs` の引数に書いた
  コマンド）、リダイレクト、ダブルクォートの中と引用符なしのヒアドキュメント本文の中のコマンド置換、`bash -c`・`sh -c`・`eval`・
  `powershell -Command`（`-EncodedCommand` を含む）・`Invoke-Expression`・`cmd /c` の引数と `bash <<EOF` の本文を同じ判定へ
  再帰的に通す。(3) git の長いオプションの短縮形（`--am`・`--no-ver`・`--har`・`--del`・`--forc`）を拒否する。(4) `--gpg-sign` が
  次の語を値として飲み込んでいた（`git commit --gpg-sign --amend` が通った）。(5) `git -c core.hooksPath=…` を拒否し、`git -c alias.*`
  は展開して判定する。`git config` での core.hooksPath・alias.* の設定も拒否に足した（レビューの指摘外。定義した後のエイリアスは
  判定できないため）。(6) `git checkout main -- <path>` をブランチの切り替えと誤認し、その後のコミットを拒否していた。(7) gh は
  `gh api` の書き込み（`-X`/`--method` が GET 以外、または `-f`・`-F`・`--input` で本文を送る形）、release の作成・編集・削除、
  `workflow run`、`pr update-branch` を拒否に足した。同じ趣旨で run の rerun・cancel、repo・secret・variable・label・gist の
  作成・変更・削除、`gh alias set`、作成の別名 `new` も足した。`gh api graphql` は読み取りでも POST で本文を送るため止まる（承知の
  誤止めとして記載）。(8) 判定中の例外では、コマンドに禁止操作の語（push・amend・commit・gh など）が含まれるときだけ「hook の判定に
  失敗したので安全側で止めた」と返して拒否し、それ以外は通す（標準入力の JSON が読めない場合は従来どおり通す）。(9) `--force` は
  全サブコマンドで拒否のまま、`git branch -f`（と同じ上書きの `-M`・`-C`、`git tag -f`）を拒否に足した。テストを
  `scripts/test-guard-commands.py` として残し（JSON を標準入力で渡すだけで、禁止コマンドは実行しない）、実行方法を
  CONTRIBUTING.local.md に書いた。205件（拒否を期待128件・通過を期待77件。通過側に Skill dependabot-alert-triage・pr-create が使う
  `gh api` の GET・`gh pr view`・`gh --version` と、本文に push・rebase・`--amend`・`--no-verify` を含むコミットメッセージと
  ヒアドキュメントを含む）がすべて期待どおり。同じテストを修正前の hook に通すと79件が失敗する。`.claude/settings.json` は
  変えていない（gh の書き込みは hook で止まるため deny へ足す必要が無い）。あわせて BL-083（文書の食い違いと小さな指摘）を起票した
- `scripts/validate-records.py` の検査を FORMAT.md の定義へ近づけた（BL-069）。追加した検査は、(1) BACKLOG の `区分` が FORMAT.md の
  6値（不具合・UX改善・機能追加・性能・品質ゲート・人手検証）のいずれか、(2) BACKLOG の `id` の重複、(3) EXECUTE.md の必須キー
  （date・summary・details の変更内容・変更ファイル・検証コマンド・検証結果）と date の `YYYY-MM-DD HH:mm` 形式（秒つき・日付だけも
  不合格）、(4) EXECUTE の `変更ファイル` と（ある場合の）`関連ID` が配列であること。既存の記録はすべて合格し、記録も検査も緩めずに
  済んだ。記録ファイルの一時コピーへ違反を1つずつ入れて（区分・id 重複・date 欠落・date の形式3種・summary 欠落・検証コマンド欠落・
  変更ファイルと関連ID が配列でない）終了コード1、元のコピーで0を確かめた。実行時間は約0.3秒で、記録ファイル編集後の hook への影響は
  無視できる。本スクリプトは配布物（docs/guidelines/ADOPTION.md の表）のため、取り込みで上書きしないよう CONTRIBUTING.local.md
  「配布物への固有の追記」へ行を足した
- Skill `autonomous-loop` に節「オーケストレータとサブエージェントの分担」を追加した（BL-068）。役割の表（オーケストレータ・
  backlog-implementer・diff-reviewer・quality-gate-runner。名前は `.claude/agents/` の定義と一致）、渡し方（BACKLOG の id・ブランチ・
  ゲートの段階・報告の行数）、コミットは実装の担当が行うこと、サブエージェントは再委譲できないこと、管理できないバックグラウンド実行を
  使わないこと（managed-agent-execution）を書いた。BACKLOG の案に加え、このセッションで分かった運用を反映した。実装の担当は同じ
  作業ツリーを使うので1体ずつ順番に動かし、依存の無い2〜3件を1体へまとめて渡すと往復が減る。読み取り専用の diff-reviewer は
  コミット範囲を指定すれば次の実装と並行できる。オーケストレータは担当の全文ログを読まず報告の行数を指定する。ユーザーの判断は
  実行中の担当が終わってから次の委任に組み込む。フル検証は quality-gate-runner にループの最後に1回させる（BL-079 の段階）。
  定義はセッション開始時に読み込まれるため追加直後のセッションでは使えないことがある。案の「1タスク1サブエージェント」は、
  2〜3件をまとめる運用に合わせて「まとめるのはこの程度」とした。guardrails セクション12は複製せず参照した。配布スキルへの固有の
  追記として CONTRIBUTING.local.md の表へ記載した。あわせて BL-078 を更新した（diff-reviewer は起動を確認済み、
  quality-gate-runner と backlog-implementer は未確認のため項目は残す）
- サブエージェント `.claude/agents/backlog-implementer.md` を追加した（BL-067）。BACKLOG のタスクを受け取り、Skill `mimiduku-change`
  に従って実装・テスト・指定段階の品質ゲート・記録ファイルの更新を行い、自律ループ実行モード中に限り作業ブランチへ1タスク1コミットする
  担当。tools は Read・Edit・Write・Bash・Grep・Glob・Skill（Agent を与えず再委譲させない）、model は inherit（実装の判断を伴うため）。
  BACKLOG の案に加えて、このセッションで実際に使った委任の形を反映した。最初に読むもの（Skill autonomous-loop・mimiduku-change、
  FORMAT.md、ユーザー共通スキル guardrail-records-update）、依存の無い2〜3件をまとめて受けても1件ずつコミットすること、品質ゲートは
  段階に従いフル検証はオーケストレータが最後に行うこと、hook や権限に拒否されたら回避せず報告すること、報告は30行程度で「完了か・
  コミット・変更ファイル・検証の成否・既定値で決めたこと・未確認のこと」。BACKLOG の案の「40行以内」は運用に合わせて30行程度にした。
  禁止操作と停止条件は guardrails 12.2・12.3 を正本として参照し、列挙は最小限にした。CONTRIBUTING.local.md「ドキュメント構成（追加分）」の
  `.claude/agents/` の行を更新した（コミットするのは本担当だけ）。定義はセッション開始時に読み込まれるため、起動の確認は次のセッションで行う
- BL-070・BL-071 の差分レビューの軽微な指摘3件を BL-080 として起票し、同じイテレーションで対応した。(1) docs/TESTING.md 2.9 の X14 に
  「B の遷移先のサイトも未登録」の前提を明記した（登録済みなら置き換わるバナーは無く A のバナーが残る）。(2) EXECUTE.md の BL-070 の
  記録「ドメイン待ちの予約は介入を行う関数の分岐の中に置いた」は BL-071 で DomainWait へ移ったため、既存ログは変えず BL-071 の記録の
  変更内容へ補足を足した。(3) MimidukuAccessibilityService.onDestroy は domainCheck.cancel() だけで、DomainWait の待ちを確定させないため、
  サービス終了の1秒以内に判定待ちだった検知はログにもバナーにも残らないことをコードで確かめた（AwaitDomain の時点では記録しない）。
  直すかは記録の範囲に関わる仕様の判断のため、実装せず BL-081（要確認）で起票した。コードは変えていない
- **利用者への告知（リリースノート）**: アクセシビリティ API の開示文の版数（`ConsentStore.CURRENT_VERSION`）を 1 から 2 へ
  上げた（BL-074）。BL-055 で開示文へ足したサイト名のハッシュ・PIN で暗号化した控えの保存とアドレスバーの読み取りについて、
  同意を取り直すため。この版へ更新すると、以前の版で同意していた利用者にも「はじめに：画面の読み取りについて」が再び出て、
  同意し直すまで検知・介入・バッジが止まる。配布時のリリースノートには「更新後にアプリを開いて、画面の読み取りについての説明に
  もう一度同意してください。同意するまで保護は動きません」と書く。開示文の文面と版数以外の挙動は変えていない。リリースノートの
  原本の置き場所が README・CONTRIBUTING・既存スキルに無いため、本ファイルを原本とした。実施記録は EXECUTE.md
- 品質ゲートを段階に分けた（BL-079。ユーザーの提案「フル検証はループの最後に1回でよい」を受けて起票し、同じイテレーションで
  完了）。CLAUDE.md の品質ゲート表へ「段階」列と軽い検証の行（`./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest
  --console=plain`）を加え、`毎回`（Markdown の静的解析・記録ファイルの検証・秘密情報スキャン）、`コード`（Kotlin・Gradle を変えた
  イテレーションで軽い検証を追加）、`最後`（ループの最後と PR 作成前に全ゲート）とした。振り分けは実測で決めた。コード変更直後の
  単独実行で ktlint 11.3秒・detekt 4.4秒・Android Lint 33.3秒・assembleDebug 12.9秒・両方の版の単体テスト 21.3秒・GitHub 版のみ
  5.8秒、軽い検証の1回起動で 12.2〜20.6秒（フル検証は約83秒で Android Lint が支配的）。単体テストを GitHub 版にしたのは
  `src/github` も含めてコンパイル対象が最も広いため。実測値・up-to-date で素通りしていないことの確かめ方・失う保証（途中の
  コミットはフル検証済みでない、版の差と Android Lint の指摘が最後まで見つからない、amend・rebase 禁止のため修正コミットを積む）は
  CONTRIBUTING.local.md「品質ゲートの段階」に置き、常時読み込みの CLAUDE.md は表の列と3行の説明だけ増やした。Skill
  `autonomous-loop` の手順4・5と完了条件（段階の定めがあれば該当段階だけ、最後に全ゲート。定めが無ければ全ゲート）、
  サブエージェント `quality-gate-runner`（段階の指定と、コンパイル・テストのタスクの UP-TO-DATE の報告）、`.claude/settings.json`
  の allow（軽い検証のコマンド）を更新した。配布スキルへの固有の追記として CONTRIBUTING.local.md の表へ記載した。
  guardrails 12.3 の停止条件は変えていない（最後の実行での失敗も同じ数え方）
- `.claude/settings.json` の権限と hooks を見直した（BL-077）。ユーザーの決定は「deny と hooks を採用、allow は品質ゲートのみ」で、
  BL-077 の3提案はすべて採用した（不採用なし）。(1) `permissions.deny` に push・`commit --amend`・`commit --no-verify`・rebase・
  `reset --hard`・ブランチとタグの削除・gh の PR と Issue の作成と更新を Bash と PowerShell の両方で加えた。(2) PreToolUse の
  `.claude/hooks/guard-commands.py` がコマンドをシェルの語に分解して判定し、deny の先頭一致で漏れる書き方（`git -C . push`、
  `git commit -m x --amend`、`-nm` の束、`$(...)` の中、PowerShell の `& git.exe push`）、`--force` 系のオプション、規定ブランチ
  （main・master）上での commit・merge・cherry-pick・revert・am を拒否する。コミットメッセージ・ヒアドキュメント・PowerShell の
  ヒア文字列の本文は判定しない。PostToolUse の `.claude/hooks/validate-records-after-edit.py` は記録ファイル3つの Edit・Write の後だけ
  `scripts/validate-records.py` を実行し、失敗を終了コード2と標準エラーで Claude へ返す。(3) `permissions.allow` に品質ゲート表の
  コマンドを完全一致で加えた（秘密情報スキャンを含む）。gh の PR 作成・更新を拒否したため Skill `pr-create` を「本文の用意まで
  エージェント、コマンドはユーザーが実行」へ改め、CLAUDE.md の節「記録ファイルの権限設定と禁止操作の強制」と
  CONTRIBUTING.local.md（`.claude/hooks/` の行、配布物への固有の追記の表）を実態に合わせた。検証は、フックへ標準入力で JSON を渡す
  55件（拒否28・通過27。通過側に「push」「rebase」「--no-verify」を本文に含むコミットメッセージとヒアドキュメントを含む）がすべて
  期待どおりで、使い捨てリポジトリの main 上の commit・merge・`-C` 指定が拒否され、壊した記録ファイルの複製で検証フックが
  終了コード2と理由を返すことを確かめた。実際の push 等は実行していない。`claude doctor` で問題の報告は無かった。対象外としたもの:
  `gh api` による書き込みと `gh release`（BL-077 の範囲外）、`sh -c "git push"` のような別プロセス経由の実行（フックの判定外。
  deny も同様で、公式ドキュメントのとおりセキュリティ境界ではない）
- プロジェクト固有スキル `.claude/skills/mimiduku-change/SKILL.md` を追加した（BL-066）。本アプリへ機能追加・不具合修正・検知ルールや
  文言の変更を入れるときに、変更の種類ごと（判定・版の差・検知ルール・スケアウェア文言・アドレスバー・文言・開示文・保存する情報・UI）に
  守ることと正本（DESIGN.md の節と FR、README.md、strings.xml のコメント、テストの KDoc、docs/TESTING.md・PRIVACY.md・PLAY_LISTING.md）を
  表にした索引で、規範の本文は複製していない。DESIGN.md「エージェント実装指示」から本スキルを参照した（記録ファイルの更新のみで
  コード修正ではないため EXECUTE.md は更新していない）。description は常時読み込みのため256字に収めた。Gradle 系の品質ゲートは省略した
- サブエージェント `.claude/agents/quality-gate-runner.md`（品質ゲートの実行と30行以内の合否表。haiku、tools は Bash・Read・
  Grep・Glob）と `diff-reviewer.md`（本アプリ固有の観点での差分点検と重大度つきの指摘。sonnet、Bash は git の読み取りに限定）を
  追加し、CONTRIBUTING.local.md「ドキュメント構成（追加分）」に置き場所と役割を書いた（BL-065）。どちらもコマンドや規範を複製せず
  CLAUDE.md・DESIGN.md などの正本を参照する。定義はセッション開始時に読み込まれるため、追加した会話では起動できなかった
  （Agent ツールに現れない）。代わりに haiku の汎用サブエージェントへ quality-gate-runner の定義を読ませて全ゲートを実行させ、
  成否表（Gradle 系5件は成功（up-to-date）、脆弱性チェックは対象外、記録ファイル・秘密情報スキャンは成功）が返ることと、定義自身の
  Markdown lint 違反（MD038、コード範囲の末尾空白）を検出できることを確かめ、その違反を直した。実際の定義での起動確認は BL-078 として
  起票した。Kotlin・Gradle に触れないため、本イテレーションでの Gradle 系の再実行は上記以外に行っていない
- コミット前の秘密情報スキャン `scripts/scan-secrets.py` を追加した（BL-064）。ステージ済みの差分（`--include-unstaged` で
  未ステージと未追跡も）の追加行から、秘密鍵の PEM ヘッダ・AWS／GitHub／Google のキーの形式・パスワードへのリテラル代入を、
  パスから `.jks`・`.keystore`・`local.properties`・`.env` を検出し、値を伏せてファイル名と行番号を出す（検出ありで終了コード1）。
  標準ライブラリのみ。CLAUDE.md の品質ゲート表と autonomous-loop の手順7から参照した。使い捨てリポジトリのダミー値（明らかな
  ダミー文字列）で12件の検出と終了コード1、検出しない例（変数からの代入・空文字・説明文・`${...}` の参照）で終了コード0を確認した。
  記録先は、アプリのコードではない開発用スクリプトで、BL-064 の完了条件が CHANGELOG.md を指定しているため CHANGELOG.md とした。
  Kotlin・Gradle に触れないため Gradle 系の品質ゲートは省略した
- ブラッシュアップの計画として、アプリの保守性・テスト（BL-070〜BL-072）、判定待ち中の検知の取りこぼし（BL-071）、
  開発支援のスキル・サブエージェント・スクリプト（BL-064〜BL-069・BL-073）を BACKLOG へ起票した。人の判断が要る
  開示文の版数（BL-074）、release 版の署名（BL-075）、CI の導入（BL-076）、`.claude/settings.json` の権限と hooks（BL-077）は
  要確認として起票した。コード変更なし
- BL-023（第2段階・Play 版の実機確認）を完了した。Pixel 8 Pro（Android 17）で P6〜P10・P12 と Play 版の G0〜G5 を確認し、結果を
  docs/TESTING.md の記録欄へ記録した。広告画面は、広告 SDK の Activity 名・ラベル・カウントダウン・閉じるボタンを持つ使い捨ての
  検証用アプリで再現した（実広告は使っていない）。docs/TESTING.md の P10（バッジ自体のタップは「×」の表示。BL-054）と
  G1（配布版の表示は「このアプリについて」）の期待値を現在の仕様へ直した。コード変更なし
- 実機（Pixel 8 Pro・Android 17）で BL-009 の T9〜T12・T14 を確認し、結果を docs/TESTING.md の記録欄と BL-009 の根拠へ記録した。
  ページの読み込み直後は操作しないとスケアウェアの警告が出ない不具合を BL-063 として起票した。T13・T15 は利用者の操作が
  要るため未実施。コード変更なし
- BL-062（起動済みのブラウザで外部遷移が検知されない）を実機で検証して完了した。Chrome のウィンドウ状態の変化イベントだけを
  一時的に捨てた状態で、補正なしでは 3 回とも検知されず（+0 件）、補正あり（`promoteToForeground`）では 4 回とも検知された
  （起動済み・コールドの両方）。通常のイベント順でも 12 回の負荷試験で毎回 1 件ずつ記録され、二重検知は無かった。
  Chrome がイベントを出さない原因そのものは未特定だが、欠けても検知できるようになった。コード変更なし（検証のみ）
- 起動済みのブラウザ・ストアへの遷移が検知されないことがある不具合を BL-062 として起票した（コールド起動では検知される。
  前面判定が古く残る疑い）。コード変更なし
- 実機（Pixel 8 Pro・Android 17）で T1〜T7・P1〜P5・P11・B1〜B4・B8 を確認し、結果を docs/TESTING.md の記録欄と BACKLOG
  （BL-009・BL-023・BL-043）へ記録した。見つけた不具合を BL-059（最小化中のバッジの縦ドラッグで最小化が解除される）、
  BL-060（アクセシビリティ設定の本アプリの画面で自身の警告が出る）、BL-061（ストアへの遷移で検知が二重記録され戻りすぎる）
  として起票した。コード変更なし
- Play 版の公開判断（BL-024）を記録した。事前相談はせず先に Play Console へ申請し、審査で `docs/design/DISTRIBUTION.md` 6章の
  未確認点を確かめる（申請は BL-031、却下時は理由を記録して機能の絞り込みか案 A への切り替えを起票する）。
  BACKLOG から BL-024 を削除し、DESIGN.md と DISTRIBUTION.md の参照を更新した。コード変更なし
- 「戻る／このまま進む」の確認を省く対象を追加した（BL-055）。確認バナーの「今後は確認しない」から、遷移元のアプリ
  （パッケージ名）とブラウザで開いたサイト（ドメインのみ・照合用のハッシュと PIN で暗号化した控えで保存。一覧は PIN を入力したときだけ復号して表示し、PIN の削除で消える）を保護者 PIN で登録でき、
  登録済みなら確認を出さずログだけ残す。自動で戻す介入・警告のみ・偽の警告・インストーラーへの遷移は省かない。
  アドレスバーの判定のため `flagReportViewIds` を追加し、開示文・設定画面・PRIVACY.md・DESIGN.md（FR-25）・
  TESTING.md（2.9）を更新した
- ガードレール配布元 copilot-rules を 0.29.0 へ追従した。`.markdownlint-cli2.yaml`（`gitignore: true` の追加）、
  `CONTRIBUTING.md`（Copilot にも効かせる固有規約の置き場所）、`docs/guidelines/ADOPTION.md`（`.github/copilot-instructions.md`
  をマージ扱いへ変更、`CONTRIBUTING.local.md` の作成手順）を配布元版で置き換えた。規範そのものの追加・削除は無く、
  `CLAUDE.md` の品質ゲート定義と `.github/copilot-instructions.md`（固有の追記なし、配布元と同一）は変更していない。
  常時読み込みのファイルは変更していない
- `CONTRIBUTING.local.md` を雛形から作成した（規定ブランチ `main`、レビュー責任者は全担当 `@filderschoice`、
  自律ループ実行モードは採用）。`.github/CODEOWNERS` の雛形の値（`@your-org/...`）も同じ担当者へ置き換えた
- バッジを 64dp（のぞき分 32dp）に大きくしてドラッグしやすくし、バッジをタップすると隣に「×」を出して閉じられるようにした。BL-054
- GitHub 版のアプリアイコンを「タコを食べるみみづく」にした（Play 版は通常のみみづく）。ホーム上部のアイコンにも同じ絵を使い、README・PRIVACY・UI デザイン設計書・DESIGN.md を最新化。BL-053
- 実機確認で見つけた不具合（同意文の改行の崩れ、回転でホームへ戻る）を直し、文字を大きくし、絵文字アイコン・丸いカード・みみづくの表情でポップにした。版の表示を上部のアイコンに替え、「このアプリについて」へ GitHub への導線を追加。BL-046〜BL-051
- 画面の説明文を「必要な情報」と「補足説明」に分け、補足をカード右上の「ⓘ」で開閉する形にした（ホーム・設定）。UI デザイン設計書と DESIGN.md（FR-22）も更新。BL-044
- みみづくバッジを指でドラッグして動かせるようにした（離すと左右の端へ吸着し、縦位置を割合で保存して縦向き・横向きに追従）。画面の外へ引き出すと端へ最小化（28dp をのぞかせ、最小化中もドラッグ・タップで操作できる）。BL-040
- 通知に「バッジを隠す／出す」ボタンを追加し、設定画面に通知のスイッチを追加した（通知権限 `POST_NOTIFICATIONS` を宣言し、オンにしたとき Android 13 以降は求める）。BL-041
- DESIGN.md（FR-18 の更新と FR-21）・README・PRIVACY.md・docs/TESTING.md（2.7 バッジの確認項目 B1〜B8、旧 2.7 を 2.8 へ）・
  UI デザイン設計書・MVP_DESIGN.md の権限の記述を最新化した。BL-042
- UI を整理した（ネイビー基調のダーク対応テーマ、ホーム・設定・このアプリについての画面構成、歯車と「⋮」メニュー、オーバーレイの主ボタンの配色、丸みのあるバッジ・アイコン）。BL-032〜BL-036
- UI デザイン設計書（docs/design/UI_DESIGN.html）を新しい UI に追従させ、所見の対応状況を反映した（BL-037）
- PLAN.md をもとに MVP（外部遷移の検知とスケアウェア文言の警告）のタスクを BACKLOG へ分解した
- PLAN.md 8章への回答と MVP の設計を docs/design/MVP_DESIGN.md にまとめた（大人の利用を考慮した利用モードを含む）
- README.md（セットアップ・サイドロード・更新・ロールバック）と docs/TESTING.md（単体テストと実機確認の手順）を追加した
- DESIGN.md を MVP の実装済み内容（FR-1〜FR-9）で最新化した
- 第2段階（保護者 PIN を先に、広告画面の判定・タップシールド・代理タップ・バッジ）を BL-014〜BL-023 へ分解した
- 脆弱性チェックは未導入のまま、リポジトリ公開後に GitHub の Dependabot で確認する方針とした（BL-011 を閉じた）
- README・docs/TESTING.md・DESIGN.md を第2段階（PIN・一時停止・ログ閲覧・広告画面・シールド・代理タップ・バッジ）に合わせて更新した
- Google Play のポリシー調査と配布の構成案の作成を BL-026 として登録した
- Google Play のポリシーを調べ、Play 版（機能限定）と GitHub 版（全機能）に分ける案を含む配布の構成案を docs/design/DISTRIBUTION.md にまとめた（BL-026）
- 全画面広告では正常に見えても悪質な場合があるため、GitHub 版では全画面広告への介入を残す方針とした（ユーザー決定）
- Play 版と GitHub 版を同じソースからフラグで出し分ける作業を BL-028〜BL-031 へ分解した
- プライバシーポリシー（docs/PRIVACY.md）と Google Play のリスティング文案（docs/PLAY_LISTING.md）を追加した（BL-030）
