<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
- date: 2026-11-23 23:00
  summary: アプリ内広告の部分シールドを見送ると決め、ShieldTimer のコメントを直す（BL-025）
  details:
    変更内容: >-
      AdMob 公式サンプル2件の実機調査（2026-11-23）の結果を受け、ユーザーがアプリ内バナー・ネイティブ広告の部分シールドを見送ると決めた。
      ShieldTimer.kt のコメント「実機での確認が済むまで張らない」を「領域を誤ると子供が操作できなくなるおそれがあるため張らない」へ直した。
      コメントのみの変更で、動作は変わらない。DESIGN.md の要件トレーサビリティ要件の記述も合わせた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/ShieldTimer.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
      - .claude/skills/mimiduku-device-verification/SKILL.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、npx markdownlint-cli2、
      python scripts/validate-records.py、python scripts/scan-secrets.py --require-changes
    検証結果: >-
      成功 - 軽い検証は成功（compileGithubDebugKotlin は実行。コメントのみの変更でコンパイル結果が同じため testGithubDebugUnitTest は UP-TO-DATE）。markdownlint は 0 件、validate-records.py は成功、scan-secrets.py は検出なし
    関連ID:
      - BL-025
- date: 2026-11-23 22:00
  summary: バッジ・アイコンの自作画像を本素材として確定し、リソースのコメントの「仮」を外す（BL-038）
  details:
    変更内容: >-
      ユーザーが、バッジ（badge_owl_alert・badge_owl_sleepy・badge_owl_wide）とアプリアイコンの前景の自作画像を本素材として
      確定すると決めたため、各 drawable の先頭コメントの「仮の自作画像」を「自作画像（2026-11-23 に本素材として確定）」へ直した。
      コメントのみの変更で、描画・ビルド成果物の動作は変わらない。DESIGN.md と UI_DESIGN.html（所見4・8を解決へ）も合わせた。
    変更ファイル:
      - mobile/src/main/res/drawable/badge_owl_alert.xml
      - mobile/src/main/res/drawable/badge_owl_sleepy.xml
      - mobile/src/main/res/drawable/badge_owl_wide.xml
      - mobile/src/main/res/drawable/ic_launcher_foreground.xml
      - mobile/src/github/res/drawable/ic_launcher_foreground.xml
      - docs/design/UI_DESIGN.html
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/EXECUTE.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、npx markdownlint-cli2、
      python scripts/validate-records.py、python scripts/scan-secrets.py --require-changes
    検証結果: >-
      成功 - 軽い検証は成功（リソースの処理のタスクを実行）。markdownlint は 0 件、validate-records.py は成功、scan-secrets.py は検出なし
    関連ID:
      - BL-038
      - BL-100
- date: 2026-11-23 12:00
  summary: debug ビルドのアプリ名とアクセシビリティサービス名の末尾に「 (debug)」を付ける（BL-099）
  details:
    変更内容: >-
      実機検証でリリース版と debug 版を端末上で見分けられるよう、debug ソースセット（mobile/src/debug/res/values/strings.xml）で
      app_name と accessibility_service_label を上書きした。マニフェストの android:label は両方とも文字列リソース参照のため、
      ランチャー名・アクセシビリティ設定画面のサービス名・ホーム画面の見出し（HomeScreen の app_name）の全てに効く。
      GitHub 版・Play 版の debug の両方に効き、release は変わらない（main の値のまま）。コードの変更は無い。
    変更ファイル:
      - mobile/src/debug/res/values/strings.xml
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、npx markdownlint-cli2、
      python scripts/validate-records.py、python scripts/scan-secrets.py --require-changes
    検証結果: >-
      成功 - ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest は成功（リソースの統合と処理のタスクは実行され、Kotlin のコンパイルとテストは変更が無いため UP-TO-DATE）。processPlayDebugResources は成功。統合後の values.xml に「みみづく (debug)」が入っていることを確認。markdownlint は 0 件（40ファイル）、validate-records.py は成功、scan-secrets.py --require-changes は検出なし。実機での表示は別の作業（edf0165）で uiautomator dump により確認済みで、「(debug)」が出た
    関連ID:
      - BL-099
- date: 2026-11-14 00:15
  summary: サービスの終了時に残す検知ログの行を、種別は ASK_USER のまま印で見分けてログの画面に「保護の終了時のため確認なし」と出す（BL-086）。鍵の兼用をユーザーの決定として記録する（BL-085）
  details:
    変更内容: >-
      (1) BL-086。ユーザーの決定「(b) 種別はそのまま、文言で分かるようにする」により実装した。既存の項目（日時・パッケージ名・種類・
      ルール ID・介入・直前のクリック・利用モード）は確認バナーを出した通常の ASK_USER の行と同じ値になり区別できないため、
      DetectionLogEntry へ省略可能な真偽値 recordedAtServiceEnd（既定 false）を1つだけ足した。中身は終了時に記録したという事実だけで、
      個人情報やドメインなど新しい情報は持たない。InterventionAction の値は増やしていない。DomainWait.finish は Detection.atServiceEnd()
      で印を立ててから記録し、LogRows は印があり種別が ASK_USER の行の介入を strings.xml の log_action_ask_user_at_service_end
      「保護の終了時のため確認なし」にする（既存の「登録済みのため確認なし」に調子を合わせた。他の種別は印があっても文言を変えない）。
      ログの書き出し・共有の機能は無いため対象外。版による違いは無い（FeatureSet を使わず両方の版で同じ）。開示文と settings_log_privacy の
      文面は変えていない（新しい情報を保存しないため）。DESIGN.md の FR-8（保存形式と互換）・FR-25、TESTING.md の 1章と X16、
      UI 設計書の検知ログ画面の対応表、Skill mimiduku-device-verification のログの読み方を更新した。BACKLOG の BL-056 に X16 の文言の確認を足した。
      互換性。保存は kotlinx.serialization の既定（encodeDefaults=false）で既定値を書き出さないため、通常の行の保存形式は変わらない。
      印の無い古い行は false として読む（アプリの更新）。読み込みの Json は初版（fc7e3a6）から ignoreUnknownKeys = true で、
      git log -S で外した履歴が無いことを確かめた。このため印を知らない古い版は印を無視して全件を読み、その行は従来どおり
      「利用者に確認」と出る（ダウングレード。ログ全体を失わない）。古い版が次に追記すると、書き直しで印が落ち、その後に新しい版へ戻しても
      その行は「利用者に確認」と出る（表示の情報が戻るだけで、件数は失わない）。リリースのタグは無く versionCode は 1 のまま。
      (2) BL-085。根拠の「鍵を GitHub 版の配布鍵と Play 版のアップロード鍵に兼用する」を、担当の既定値から 2026-11-14 のユーザーの決定へ
      書き換え、DESIGN.md の FR-19 の該当箇所にも決定日を足した。README.md・build.gradle.kts は事実として書かれており仮の前提の書き方は
      無いため変えていない。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/log/DetectionLog.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DomainWait.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/LogRows.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/log/DetectionLogTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/DomainWaitTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/LogRowsTest.kt
      - docs/TESTING.md
      - docs/design/UI_DESIGN.html
      - .claude/skills/mimiduku-device-verification/SKILL.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、フル検証として ./gradlew ktlintCheck、
      ./gradlew detekt、./gradlew lintGithubDebug lintPlayDebug、./gradlew assembleDebug、./gradlew testGithubDebugUnitTest testPlayDebugUnitTest
      （test-results を消してから実行）、npx markdownlint-cli2、python scripts/validate-records.py、python scripts/test-guard-commands.py、
      python scripts/scan-secrets.py（ステージ済みの差分）、python scripts/scan-secrets.py --range main...HEAD。古い版の読み込みは
      DetectionLogTest で印の無い型（BL-086 より前の DetectionLogEntry と同じ項目）と同じ Json の設定を使って再現した。
    検証結果: >-
      成功 - 軽い検証は1回目に detekt の LongParameterList（LogRowsTest の補助関数）で失敗し、直して成功。フル検証の ktlintCheck・
      detekt・Lint（両版）・assembleDebug は成功（Lint・パッケージ・Play 版のコンパイルは実行され、GitHub 版の本体のコンパイルは同じ内容で
      軽い検証の1回目に実行済み）。単体テストは実行され GitHub 版 244件・Play 版 244件で失敗0（前回 238件から DetectionLogTest 4件・
      LogRowsTest 2件が増えた）。markdownlint は 36ファイルで0件、hook のテストは216件で失敗0件。実機での X16 の表示は未確認（BL-056）。
    関連ID:
      - BL-086
      - BL-085
