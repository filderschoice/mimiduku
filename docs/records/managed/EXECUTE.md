<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- date: 2026-10-19 13:00
  summary: 保護者向けの検知ログ閲覧画面を実装する（BL-017）
  details:
    変更内容: >-
      LogRows（ログを新しい順に並べ、日時の書式化・アプリ名の解決（分からなければパッケージ名）・検知の種類と
      介入の内容の文言を決める）と LogScreen（一覧、ログの消去、戻るキーで設定画面へ戻る）を追加した。
      ログの消去は PIN を求める。SettingsController に閲覧画面の状態を足し、操作の interface を
      SettingsActions と LogActions に分けた。あわせて Google Play での配布の検討を BL-024（要確認）として登録した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/LogRows.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/LogScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の interface の関数の数の指摘を直して再実行し、いずれも終了コード0、単体テスト95件成功
    関連ID:
      - BL-017
      - BL-024
- date: 2026-10-19 12:00
  summary: PIN による保護の一時停止を実装する（BL-016）
  details:
    変更内容: >-
      PauseStore（15・30・60分の一時停止。端末の時刻で期限を持ち再起動後も保つ。時刻が開始より前へ戻された場合・
      期限切れ・壊れた保存値では停止を終える）を追加し、サービスは停止中のイベントを処理しないようにした。
      設定画面に「一時停止」のカード（停止中の残り時間と再開ボタン）を加え、停止には PIN を求め、再開には求めない。
      detekt の指摘（関数の長さ・引数の数）に合わせ、設定画面の状態と操作を SettingsController へまとめ、
      SettingsActions を interface にし、MainActivity は SettingsPlatform の実装と画面の組み立てだけにした。
      detekt の TooManyFunctions に ignoreOverridden を設定した（interface の実装を数に含めない）。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/PauseStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/mode/PauseStoreTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - config/detekt/detekt.yml
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の指摘3件と、壊れた保存値を消さない不具合（単体テストで検出）を直して再実行し、
      いずれも終了コード0、単体テスト92件成功
    関連ID:
      - BL-016
- date: 2026-10-19 11:10
  summary: 設定画面を保護者 PIN で保護する（BL-015）
  details:
    変更内容: >-
      PinPolicy（保護を弱める操作＝おとなモードへの切替・PIN の変更と削除・一時停止・ログの消去だけ PIN を求め、
      こどもモードへの切替と再開は求めない。照合結果ごとの文言）を追加した。設定画面に「保護者 PIN」の
      カードと PIN の入力・設定ダイアログ（数字のみ・伏せ字・2回入力）を加え、MainActivity で操作ごとに PIN を求める。
      画面部品は settings パッケージの SettingsScreen.kt・PinDialogs.kt へ分けた。PIN を忘れた場合は
      アプリの入れ直しで復旧すると画面に示す。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinPolicy.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinDialogs.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/PinPolicyTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の関数の長さの指摘を直して再実行し、いずれも終了コード0、単体テスト75件成功
    関連ID:
      - BL-015
- date: 2026-10-19 10:30
  summary: 保護者 PIN の保存と照合を実装する（BL-014）
  details:
    変更内容: >-
      PinHasher（塩16バイト・PBKDF2WithHmacSHA256・反復10万回、保存形式 v1:反復回数:塩:ハッシュ）と
      PinManager（4〜8桁の数字、5回続けて間違えたら30秒、以降は倍で上限30分の待ち時間、成功で失敗回数を戻す、
      保存値が壊れた場合は PIN なしとみなさず照合不能にする）を追加した。StringStore を storage パッケージへ移し、
      remove を足した。テスト用の MemoryStringStore はテスト側へ置いた。
      待ち時間は端末の時刻で数えるため、時刻を進めると短くなる（時刻の変更には設定画面の操作が要る）。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/pin/PinHasher.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/pin/PinManager.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/storage/StringStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/UsageModeStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/pin/PinManagerTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/storage/MemoryStringStore.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/mode/UsageModeTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の return の数の指摘を直して再実行し、いずれも終了コード0、単体テスト70件成功
    関連ID:
      - BL-014
