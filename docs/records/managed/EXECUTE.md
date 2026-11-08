<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用実施記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.execute.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
```yaml
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