- date: 2026-11-14 00:06
  summary: 差分レビューの指摘（release 署名のキーの不足、版の入力の検査、終了時の待ちの上限）を直し、終了時の検知ログの種別を要確認で起票する（BL-075・BL-081・BL-086）
  details:
    変更内容: >-
      (1) BL-075 の続き。mobile/build.gradle.kts は RELEASE_STORE_FILE だけでなく4つのキー（RELEASE_STORE_PASSWORD・
      RELEASE_KEY_ALIAS・RELEASE_KEY_PASSWORD を含む。空の値は無いものとみなす）がすべてあるときだけ署名の設定を作る。一部だけある場合は
      署名の設定を作らず、pre〜ReleaseBuild のタスクの実行時に、欠けているキーの名前だけ（値は出さない）を示して GradleException で
      失敗させる。debug のビルドと品質ゲートは通す。sesami-wear の方式（local.properties の同じキー名、rootProject.file で解決）は維持した。
      (2) scripts/release-build.ps1 は -VersionCode を文字列で受け、1 以上 2100000000（Google Play の上限）以下の整数でなければビルドに
      進まずに中止する（[int] で受けると 1.5 が丸められるため）。scripts/version.properties の空行・# の行以外で読めない行や、
      VERSION_CODE が整数でない行は、行番号と行を示して中止する。release-build.bat はコメントだけ足した。
      (3) README.md と docs/RELEASE_NOTES.md に、引数なしの実行では最初の配布の versionCode が 2 になること・1 にするなら初回に
      -VersionCode 1 を指定すること、RELEASE_STORE_FILE の相対パスはリポジトリの直下を基準に解決することを書いた。
      (4) BL-081 の続き。MimidukuAccessibilityService の LOG_FLUSH_MILLIS を 1000ms から 500ms に下げ、onDestroy がメインスレッドで
      呼ばれ最大でこの時間止めること（通常は待ち行列が空ですぐ戻る）を KDoc に書いた。DESIGN.md の FR-25 も合わせた（TESTING.md の
      「1秒」はサイトの判定待ち DomainWait.SETTLE_MILLIS の記述で、この待ちとは別のため変えていない）。
      (5) BL-086 を起票した（終了時の検知ログの介入の種別が ASK_USER になる件。要確認）。
    変更ファイル:
      - mobile/build.gradle.kts
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - scripts/release-build.ps1
      - scripts/release-build.bat
      - README.md
      - docs/RELEASE_NOTES.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      署名の設定は、リポジトリを作業用フォルダへ git clone して変更後の build.gradle.kts を写し、その複製の中だけに仮の値の local.properties
      を作って確かめた（リポジトリの local.properties は開かず変更していない）。一部のキーで ./gradlew :mobile:assembleGithubDebug と
      ktlintCheck detekt lintPlayDebug、:mobile:assembleGithubRelease、キー無しと仮の4キーで :mobile:assembleGithubRelease。
      スクリプトは作業用フォルダへ写した release-build.ps1 を、壊した version.properties と不正な -VersionCode で pwsh と
      Windows PowerShell 5.1 から実行した（gradlew.bat の無い場所のためビルドには進まない）。リポジトリではフル検証の全ゲート。
    検証結果: >-
      成功 - 一部のキー（RELEASE_STORE_FILE・RELEASE_KEY_ALIAS だけ）では debug のビルドと ktlint・detekt・Lint は成功し、release は
      preGithubReleaseBuild で「足りないキー RELEASE_STORE_PASSWORD、RELEASE_KEY_PASSWORD」を示して失敗した。キー無しでは署名なしの
      APK ができた。仮の4キーでは署名の設定が作られ、鍵ストアが複製の直下を基準に解決されて見つからないことで失敗した（想定どおり）。
      スクリプトは 0・-5・abc・1.5・99999999999 の -VersionCode、読めない行・VERSION_CODE=x・VERSION_CODE=0・空の VERSION_NAME で
      いずれも中止し、正しい入力はビルドの手前（gradlew.bat が無い）まで進んだ。実際の鍵での release ビルドは未確認（BL-085）。
      リポジトリのフル検証は ktlintCheck・detekt・lintGithubDebug lintPlayDebug・assembleDebug が成功（コンパイルと Lint、パッケージの
      タスクは実行された）、単体テストは GitHub 版 238件・Play 版 238件ですべて成功、markdownlint は 36ファイルで0件、記録ファイルの
      検証は成功、hook のテストは216件で失敗0件、秘密情報スキャン（ステージ済みの差分とブランチ全体）は成功。
    関連ID:
      - BL-075
      - BL-081
      - BL-086
- date: 2026-11-13 23:55
  summary: release 署名と版の管理を sesami-wear と同じ方式で入れ、鍵ストアの追加を秘密情報スキャンで検出する（BL-075）
  details:
    変更内容: >-
      ユーザーの決定「鍵をユーザーが作成・保管する」「versionCode は既存の規則を維持し sesami-wear を前提にする」により実装した。
      sesami-wear はビルドスクリプト・.gitignore・README・scripts だけを読み取り（鍵や値の入ったファイルは開いていない）、同じ方式に
      揃えた。(1) mobile/build.gradle.kts は local.properties（.gitignore の対象）の RELEASE_STORE_FILE・RELEASE_STORE_PASSWORD・
      RELEASE_KEY_ALIAS・RELEASE_KEY_PASSWORD を読み、RELEASE_STORE_FILE があるときだけ signingConfigs の release を作って release
      ビルドへ設定する（無ければ署名なし。debug と品質ゲートは影響なし）。GitHub 版・Play 版は同じ鍵で署名する。versionCode・versionName は
      -PappVersionCode・-PappVersionName で上書きでき、既定は従来の 1・0.1.0。(2) scripts/version.properties（VERSION_CODE=1・
      VERSION_NAME=0.1.0）と scripts/release-build.bat・release-build.ps1 を追加した。versionCode を1つ上げて（指定時はその値、
      現在値より小さければ中止）assembleGithubRelease と bundlePlayRelease を実行し、成功したときだけ version.properties を LF で更新する。
      sesami-wear との違いは、wear の別系列（1001始まり）が無く両方の版で共通の1系列であること、現在値より小さい指定を止める検査を
      足したこと。(3) .gitignore に *.p12・*.pfx・*.bks を足し、scripts/scan-secrets.py の鍵ストアのファイル名の規則を
      jks・keystore に p12・pfx・bks を加えた形にした。(4) README.md に「リリースビルド」（鍵の作成と保管、local.properties のキー、
      版の上げ方、成果物、署名の確かめ方）を書き、更新とロールバックの release 版の記述を直した。sesami-wear と同じ構成で
      docs/RELEASE_NOTES.md（位置づけと運用ルール。配布前のため記録なし）を追加した。CONTRIBUTING.local.md の文書構成と DESIGN.md の
      FR-19・運用制約を更新した。実際の鍵での release ビルドの確認は BL-085（人手検証）として起票し、BL-031 の依存へ加えた。
      Gradle プラグインと依存の追加は無い。鍵の作成・入力と release ビルドは行っていない。
    変更ファイル:
      - mobile/build.gradle.kts
      - scripts/version.properties
      - scripts/release-build.bat
      - scripts/release-build.ps1
      - scripts/scan-secrets.py
      - .gitignore
      - README.md
      - docs/RELEASE_NOTES.md
      - CONTRIBUTING.local.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、
      ./gradlew --rerun-tasks --continue ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest
      testPlayDebugUnitTest --console=plain（フル検証。強制再実行）、一時リポジトリの空ファイルでの python scripts/scan-secrets.py、
      git check-ignore -v、PowerShell の構文解析（release-build.ps1。実行はしない）、npx markdownlint-cli2 "**/*.md" --config
      .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/test-guard-commands.py、python scripts/scan-secrets.py、
      python scripts/scan-secrets.py --range main...HEAD
    検証結果: >-
      成功 - フル検証は BUILD SUCCESSFUL（強制再実行。ktlint の main・test・スクリプトの検査が実行された）、単体テストは GitHub 版 238件・
      Play 版 238件がすべて成功。一時リポジトリでは .jks・.KEYSTORE・.p12・.pfx・.bks の空ファイルの追加を5件とも検出して終了コード1、
      p12.txt・jks.kt・keystore-guide.md などでは検出なしの0。local.properties と各鍵ストアの拡張子が .gitignore で除外されることを確認。
      release-build.ps1 は構文エラー0。markdownlint 0件、記録ファイルの検証・hook のテスト 216件・秘密情報スキャンは成功。
      実際の鍵での release ビルドと署名の確認は未確認（BL-085 の人手検証）。
    関連ID:
      - BL-075