- date: 2026-10-18 17:40
  summary: 保護者・利用者向けの設定画面を実装する（BL-007）
  details:
    変更内容: >-
      MainActivity を Compose の設定画面にした。見張りの状態（アクセシビリティのオン/オフ）とアクセシビリティ設定を
      開くボタン、Android 13 以降でオフのときの「制限された設定」の解除手順とアプリ情報を開くボタン、
      利用モード（こども／おとな）の切り替え、検知ログの件数とプライバシーの説明を出す。画面へ戻るたびに状態を
      読み直す。状態判定は Android 非依存の SetupStatus に置いた（短いクラス名の形・debug 版の別パッケージにも対応）。
      ktlint が Composable 関数の命名を指摘するため、sesami-wear と同じ .editorconfig を置いた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SetupStatus.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SetupStatusTest.kt
      - mobile/src/main/res/values/strings.xml
      - .editorconfig
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt（引数の数・行長）と ktlint（Composable の命名）の指摘を直して再実行し、
      いずれも終了コード0、単体テスト57件成功
    関連ID:
      - BL-007
- date: 2026-10-18 17:00
  summary: AccessibilityService と警告バナーを実装し、判定ロジックへ接続する（BL-006）
  details:
    変更内容: >-
      MimidukuAccessibilityService を追加し、イベントを Android 非依存の ProtectionEngine へ渡して、
      検知結果に従って GLOBAL_ACTION_BACK・警告バナー・ログ保存（別スレッド）を行うようにした。
      ProtectionEngine はイベントの変換、外部遷移の判定、前面アプリだけを対象にしたスケアウェア照合
      （1秒間隔の間引き、同じアプリ・同じ文言は60秒繰り返さない）を受け持つ。NodeTextCollector はノード数400・
      深さ40を上限に文字を集める。OverlayController は TYPE_ACCESSIBILITY_OVERLAY で画面上部にバナーを出し、
      BannerSpec が利用モードと介入内容から文言・ボタンを決める。警告文言はこども向けとおとな向けに分けて
      strings.xml へ置いた。accessibility_service_config.xml、サービスの宣言と queries、
      dataExtractionRules（バックアップ・端末間転送から全データを除外）を追加した。
      作業中に補助スクリプトの置換範囲の誤りで未コミットの ProtectionEngine.kt を壊したため、削除して書き直した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/NodeTextCollector.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BannerSpec.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/res/layout/overlay_banner.xml
      - mobile/src/main/res/drawable/overlay_banner_background.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/xml/accessibility_service_config.xml
      - mobile/src/main/res/xml/data_extraction_rules.xml
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - docs/design/MVP_DESIGN.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の指摘（入れ子の深さ・行長・return の数）を直して再実行し、いずれも終了コード0、
      単体テスト52件成功。Android Lint の警告（アイコン未設定・依存の新版あり等）は残るが、合否基準の対象外
    関連ID:
      - BL-006
- date: 2026-10-18 16:20
  summary: 大人も使えるように利用モード（こども／おとな）を実装する（BL-013）
  details:
    変更内容: >-
      UsageMode（既定はこども）と InterventionPolicy を追加した。こどもモードは外部遷移を自動で戻して警告し、
      同じ遷移元から30秒以内に4回目以降の遷移が来たら警告だけにする。おとなモードは自動では戻さず、
      「戻る」「このまま進む」を出す。スケアウェアはどちらのモードでも警告だけにする。
      UsageModeStore は StringStore 経由で SharedPreferences へ保存し、未知の保存値は既定へ戻す。
      検知ログに利用モードの項目を足した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/UsageMode.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/UsageModeStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/log/DetectionLog.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/mode/UsageModeTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/log/DetectionLogTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 初回は detekt の行長の指摘1件で失敗し、テストの書き方を直して再実行。
      いずれも終了コード0、単体テスト42件成功
    関連ID:
      - BL-013