- date: 2026-11-13 18:52
  summary: サービスの終了時にサイトの判定待ちだった検知を、バナーを出さずに検知ログへ残す（BL-081）
  details:
    変更内容: >-
      ユーザーの決定「BL-081 は直す」により実装した。DomainWait に finish（予約を取り消し、待ち中の検知があれば、保護が止まって
      いないときだけ afterDomain にドメイン未取得を渡した結果の検知を新しい引数 record でログへ残す）を足し、
      MimidukuAccessibilityService.onDestroy は domainCheck.cancel の代わりに domainWait.finish を呼ぶ（接続前で engine が無いときは
      従来どおり取り消しだけ）。ログの書き込みは logExecutor（単一スレッド）で行うため、finish を logExecutor.shutdown より前に
      呼び（shutdown の後の execute は拒否されるため）、shutdown の後に awaitTermination で最大1秒（LOG_FLUSH_MILLIS）書き終えるのを
      待つようにした（shutdown は依頼済みの書き込みを捨てないが、サービスの終了後はプロセスが止められ得るため）。利用者に見える変化は、
      サイトを1件以上登録した大人モード等で、ブラウザへの遷移から1秒以内にアクセシビリティをオフにした・システムに止められた場合に、
      その遷移が検知ログに1件（介入は「利用者に確認」）残るようになったこと。バナーは出ない（サービスの終了でオーバーレイを閉じるため）。
      待ち中に一時停止・同意の取り消しがあった場合は従来どおり何も残さない。先に DomainWaitTest へ再現テストを足し、finish が予約の
      取り消しだけ（修正前の onDestroy と同じ）の状態で失敗することを確かめてから直した。docs/TESTING.md 2.9 に X16 を足し、
      1章の DomainWaitTest の説明を更新した。実機での確認は BL-056 の人手検証に含めた。DESIGN.md の FR-25 を更新した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DomainWait.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/DomainWaitTest.kt
      - docs/TESTING.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew testGithubDebugUnitTest --tests "com.mimiduku.mobile.service.DomainWaitTest" --console=plain（修正前の再現）、
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 修正前は追加した「サービスの終了で切られた待ちがログに残る」テストが失敗（8件中1件）し、修正後の軽い検証は
      compileGithubDebugKotlin・compileGithubDebugUnitTestKotlin・testGithubDebugUnitTest が UP-TO-DATE ではなく実行され、GitHub 版
      238件が成功・失敗0。実機での確認（X16）は未確認（BL-056 の人手検証）。Play 版の単体テスト・Android Lint・assembleDebug は
      段階「最後」のためループの最後の全ゲートで確かめる。
    関連ID:
      - BL-081
- date: 2026-11-13 18:44
  summary: 差分レビューの小さな指摘と文書の食い違いを直し、BL-078 を閉じる（BL-083・BL-078）
  details:
    変更内容: >-
      BL-083 は (1) mimiduku-change 4章を「自律ループでは autonomous-loop と CLAUDE.md の段階、対話実行は軽い検証を除く全ゲート」に直した。
      (2) backlog-implementer に、同じコミットで BACKLOG から削除するタスクは進行中への更新を省いてよいことと、単体テストの件数を
      mobile/build/test-results の TEST-*.xml の testsuite 属性から版ごとに集計することを足し、autonomous-loop の手順2にも同じ例外を
      書いた（配布スキルへの固有の追記として CONTRIBUTING.local.md の表を更新）。(3) ConsentStoreTest の版数が 2 であることだけを
      確かめるテストを、現在の1つ前の版（CURRENT_VERSION - 1）の同意が無効になることを確かめるテストへ置き換えた（値を固定しない）。
      (4) scripts/scan-secrets.py に --range（git diff へ渡すコミット範囲。--include-unstaged と併用不可、- で始まる値は拒否）を足し、
      CLAUDE.md の品質ゲート表へ段階「最後」の行（python scripts/scan-secrets.py --range main...HEAD）を加え、quality-gate-runner に
      フル検証ではブランチ全体の行で判定することを書いた。BL-078 は、このセッションで diff-reviewer・quality-gate-runner・
      backlog-implementer の3つが実際に起動して定義どおりの報告を返したため閉じた（確かめた範囲は CHANGELOG.md）。
      DESIGN.md の FR-20 にテストの確かめ方を足した。
    変更ファイル:
      - mobile/src/test/kotlin/com/mimiduku/mobile/mode/ConsentStoreTest.kt
      - scripts/scan-secrets.py
      - CLAUDE.md
      - .claude/agents/quality-gate-runner.md
      - .claude/agents/backlog-implementer.md
      - .claude/skills/autonomous-loop/SKILL.md
      - .claude/skills/mimiduku-change/SKILL.md
      - CONTRIBUTING.local.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --rerun-tasks --continue ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest
      testPlayDebugUnitTest --console=plain（フル検証。UP-TO-DATE の素通りを避けるため強制再実行）、表のとおりの各 Gradle コマンド、
      一時リポジトリでの python scripts/scan-secrets.py --range base...HEAD、npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、
      python scripts/validate-records.py、python scripts/scan-secrets.py、python scripts/scan-secrets.py --range main...HEAD、
      python scripts/test-guard-commands.py
    検証結果: >-
      成功 - フル検証は全タスクが実行され（UP-TO-DATE は動作を持たないライフサイクルのタスクだけ）BUILD SUCCESSFUL、単体テストは
      GitHub 版 235件・Play 版 235件がすべて成功（置き換えたテストが両方の版で実行されたことを XML で確認）。--range は、明らかな
      ダミー値（password へのリテラル代入）を含むコミットの範囲で終了コード1と該当行、含まない範囲で0、--include-unstaged との併用と
      - で始まる値で2を返した。markdownlint 0件、記録ファイルの検証・秘密情報スキャン（ステージ済み・ブランチ全体）・hook のテスト
      205件はすべて成功。
    関連ID:
      - BL-083
      - BL-078
- date: 2026-11-13 18:19
  summary: スケアウェア文言の実機確認用テストページ・TESTING.md の配信手順・実機確認スキルを追加し、ページの本文を単体テストで確かめる（BL-073）
  details:
    変更内容: >-
      (1) tools/verification/scareware-page/ に index.html（読み込み直後に警告の文言）・scroll.html（下までスクロールすると template の中身を
      差し込む）・safe.html（T11 のリンク先、文言なし）を追加した。文言は rules.json の照合語（your device is infected・ウイルスに感染・
      virus detected・ウイルスを検出）だけを使った架空のもので、実在の名前・ロゴ・電話番号、外部の画像・スクリプト・リンクは無い。
      mobile/ の外に置くため APK に入らない（置き場所は BACKLOG の既定値）。(2) ScarewarePageTest を追加し、index.html の表示文字が
      DetectionRules.DEFAULT で一致すること、scroll.html は読み込み直後に一致せず template の中身は一致すること、safe.html は一致しない
      ことを確かめる（コメント・head・script・template を除いてタグを外した文字で照合）。(3) docs/TESTING.md 2.2 に手順3として、
      python -m http.server を 127.0.0.1 だけで待ち受けて adb reverse tcp:8000 tcp:8000 で端末の Chrome から開く手順と後片付けを足し、
      T9 の手順をテストページへ向け、1章の一覧へ ScarewarePageTest を足した。(4) プロジェクト固有スキル mimiduku-device-verification を
      作成した（BACKLOG の人手検証と TESTING.md の節の対応、入れる版、素材、検知ログをテキストで読む方法、元へ戻すこと、記録先）。
      adb 接続と利用量の抑え方はユーザー共通スキル adb-device-connection・verification-cost-control を参照し、複製していない。
      全体を通して実機で使っていないことをスキル内に明記した。mimiduku-change の3章から新しいスキルを参照した。DESIGN.md の FR-4 へ
      テストページとテストを足した。
    変更ファイル:
      - tools/verification/scareware-page/index.html
      - tools/verification/scareware-page/scroll.html
      - tools/verification/scareware-page/safe.html
      - mobile/src/test/kotlin/com/mimiduku/mobile/rules/ScarewarePageTest.kt
      - docs/TESTING.md
      - .claude/skills/mimiduku-device-verification/SKILL.md
      - .claude/skills/mimiduku-change/SKILL.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、
      python -m http.server 8765 --bind 127.0.0.1 --directory tools/verification/scareware-page と curl での取得、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は compileGithubDebugUnitTestKotlin と testGithubDebugUnitTest が UP-TO-DATE ではなく実行され、GitHub 版 235件
      （うち ScarewarePageTest 3件）成功・失敗0。PC 上の配信で3ページとも HTTP 200。端末の Chrome での表示と警告の確認、adb reverse
      経由の配信は未確認（実機を操作しないため。BL-009 の T9〜T11 の実施時に確かめる）。Play 版の単体テストと Android Lint・
      assembleDebug は段階「最後」のためループの最後の全ゲートで確かめる（本イテレーションでは未実行）。
    関連ID:
      - BL-073
- date: 2026-11-13 18:16
  summary: ログ閲覧画面の行への変換 LogRows の単体テスト LogRowsTest を追加する（BL-072）
  details:
    変更内容: >-
      LogRowsTest を追加した（本体は変えていない）。新しい順の並べ替え、アプリ名が取れないときのパッケージ名への代替（遷移元・遷移先の
      両方）、遷移先の無いスケアウェアの行で targetApp が null、DetectionKind 2種と InterventionAction 4種（SKIPPED_EXCLUDED を含む）が
      それぞれの文字列リソースへ対応すること（全値を列挙して期待値の表と突き合わせるため、WARN_ONLY と ASK_USER の入れ替えのような
      取り違えも失敗になる）、UTC と Asia/Tokyo で同じ時刻が別の表示になる書式、空のログで行が無いことを確かめる7件。テストを書く中で
      本体の不具合は見つからなかった。docs/TESTING.md 1章のテスト一覧と DESIGN.md の FR-13 へ LogRowsTest を足した。
    変更ファイル:
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/LogRowsTest.kt
      - docs/TESTING.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は1回目に ktlint・detekt の行長の指摘（LogRowsTest の2行）で失敗し、直して成功。compileGithubDebugUnitTestKotlin と
      testGithubDebugUnitTest は UP-TO-DATE ではなく実行され、GitHub 版 232件（うち LogRowsTest 7件）成功・失敗0。Play 版の単体テストと
      Android Lint・assembleDebug は段階「最後」のためループの最後の全ゲートで確かめる（本イテレーションでは未実行）。
    関連ID:
      - BL-072
- date: 2026-11-13 18:09
  summary: 開示文の版数 ConsentStore.CURRENT_VERSION を 1 から 2 へ上げ、旧版の同意で保護が動かないことを単体テストで確かめる（BL-074）
  details:
    変更内容: >-
      ユーザーの決定「BL-074 は版数を上げる」に従い、ConsentStore.CURRENT_VERSION を 2 にした（BL-055 で開示文へ足したサイト名の
      ハッシュ・PIN で暗号化した控えの保存とアドレスバーの読み取りに対する同意を取り直す。DESIGN.md の FR-20）。開示文の文面と
      版数以外の挙動は変えていない。ConsentStoreTest を追加し、版 1 で同意済みの保存値では hasConsented が false（サービスの
      protectionStopped が true になり、イベントを処理せずバッジも出さない）、accept の後は true で保存値が 2 になること、
      版数が 2 であること、読めない保存値は同意とみなさないことを確かめた。既存の SettingsControllerTest
      「a consent for an older disclosure version asks again」は CURRENT_VERSION - 1（今回から実際の旧版 1）で開示の再表示を
      押さえている。再同意が要ることをリリースノートの原本（CHANGELOG.md の Unreleased）と DESIGN.md の FR-20 へ書き、
      実機の再同意の流れを docs/TESTING.md 2.3 の T16 として BL-009 の確認範囲へ加えた。BL-057 の版数の記述を現状へ合わせた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/ConsentStore.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/mode/ConsentStoreTest.kt
      - docs/TESTING.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew --continue ktlintCheck detekt testGithubDebugUnitTest --console=plain（軽い検証）、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py、python scripts/scan-secrets.py
    検証結果: >-
      成功 - 軽い検証は終了コード0で、compileGithubDebugKotlin・compileGithubDebugUnitTestKotlin・testGithubDebugUnitTest は
      UP-TO-DATE でなく実行された。GitHub 版の単体テスト 225 件成功（ConsentStoreTest 4 件を含む）。Android Lint・assembleDebug・
      Play 版の単体テストは段階「最後」のためこのイテレーションでは未実行。実機での再同意は未確認（BL-009 の T16）。
    関連ID:
      - BL-074
- date: 2026-11-13 17:52
  summary: サイトの判定待ちの間に次の遷移が来ると前の検知がログに残らない不具合と、待機中の一時停止を見ていない点を直す（BL-071）
  details:
    変更内容: >-
      サイトの除外の判定待ち（遷移の1秒後にアドレスバーを読む）を、サービスから Android 非依存の `DomainWait` へ移して直した。
      (1) 待っている間に次のドメイン待ちが来ると、DelayedAction が前の予約を取り消すため、前の検知は確認バナーもログも残らなかった。
      前の待ちをドメインを読まずにその場で確定させ、`InterventionPlanner.afterDomain`（ドメイン null）で確認を出してログに残すようにした。
      (2) 待っている間に一時停止・同意の取り消しがあっても1秒後に確認を出してログに残していた。待ち終えた時点で保護が止まっていれば
      何もしない（スケアウェアの settle と同じ扱い。FR-12）。待ち時間の定数はサービスの DOMAIN_SETTLE_MILLIS から
      `DomainWait.SETTLE_MILLIS`（1秒のまま）へ移した。テストのため DelayedAction に `OneShotTimer` の interface を付けた。
      先に DomainWaitTest を修正前の挙動のまま実行し、2件のテスト（2件目の遷移で1件目が残る・待ち中の一時停止）が失敗することを
      確かめてから直した。
      利用者に見える変化は次のとおり（サイトを1件以上登録し、確認を出す介入のときだけ。登録が無い・自動で戻す介入では変わらない）。
      (1) 1秒以内に続いた2件目のブラウザ遷移で、1件目の確認バナーがすぐに出て、約1秒後に2件目の確認バナー（または登録済みならバナーなし）
      に置き換わる。検知ログは1件から2件になる（1件目は登録済みのサイトであっても、ドメインを読まないため「戻る／このまま進む」として
      残る）。1件目のバナーの「今後は確認しない」ではサイト名をボタンを押した時点で読む。
      (2) 待ち中に一時停止・同意の取り消しをすると、確認バナーもログも出なくなる（修正前は1件出ていた）。
      直し方の選択は BL-071 の既定値どおりで、(1) は確認を省かない側（FR-25 の「読めない場合は確認を出す」と同じ側）、(2) は FR-12 に揃えた。
      補足（BL-080 で追記）。直前の BL-070 の記録にある「ドメイン待ちの予約は介入を行う関数の分岐の中に置いた」は本変更で改められ、
      予約と待ちの管理は DomainWait へ移り、サービスの carryOut は InterventionPlan.AwaitDomain を DomainWait.await へ渡すだけになった。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DomainWait.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/OneShotTimer.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DelayedAction.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/DomainWaitTest.kt
      - docs/TESTING.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest --rerun-tasks、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0。単体テストは両版とも221件成功（新規 DomainWaitTest 5件を含む。修正前の挙動では
      5件中2件が失敗することを確認済み）。実機確認は未実施（未確認）で、docs/TESTING.md 2.9 の X14・X15 として BL-056 の実施時に行う。
    関連ID:
      - BL-071
- date: 2026-11-13 17:47
  summary: サービスに残る介入の分岐を Android 非依存の InterventionPlanner へ移し、単体テストを足す（BL-070）
  details:
    変更内容: >-
      MimidukuAccessibilityService の intervene・present・deferForDomain にあった分岐（アプリの除外ならログのみ、サイトの除外の登録が
      あるブラウザへの確認ならドメイン待ち、BACK_AND_WARN なら戻る、ASK_USER なら「今後は確認しない」を付ける、待機後に登録済みの
      ドメインなら SKIPPED_EXCLUDED でログのみ）を、結果を表す値 `InterventionPlan`（LogOnly／AwaitDomain／Present）を返す
      `InterventionPlanner`（service パッケージ）の `plan` と `afterDomain` へ移した。サービスは返った計画をそのまま Android の操作として行う。
      イベントの処理後と照合の settle 後に同じ形で書いていたシールド・案内パネル・バッジの同期を `syncAdScreen` 1つにまとめ、
      一時停止・未同意の判定も1か所（`protectionStopped`）にした。挙動は変えていない（判定の順序・条件・ログの内容・ShieldTimer の
      呼び出し時機は移す前と同じ。判定に使わないイベントと一時停止中は従来どおりバッジだけ合わせる）。detekt の関数数の上限（11 未満）に
      収めるため、ドメイン待ちの予約は介入を行う関数の分岐の中に置いた。InterventionPlannerTest（11件）で各分岐と版（FeatureSet）に
      よる違いを確かめ、既存のテストは変更していない。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/InterventionPlanner.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/InterventionPlannerTest.kt
      - docs/TESTING.md
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/EXECUTE.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug --rerun-tasks、
      ./gradlew testGithubDebugUnitTest testPlayDebugUnitTest、npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0。単体テストは両版とも216件成功（新規 InterventionPlannerTest 11件を含み、
      既存の ProtectionEngineTest は変更なしで成功）。リファクタリングのため実機確認は行っていない（未確認）。
    関連ID:
      - BL-070