- date: 2026-10-18 16:00
  summary: 検知ログの保存を実装する（BL-005）
  details:
    変更内容: >-
      DetectionLog を追加した。アプリ内ストレージの JSON ファイルへ件数上限（既定500件）つきで保存し、
      一時ファイルへ書いてから ATOMIC_MOVE で置き換える。読めないファイルは空として扱い、次の追記で置き換える。
      ログの項目は日時・パッケージ名・検知の種類・ルール ID・介入の内容・直前のクリックの有無に限り、
      画面の文字・URL を持たないことを単体テストで確かめる。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/log/DetectionLog.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/log/DetectionLogTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 初回はログ項目の検査テストの期待値誤り（既定値の項目は出力されない）で1件失敗し、
      期待値を「許可した項目以外が無いこと」へ直して再実行。いずれも終了コード0、単体テスト36件成功
    関連ID:
      - BL-005
- date: 2026-10-18 15:40
  summary: 前面パッケージの変化から外部遷移を判定する処理を実装する（BL-004）
  details:
    変更内容: >-
      PackageClassifier（ルールと端末から得たブラウザ・ランチャー・キーボードの一覧で、ブラウザ・ストア・
      インストーラー・一時的な前面・除外する遷移元を判別）と ExternalTransitionDetector（前面の変化から
      外部遷移を判定し、直前のクリックの有無と同じ遷移元からの30秒以内の繰り返し回数を付ける）を追加した。
      MVP_DESIGN.md 5章のクールダウンを「繰り返しの上限」に具体化し、ブラウザからの遷移の扱いを追記した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/PackageClassifier.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/ExternalTransitionDetector.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/detect/ExternalTransitionDetectorTest.kt
      - docs/design/MVP_DESIGN.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0、単体テスト28件成功、Markdown は 0 issues
    関連ID:
      - BL-004
- date: 2026-10-18 15:10
  summary: 検知ルールの JSON 化とスケアウェア文言の照合を実装する（BL-003）
  details:
    変更内容: >-
      検知ルール（ブラウザ・ストア・インストーラー・一時的な前面・除外する遷移元・スケアウェア文言）を
      DetectionRules として定義し、assets/rules.json に同じ内容を置いた。RulesLoader は空・壊れた JSON・
      必須の項目の欠け・未対応の版のときに既定のルールへ戻す。ScarewareMatcher は NFKC・小文字化・空白除去で
      表記ゆれを吸収し、ノードをまたいだ文言も照合する。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/rules/DetectionRules.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/rules/RulesLoader.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/rules/ScarewareMatcher.kt
      - mobile/src/main/assets/rules.json
      - mobile/src/test/kotlin/com/mimiduku/mobile/rules/RulesLoaderTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/rules/ScarewareMatcherTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0、単体テスト13件成功、Markdown は 0 issues
    関連ID:
      - BL-003
- date: 2026-10-18 14:30
  summary: Gradle プロジェクトの雛形と品質ゲートを用意する（BL-001）
  details:
    変更内容: >-
      mobile モジュール（applicationId com.mimiduku.mobile、debug は .debug サフィックス、minSdk 29、
      targetSdk 36）と最小の MainActivity（Compose）を追加した。ktlint・detekt を全サブプロジェクトへ適用し、
      Gradle Wrapper 8.13 と detekt 設定、.gitignore（local.properties・鍵ファイル等を除外）を置いた。
      CLAUDE.md の品質ゲート定義を Gradle の実コマンドで確定した（脆弱性チェックは BL-011 で要確認）。
      ユーザー指示により大人の利用を考慮した利用モード（BL-013）を BACKLOG へ追加した。
    変更ファイル:
      - build.gradle.kts
      - settings.gradle.kts
      - gradle.properties
      - gradle/libs.versions.toml
      - gradle/wrapper/gradle-wrapper.jar
      - gradle/wrapper/gradle-wrapper.properties
      - gradlew
      - gradlew.bat
      - config/detekt/detekt.yml
      - .gitignore
      - mobile/build.gradle.kts
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - CLAUDE.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0、Markdown は 0 issues
    関連ID:
      - BL-001
      - BL-013
```
<!-- COPILOT_RECORDS:END -->