- date: 2026-11-09 00:14
  summary: ページの読み込み直後に操作しなくてもスケアウェアの警告が出るようにする（BL-063）
  details:
    変更内容: >-
      照合は前面が変わった直後と、前回から 1 秒以上空いた文字の変化でだけ走るため、読み込みの最後の変化が間引かれて
      その後イベントが来ないと、読み込み後の画面は次の変化まで照合されなかった。エンジンに、前面の画面で照合が間引かれたことを
      示す `hasPendingScan` と、間引かれた照合を 1 回だけ行う `settle` を追加した。サービスは、間引きが残っている間、
      文字の変化のたびに `DelayedAction` を 1 秒で予約し直し（デバウンス）、変化が止まった時点で `settle` を呼ぶ。待っている間に
      一時停止・同意の取り消しがあれば行わない。間引かれていない（直近に照合した）画面では何もしないため、通常の画面で
      照合の回数は増えない。detekt の関数数の上限のため、サービスの処理は `process` の中に置いた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/TESTING.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py、npx markdownlint-cli2、adb での実機確認（Pixel 8 Pro・Android 17）
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（両版の単体テスト全件成功）。実機（GitHub 版 debug）で、テストページを開いて
      操作せず 7 秒待つと、3 回とも警告が 1 件出た（修正前は 0 件）。T5 は +1 件、T4 は +0 件で退行なし。Play 版の実機は未確認。
    関連ID:
      - BL-063
- date: 2026-11-08 23:50
  summary: 前面に出てもウィンドウ状態の変化が届かないアプリの遷移を見逃さない補正を入れる（BL-062）
  details:
    変更内容: >-
      起動済みのブラウザなどが、ウィンドウ状態の変化イベントなしで前面に出ると、前面判定が古いまま残って外部遷移を
      見逃す。`ProtectionEngine.promoteToForeground` を追加し、追跡中の前面と違うアプリの文字の変化を、そのアプリが
      実際のアクティブなウィンドウ（`rootInActiveWindow` のパッケージ）のときだけ前面の変化として扱うようにした。
      背景のアプリの文字の変化・一時的に重なるもの（システム UI・キーボード・本アプリ）・前面のアプリ自身では、
      アクティブなウィンドウを調べない。検知されなかった状態そのものは実機で再現できず、原因の特定は未了のため
      BL-062 は「進行中」で残した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/ExternalTransitionDetector.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py、npx markdownlint-cli2、adb での実機確認（Pixel 8 Pro・Android 17）
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（両版の単体テスト全件成功）。実機（GitHub 版 debug）で、補正後も T5 を
      起動済みの Chrome で 3 回（毎回 +1 件）、T1 を起動済みで 2 回（毎回 +1 件で電卓へ戻る）、T4（ホームからの起動）は +0 件で
      退行が無いことを確認した。検知されなかった状態の再現と、補正による解消の確認は未実施。
    関連ID:
      - BL-062
- date: 2026-11-08 23:42
  summary: アクセシビリティ設定の本アプリの画面で、自分自身の偽警告が出ないようにする（BL-060）
  details:
    変更内容: >-
      システムのアクセシビリティ設定に表示されるサービス説明文（`accessibility_service_description`）に「ウイルスに感染」が
      含まれ、設定アプリを照合する本アプリ自身の偽警告バナーとログが出ていた。設定アプリ全体を照合から外すと設定アプリ内で
      偽の警告を装うページを見逃すため、説明文を「ウイルス感染などを装う偽の警告」へ言い換えて照合語に当たらないようにした。
      説明文が照合語を含まないことを単体テスト（`OwnTextScarewareTest`）で固定した。
    変更ファイル:
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/rules/OwnTextScarewareTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py、npx markdownlint-cli2、adb での実機確認（Pixel 8 Pro・Android 17）
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（両版の単体テスト全件成功）。実機（GitHub 版 debug・おとなモード）で、
      アクセシビリティ設定の本アプリの画面を開いてもバナーが出ず、スケアウェアのログも増えなかった（14 件のまま）。
      Play 版の実機は未確認（説明文は共通のリソース）。
    関連ID:
      - BL-060
- date: 2026-11-08 23:25
  summary: 最小化中のバッジの縦ドラッグと、ストア遷移の二重検知を直す（BL-059・BL-061）
  details:
    変更内容: >-
      BL-059 は、最小化中のバッジを縦に動かすと最小化が解除される不具合の修正。最小化中のバッジの中心は、のぞき分（32dp）が
      大きさ（64dp）の半分と同じため端の座標ちょうどになり、`fromRelease` の厳密な不等号の判定から漏れていた。最小化中に離した
      ときは静止位置から内側へ大きさの4分の1までのずれなら最小化を保ち、それより内側へ引き出したときだけ通常へ戻す
      （`BadgeMetrics`・`wasMinimized`）。あわせて、指が画面の端までしか動かず中心が端に着くだけの操作でも最小化になるよう、
      最小化の判定を端の座標を含める形（`<=` / `>=`）にした。
      BL-061 は、戻る操作の直後にストアが同じ遷移の画面イベントをもう一度出すと、同じ遷移が二重に検知され戻る操作も二重になる
      疑いへの対策。`ExternalTransitionDetector` が、同じ遷移元・遷移先の検知から 800ms 以内の再検知を数えない（繰り返し回数にも
      入れない）。800ms は既存の繰り返しの判定（1秒刻み）より短い値にした。実機では二重検知を再現できず、原因は単体テストでの
      想定（イベントの順序）に基づく。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgePlacement.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgeOverlay.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/ExternalTransitionDetector.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/overlay/BadgePlacementTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/detect/ExternalTransitionDetectorTest.kt
      - docs/records/managed/BACKLOG.md
      - docs/records/managed/DESIGN.md
      - docs/TESTING.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py、npx markdownlint-cli2、adb での実機確認（Pixel 8 Pro・Android 17）
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（両版の単体テスト全件成功）。実機は GitHub 版 debug で、最小化（内側を掴んで
      端まで引く）・縦に動かしても最小化のまま・x=80 から内側へ引くと通常へ戻る・T2 を 3 回実行して毎回ログ 1 件で電卓へ戻る
      ことを確認した。二重検知そのものは修正前後とも実機で再現できていない。Play 版の実機は未確認。
    関連ID:
      - BL-059
      - BL-061
- date: 2026-11-09 00:00
  summary: 確認を省く対象の実機確認の続き（X4・X8・X9）の結果を記録する（BL-056）
  details:
    変更内容: >-
      Pixel 8 Pro（Android 17）・Chrome 156 で X4・X8・X9 を確認した。X4 は、こどもモードでは登録済みのアプリ・サイトでも
      自動で戻る（BACK_AND_WARN）。X8 は、登録済みアプリからインストーラーへの移動でも確認バナーが出る。X9 は、前のタブが登録済みの
      サイトの状態から別サイトを開いても、3 回続けて確認が出た（誤って省かない）。コードの変更は無い。
      実機検証の注意として、アプリを force-stop するとアクセシビリティサービスの接続が切れ、設定値を入れ直さないとイベントが届かない。
    変更ファイル:
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      python scripts/validate-records.py、npx markdownlint-cli2
    検証結果: >-
      成功 - 記録ファイルの検証と Markdown の静的解析が通った。実機の結果は BL-056 に記録した。
    関連ID:
      - BL-056
- date: 2026-11-08 23:30
  summary: 確認を省くサイトの一覧が空になったらダイアログを閉じる（BL-058）
  details:
    変更内容: >-
      一覧から最後のサイトを外して空になったとき、「閉じる」だけが残るダイアログを自動で閉じるようにした。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest
    検証結果: >-
      成功 - すべて終了コード0。実機では未確認（単体テストで確認）。
    関連ID:
      - BL-058
- date: 2026-11-08 23:00
  summary: 確認を省くサイトの一覧を、PIN で復号して表示できるようにする（BL-058）
  details:
    変更内容: >-
      確認を省くサイトを、照合用のソルトつきハッシュに加えて、PIN から導いた鍵（PBKDF2-HMAC-SHA256・AES-256-GCM）で暗号化した
      控えとして保存するようにした（DomainVault・DomainSecrets）。設定画面の「サイトの一覧を見る（PIN）」で PIN を入力すると復号して
      一覧を表示し、一覧から個別に外せる。PIN の変更では新しい PIN で暗号化し直し、PIN の削除ではサイトの登録をすべて消す
      （ユーザー判断。強度は PIN の桁数のまま）。控えの無い旧い登録はハッシュのまま残り、件数と全削除だけできる。サービス側の判定は
      従来どおりハッシュだけを使い、PIN を必要としない。コントローラは照合した PIN を onPinVerified の処理中だけ保持する。
      開示文・ログ画面の注記・PRIVACY.md・PLAY_LISTING.md・DESIGN.md（FR-25）・TESTING.md（X11〜X13）・CHANGELOG を更新した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/DomainVault.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/DomainSecrets.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/ExclusionStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinDialogs.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinPolicy.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/ExclusionStoreTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/ExclusionPolicyTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/PRIVACY.md
      - docs/PLAY_LISTING.md
      - docs/TESTING.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（単体テストは両版で全件成功）。実機での確認は BL-056（X11〜X13）に残した。
    関連ID:
      - BL-058
      - BL-056
- date: 2026-11-08 22:00
  summary: 実機確認で見つけた、最初のサイトを登録できない不具合を直す（BL-056）
  details:
    変更内容: >-
      サイトの除外は、登録が 1 件も無いと遷移時にアドレスバーを読まないため、最初のサイトの候補が常に空で登録できなかった。
      「今後は確認しない」が押された時点で、遷移先のブラウザのアドレスバーを読むようにした（UrlBarReader.readTarget）。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/UrlBarReader.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest
    検証結果: >-
      成功 - すべて終了コード0。Pixel 8 Pro（Android 17）・Chrome 156 の実機で、サイトの登録と省略、アプリの登録と省略を確認した。
    関連ID:
      - BL-056
      - BL-058
- date: 2026-11-07 12:00
  summary: 「戻る／このまま進む」の確認を省く対象（アプリ・サイト）を追加する（BL-055）
  details:
    変更内容: >-
      確認バナーに「今後は確認しない」を追加し、遷移元のアプリ（パッケージ名）とブラウザで開いたサイト（ドメインのみ）を
      確認の対象から外せるようにした。登録は本体の設定画面のダイアログで選び、保護者 PIN を必須とする（未設定なら登録せず PIN の
      設定を求める）。判定は ExclusionPolicy にあり、確認を出す介入（ASK_USER）だけを省き、自動で戻す介入・警告のみ・偽の警告・
      インストーラーへの遷移は省かない。アプリの登録は ProtectionEngine で即時に判定し、サイトの登録があるときは遷移の1秒後に
      前面ブラウザのアドレスバーを UrlBarReader で読んで DomainNormalizer（ホスト名のみ、www 除去、サブドメインは別扱い）で
      照合する。読めない場合は確認を出す。ドメインは端末ごとのソルトつき SHA-256 でだけ保存し、アプリはパッケージ名で保存する
      （各50件まで）。省いた検知はログへ SKIPPED_EXCLUDED で残す。設定画面に「確認を省く対象」（アプリの個別削除・サイトの全削除は
      PIN 不要）を追加した。flagReportViewIds を追加し、開示文・ログ画面の注記・PRIVACY.md・PLAY_LISTING.md・DESIGN.md（FR-25）・
      TESTING.md（2.9）・CHANGELOG を更新した。SettingsStores の引数を detekt の上限に収めるため ProtectionOverrides にまとめた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/DomainNormalizer.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/UrlBarIds.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/ExclusionStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/exclusion/ExclusionPolicy.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/UrlBarReader.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DelayedAction.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/DevicePackages.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/NodeTextCollector.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BannerSpec.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/log/DetectionLog.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/LogRows.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinPolicy.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/PinDialogs.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/layout/overlay_banner.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/xml/accessibility_service_config.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/DomainNormalizerTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/UrlBarIdsTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/ExclusionStoreTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/exclusion/ExclusionPolicyTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/PRIVACY.md
      - docs/PLAY_LISTING.md
      - docs/TESTING.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: >-
      成功 - Gradle の品質ゲートはすべて終了コード0（単体テストは両版で全件成功）。実機での確認（アドレスバーの読み取り、
      バックグラウンドからの設定画面の起動、各ブラウザでの動作）は BL-056 に人手検証として起票した。
    関連ID:
      - BL-055
      - BL-056
      - BL-057
- date: 2026-10-23 10:00
  summary: バッジを大きくし、タップで出る「×」で閉じられるようにする（BL-054）
  details:
    変更内容: >-
      バッジを 48dp から 64dp に、最小化時ののぞき分を 28dp から 32dp に大きくしてドラッグしやすくした。最小化していない
      バッジをタップすると、画面の中央に近い側の隣に 48dp の「×」ウィンドウを出し、押すとバッジを隠す設定へ切り替える
      （OverlayController の onBadgeClose から MimidukuAccessibilityService が badgeSettings.setVisible(false) を呼ぶ）。
      「×」は4秒、ドラッグ開始、再タップ、バッジの削除で消える。DESIGN.md の FR-18・FR-21、UI デザイン設計書、TESTING.md、
      CHANGELOG を更新した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgeOverlay.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/res/values/strings.xml
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - docs/design/UI_DESIGN.html
      - docs/TESTING.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - ゲートはすべて終了コード0。実機（Pixel 8 Pro・Android 17・ダーク）で、バッジ 192px（64dp）、タップで「×」表示（右端なら左、左端なら右）、約5秒での自動消去、再タップで消去、「×」で badge_visible=false、画面外へのドラッグで最小化（見える幅 96px=32dp）、タップで復帰、左右の端への吸着を確認。TalkBack・横向き・再起動後は未確認（BL-054）
    関連ID:
      - BL-054
- date: 2026-10-22 02:00
  summary: GitHub 版のアプリアイコンを「タコを食べるみみづく」にし、各ドキュメントを最新化する（BL-053）
  details:
    変更内容: >-
      GitHub 版だけのアプリアイコンの前景（mobile/src/github/res/drawable/ic_launcher_foreground.xml）を追加した。
      ソースセットの上書きで main の通常のみみづくを差し替え、Play 版は変えない。くちばしからタコの足が垂れた絵で、
      ベクターを SVG に変換して描画を確かめた。ホームの上部バーはアプリアイコン（ic_launcher_foreground を 1.6 倍）を
      置き、版の絵文字バッジは外した（版の違いはアイコンで表す）。UI デザイン設計書の画面イメージ（絵文字アイコン・
      みみづくの表情・版ラベルの撤去・このアプリについて・GitHub 版アイコン）、DESIGN.md の FR-24、README（画面の見かた・
      サポート導線・アイコン）、PRIVACY.md（開発ページを開く操作は通信しない旨）を更新した。バッジ（オーバーレイ）の絵は
      変えていない。
    変更ファイル:
      - mobile/src/github/res/drawable/ic_launcher_foreground.xml
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/HomeScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/res/values/strings.xml
      - docs/design/UI_DESIGN.html
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - README.md
      - docs/PRIVACY.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py、adb での実機インストールと目視、ベクターの SVG 変換による描画確認
    検証結果: 成功 - ゲートはすべて終了コード0。実機はホーム上部のアイコンを目視。ランチャー上のアイコンは未確認
    関連ID:
      - BL-053
- date: 2026-10-22 01:00
  summary: 実機（Pixel 8 Pro）で見つけた表示の不整合・不具合を修正し、画面をポップにする（BL-046〜BL-051）
  details:
    変更内容: >-
      実機で次を確認し修正した。(1) 同意文の改行が空白に潰れていた（文字列リソースの改行を \n 表記に変更。BL-046）。
      (2) 画面を回転すると設定・このアプリについて・ログからホームへ戻っていた（開いていた画面とログの表示を
      rememberSaveable で保持し、SettingsController に initialScreen を追加。BL-047）。(3) 文字が小さい指摘に対し、
      テーマの文字サイズを全体で 1.125 倍にした。ⓘ の説明文は bodyMedium へ拡大し、カードのタイトルとアイコンは
      titleMedium・36dp に抑えた（BL-048）。(4) 同意前は開示だけを出し、ⓘ の面をネイビー基調の色にし、バッジの説明へ
      「見張りがオフの間は出ません」を追記した（BL-049）。(5) カードのタイトルへ絵文字アイコン（丸い面）、状態へ
      みみづくの表情と絵文字を付け、カードの角を丸くした。版の表示は上部バーのアプリアイコン（GitHub 版はタコを食べるみみづく、Play 版は通常のみみづく）だけにして、文言は
      「このアプリについて」へ移し、上部のバーにみみづくの画像を置いた（BL-050）。(6) 「このアプリについて」の一番下へ
      「困ったとき・ご意見」（開発ページ＝GitHub を開く）のカードを追加した。ブラウザへの ACTION_VIEW のみで、アプリ自身は通信しない
      （BL-051）。実機でホーム・設定・このアプリについて・回転を目視し、ⓘ の開閉と回転後の画面保持を確認した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/HomeScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsCard.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/ui/MimidukuTheme.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/design/UI_DESIGN.html
      - docs/TESTING.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py、adb での実機インストールと目視
    検証結果: 成功 - ゲートはすべて終了コード0。実機は GitHub 版 debug のみ（Play 版・ライトテーマ・TalkBack・横向きの全画面は未確認）
    関連ID:
      - BL-046
      - BL-047
      - BL-048
      - BL-049
      - BL-050
      - BL-051
- date: 2026-10-21 13:00
  summary: 画面の説明文を「ⓘ」ヘルプに分け、本文を状態と操作に絞る（BL-044）
  details:
    変更内容: >-
      SettingsCard に help 引数を追加し、タイトル右の「ⓘ」で補足説明を開閉する面（HelpPanel）を実装した（SettingsCard.kt へ分離）。
      ホームの冒頭説明を見張りの状態へ、制限された設定の手順・一時停止の説明・ログの保存内容・PIN の説明・バッジの説明を各カードの
      ⓘ へ移した。PIN の状態は「設定済み／未設定」に短縮し、利用モードの説明は選択中の項目だけに出す。バッジのスイッチは
      短いラベルにした。UI デザイン設計書の画面イメージと「補足説明の出し方」を更新し、DESIGN.md に FR-22 を追加した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsCard.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/HomeScreen.kt
      - mobile/src/main/res/values/strings.xml
      - docs/design/UI_DESIGN.html
      - docs/records/managed/DESIGN.md
      - CHANGELOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0（ⓘ の見た目と操作は実機未確認で、BL-045 として人手検証へ起票）
    関連ID:
      - BL-044
- date: 2026-10-21 12:00
  summary: バッジの移動・最小化・通知操作に合わせてドキュメントを最新化する（BL-042）
  details:
    変更内容: >-
      DESIGN.md の FR-18 を更新し FR-21（移動・最小化・通知操作）を追加した。保存キー、バッジのウィンドウの扱い、
      権限の方針（POST_NOTIFICATIONS を宣言する旨）も改めた。MVP_DESIGN.md の通知権限の記述、README の機能表、
      PRIVACY.md の保存する設定、docs/TESTING.md（BadgePlacementTest と 2.7 の確認項目 B1〜B8、旧 2.7 は 2.8 へ）、
      UI デザイン設計書の本文と寸法表（バッジの移動・最小化・通知）を実装に合わせた。UI デザイン設計書の画面イメージ（設定画面の図）
      は通知スイッチを描き足していない。
    変更ファイル:
      - docs/records/managed/DESIGN.md
      - docs/design/MVP_DESIGN.md
      - docs/design/UI_DESIGN.html
      - README.md
      - docs/PRIVACY.md
      - docs/TESTING.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0（コードの変更はなく、品質ゲートのビルド系は前回の成功から変化なし）
    関連ID:
      - BL-042
- date: 2026-10-21 11:00
  summary: 通知にバッジの「隠す／出す」ボタンを出し、設定画面に通知の表示スイッチを追加する（BL-041）
  details:
    変更内容: >-
      BadgeNotification を追加した（常駐通知のボタンから、アプリを開かずにバッジの表示を切り替える。ボタンの合図は
      サービスが RECEIVER_NOT_EXPORTED で登録する受信側へ、自アプリ内だけに送る）。通知は設定がオンで、同意済みで、
      通知権限がある場合にだけ出し、権限が無ければ何も出さず保護の動作には影響しない。設定画面へ通知のスイッチを追加し、
      オンにしたときに Android 13 以降は POST_NOTIFICATIONS を求める（SettingsPlatform.requestNotificationPermission）。
      通知アイコン ic_stat_owl とバッジの説明文（移動・最小化）を追加・更新した。detekt の関数数の上限に合わせ、
      バッジの操作を BadgeActions へ分けた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/BadgeNotification.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/BadgeSettings.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/drawable/ic_stat_owl.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0。通知の表示・ボタン操作・権限ダイアログは実機で未確認（BL-043）
    関連ID:
      - BL-041
- date: 2026-10-21 10:00
  summary: みみづくバッジをドラッグで動かせるようにし、画面の外へ引き出すと端へ最小化する（BL-040）
  details:
    変更内容: >-
      バッジのウィンドウをタッチ可能にし、BadgeOverlay（ドラッグ・離したときの端への吸着・最小化・最小化中のタップでの復帰）を
      追加した。置き場所は BadgePlacement（左右の端・縦位置の割合・最小化。Android 非依存）で表し BadgeSettings へ保存する。
      最小化中も指でつかめるよう、バッジを 48dp、のぞき分を 28dp にした。縦位置を割合で持つため縦向き・横向きのどちらでも
      同じ位置に出し、画面の向きが変わったときは onConfigurationChanged で置き直す。ドラッグ中はイベントごとの
      表示更新で置き場所を巻き戻さない。単体テスト BadgePlacementTest を追加した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgePlacement.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgeOverlay.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayLayout.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/BadgeSettings.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/overlay/BadgePlacementTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0。実機でのドラッグ・最小化の操作は未確認（BL-043）
    関連ID:
      - BL-040
- date: 2026-10-20 13:00
  summary: テーマのカード・ダイアログ背景色をブランド色へ揃え、UI デザイン設計書を新しい UI へ追従更新する（BL-037）
  details:
    変更内容: >-
      MimidukuTheme に surfaceContainer 系の色を足した（既定のままだとカード背景が Material3 既定の紫がかった灰色になるため）。
      UI デザイン設計書を Skill ui-design-doc-html の手順で更新した（ホーム・設定・このアプリについての画面、歯車と「⋮」、
      ダーク、テーマ色、オーバーレイのボタン、丸いバッジ、所見の対応状況）。みみづくの図は drawable のパスから生成した。
      README・TESTING.md（U1〜U5 を追加）の画面名を新しい構成に合わせた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/ui/MimidukuTheme.kt
      - docs/design/UI_DESIGN.html
      - README.md
      - docs/TESTING.md
      - CHANGELOG.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0。設計書はヘッドレス Edge で撮影して目視確認した（実機は未確認）
    関連ID:
      - BL-037
- date: 2026-10-20 12:00
  summary: アプリ内の UI を整理する。ネイビー基調のダーク対応テーマ、ホーム・設定・このアプリについての画面構成、歯車と「⋮」メニュー、オーバーレイのボタン配色、バッジの造形（BL-032・BL-033・BL-034・BL-036）
  details:
    変更内容: >-
      MimidukuTheme（ネイビー基調・ダーク追従・ダイナミックカラーなし）を追加し、Activity の下地を DayNight にした。
      画面をホーム・設定・このアプリについて・検知ログに分け、TopAppBar の歯車（設定）と「⋮」メニュー
      （検知ログ・アプリ情報・このアプリについて）、戻る矢印を追加した。サービス停止中の「オンにできないとき」は
      状態カードの直後に出す。移動は NavigationActions と SettingsController.screen で行い、PIN の保護は従来どおり。
      オーバーレイの主ボタンを白地に紺の文字で明示した。Play 版のこども向け確認文を短くした。みみづくバッジ3種と
      アプリアイコンを丸みのある造形へ差し替えた（ユーザー指示）。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/ui/MimidukuTheme.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/HomeScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/LogScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/values/themes.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/main/res/drawable/overlay_button_background.xml
      - mobile/src/main/res/drawable/badge_owl_sleepy.xml
      - mobile/src/main/res/drawable/badge_owl_alert.xml
      - mobile/src/main/res/drawable/badge_owl_wide.xml
      - mobile/src/main/res/drawable/ic_launcher_foreground.xml
      - mobile/src/main/res/layout/overlay_banner.xml
      - mobile/src/main/res/layout/overlay_ad_panel.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug assembleDebug testGithubDebugUnitTest testPlayDebugUnitTest
    検証結果: 成功 - いずれも終了コード0（実機での見た目は未確認。BL-039 で人手検証）
    関連ID:
      - BL-032
      - BL-033
      - BL-034
      - BL-036
- date: 2026-10-19 19:10
  summary: アクセシビリティ API の利用についての目立つ開示と同意の画面を追加する（BL-029）
  details:
    変更内容: >-
      ConsentStore（同意した開示文の版数を保存し、版数が変わったら同意を取り直す）を追加した。設定画面の最初に
      開示（読み取るもの・使い道・保存するもの・送信しないこと・いつでもオフにできること）を出し、チェックと
      「同意して進む」で同意を記録する。同意するまではアクセシビリティ設定を開くボタンを出さない。
      サービスは同意がない間イベントを処理せず、バッジも出さない（同意なしにシステムの設定から直接オンにされた場合への備え）。
      操作は ConsentActions として SettingsActions と分けた。両方の版に入れる。README・TESTING.md・DESIGN.md を更新した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/ConsentStore.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - README.md
      - docs/TESTING.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug testGithubDebugUnitTest testPlayDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0、単体テストは両方の版で各144件成功
    関連ID:
      - BL-029
- date: 2026-10-19 18:30
  summary: Play 版と GitHub 版を同じソースからフラグで出し分ける（BL-028）
  details:
    変更内容: >-
      productFlavors（github・play）と BuildConfig.DISTRIBUTION を足し、GitHub 版の applicationId に .full を付けた。
      版ごとの機能を FeatureSet に集め（知らない値は Play 版として扱う）、サービス・判定・設定画面がこれを見るようにした。
      Play 版は、こどもモードでも自動で戻さず「もどる／このまま すすむ」を出し（こども向けの文言を追加）、タップのあった
      外部遷移には介入せず、広告画面の判定を使う機能（案内パネル・シールド・代理タップ・バッジの広告反応）を外す。
      設定画面に版の表示と、版に合わせたこどもモード・バッジの説明を出す。detekt の引数の数の指摘に合わせ、
      ProtectionEngine の間引きの時間を定数にした（値は変えていない）。flavor の追加で lintDebug・testDebugUnitTest が
      曖昧になったため、CLAUDE.md の品質ゲートを両方の版のタスクに直した。README・TESTING.md・DESIGN.md・
      DISTRIBUTION.md を更新した。
    変更ファイル:
      - mobile/build.gradle.kts
      - mobile/src/main/kotlin/com/mimiduku/mobile/feature/FeatureSet.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/UsageMode.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BannerSpec.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/feature/FeatureSetTest.kt
      - CLAUDE.md
      - README.md
      - docs/TESTING.md
      - docs/design/DISTRIBUTION.md
      - docs/records/managed/DESIGN.md
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug testGithubDebugUnitTest testPlayDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - lintDebug・testDebugUnitTest の曖昧さと detekt の引数の数の指摘を直して再実行し、いずれも終了コード0、
      単体テストは両方の版で各142件成功。applicationId は github が com.mimiduku.mobile.full.debug、
      play が com.mimiduku.mobile.debug であることを出力メタデータで確認
    関連ID:
      - BL-028
- date: 2026-10-19 16:50
  summary: みみづくバッジの常駐表示と表情の切り替えを実装する（BL-022）
  details:
    変更内容: >-
      BadgeExpression（平常はうとうと、広告画面では羽角が立つ、外部遷移・偽の警告の検知から10秒は目が大きい。
      表示しない設定か一時停止中は出さない）と BadgeSettings（既定は表示）を追加した。バッジは画面の右端中央に
      44dp で出し、タッチは受け取らず下のアプリへ通す。サービスは一時停止中もイベントごとにバッジを更新する。
      設定画面に表示の切り替えを足した（保護の強さに関わらないため PIN は求めない）。
      キャラクター3種とアプリアイコン（アダプティブアイコン）を仮の自作ベクター画像で用意した。
      detekt の関数の数の指摘に合わせ、ウィンドウの配置を OverlayLayout へ分けた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/BadgeExpression.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayLayout.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/mode/BadgeSettings.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/settings/SettingsScreen.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/MainActivity.kt
      - mobile/src/main/AndroidManifest.xml
      - mobile/src/main/res/drawable/badge_owl_sleepy.xml
      - mobile/src/main/res/drawable/badge_owl_alert.xml
      - mobile/src/main/res/drawable/badge_owl_wide.xml
      - mobile/src/main/res/drawable/ic_launcher_foreground.xml
      - mobile/src/main/res/mipmap-anydpi/ic_launcher.xml
      - mobile/src/main/res/values/ic_launcher_background.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/overlay/BadgeExpressionTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/settings/SettingsControllerTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - detekt の関数の数の指摘を直して再実行し、いずれも終了コード0、単体テスト132件成功。
      Android Lint のアイコン未設定の警告は解消した
    関連ID:
      - BL-022
- date: 2026-10-19 16:00
  summary: 広告画面の閉じるボタンの代理タップを実装する（BL-021）
  details:
    変更内容: >-
      CloseButtonFinder（Android 非依存）を追加した。文字か説明が閉じる・スキップの文言に完全一致し、クリックでき、
      表示されていて、画面内に収まり、大きさが0でなく、画面の25%以下のノードだけを候補にし、閉じるをスキップより
      優先する。CloseButtonClicker が前面のノードツリー（最大400ノード）から候補を選んで ACTION_CLICK を送る。
      案内パネルの「もどる」を押したときだけ代理タップを試し、候補が無いか失敗したら GLOBAL_ACTION_BACK で代える。
      自動ではクリックしない（PLAN.md 7章の非目標「広告の自動クリック」に当たらないようにするため）。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/CloseButtonFinder.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/CloseButtonClicker.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/detect/CloseButtonFinderTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - テストの補助関数の引数の数・行長の指摘を直して再実行し、いずれも終了コード0、単体テスト125件成功
    関連ID:
      - BL-021
- date: 2026-10-19 15:20
  summary: 全画面広告のタップシールドを実装する（BL-020）
  details:
    変更内容: >-
      こどもモードで全画面の広告（広告 SDK の Activity と判定できたもの）のとき、半透明でタップを吸収する
      全画面のシールドを張り、案内パネル（大きな「もどる」）をその上に置くようにした。ShieldTimer が張るかを決め、
      同じ広告画面で張り続けるのは60秒までとし、時間切れの後はその広告画面が終わるまで張り直さない
      （判定の誤りで子供が操作できなくなるのを防ぐ）。OverlayController にも60秒の自動解除を入れた。
      AdScreenState に fullScreen を足した。完了条件のうち「画面外・極小の領域の除外」は、ノードの位置で覆う
      アプリ内バナー広告向けのもので、今回は全画面広告に限ったため対象外とし、BL-025（人手検証）へ分けた。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/ShieldTimer.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/CountdownParser.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/overlay/ShieldTimerTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 既存テストの期待値（fullScreen の追加）を直して再実行し、いずれも終了コード0、単体テスト117件成功
    関連ID:
      - BL-020
      - BL-025
- date: 2026-10-19 14:40
  summary: 広告画面のカウントダウン表示と大きな「戻る」ボタンを実装する（BL-019）
  details:
    変更内容: >-
      EngineEvent.WindowChanged に前面のクラス名を持たせ、ProtectionEngine が画面の文字を1回だけ読んで
      広告画面の判定とスケアウェアの照合に使うようにした。広告画面のあいだは adScreen（残り秒数と利用モード）を保ち、
      外部遷移・別アプリへの切替・広告の文言が消えたときに解除する（キーボード等の一時的な前面では保つ）。
      CountdownParser は「あと5秒」「12秒後」「Skip in 5」「8 seconds remaining」などから1〜120秒を読み取り、
      単位のない数字は読まない。サービスは処理のたびに案内パネル（画面下部、こども向け・おとな向けの文言、
      大きな「もどる」ボタンで GLOBAL_ACTION_BACK）を出し入れする。広告画面はログに残さない。
      「Skip in 5」形式を拾えるよう、スキップの文言ルールを3件追加した。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/ProtectionEngine.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/service/MimidukuAccessibilityService.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/CountdownParser.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/AdPanelSpec.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/overlay/OverlayController.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/rules/DetectionRules.kt
      - mobile/src/main/assets/rules.json
      - mobile/src/main/res/layout/overlay_ad_panel.xml
      - mobile/src/main/res/values/strings.xml
      - mobile/src/test/kotlin/com/mimiduku/mobile/detect/CountdownParserTest.kt
      - mobile/src/test/kotlin/com/mimiduku/mobile/service/ProtectionEngineTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: >-
      成功 - 行長の指摘と、「Skip in 5」を広告と判定できない不足（単体テストで検出）を直して再実行し、
      いずれも終了コード0、単体テスト113件成功
    関連ID:
      - BL-019
- date: 2026-10-19 13:40
  summary: 広告画面の判定を実装する（BL-018）
  details:
    変更内容: >-
      検知ルールに広告 SDK の Activity のクラス名の接頭辞（Google Mobile Ads・AppLovin・Unity・ironSource・
      Meta Audience Network・Vungle・Mintegral・Pangle・Chartboost・FIVE・maio・nend）と、広告画面の文言
      （ID・文字・完全一致/部分一致・重み）を追加し、rules.json と既定値を揃えた。旧形式の JSON でも読めるよう
      新しい項目には既定値（空）を持たせた。AdScreenDetector はクラス名の接頭辞、または文言の重みの合計3以上で
      広告画面とする。「広告」の表示だけ（重み1）では広告画面とせず、短いラベルはノードの文字全体の一致に限る。
    変更ファイル:
      - mobile/src/main/kotlin/com/mimiduku/mobile/rules/DetectionRules.kt
      - mobile/src/main/kotlin/com/mimiduku/mobile/detect/AdScreenDetector.kt
      - mobile/src/main/assets/rules.json
      - mobile/src/test/kotlin/com/mimiduku/mobile/detect/AdScreenDetectorTest.kt
      - docs/records/managed/BACKLOG.md
    検証コマンド: >-
      ./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug、
      npx markdownlint-cli2 "**/*.md" --config .markdownlint-cli2.yaml、python scripts/validate-records.py
    検証結果: 成功 - いずれも終了コード0、単体テスト103件成功
    関連ID:
      - BL-018
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
