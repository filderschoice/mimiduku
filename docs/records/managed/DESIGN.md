<!-- markdownlint-disable-file MD041 -->
<!-- AIエージェント専用設計記録ファイル（ユーザ編集禁止） -->
<!-- このファイルはAIエージェントがプロンプト指示の処理実行時のみ自動更新します。 -->
<!-- schema: records.design.v1 -->

<!-- COPILOT_RECORDS:BEGIN -->
# 実装用プロンプト設計書

## 目的

- mimiduku（みみづく）の MVP を、同じ要件で AI エージェントに再実装させるための設計書。
- 要件の出典は `PLAN.md`、検討結果（バージョン差分・可否の再評価・誤ブロック対策など）は
  `docs/design/MVP_DESIGN.md`。本書は実装済みの内容を統合した最新版を保つ。

## 対象システム概要

- 対象: Android 端末で、広告による悪質な挙動（勝手な外部遷移・偽の警告）を外側から検知して介入する常駐アプリ。
  子供の保護が主目的だが、大人も広告対策として使う。
- 実装形態: AccessibilityService ＋ オーバーレイ（`TYPE_ACCESSIBILITY_OVERLAY`）のみ。WebView・JS 注入・
  ブラウザ拡張は使わない。
- 前提環境: Kotlin、AGP 8.13、Kotlin 2.0.21、Compose BOM 2024.12.01、minSdk 29、targetSdk/compileSdk 36、JDK 17。
  単一モジュール `mobile`。applicationId は `com.mimiduku.mobile`（debug は `.debug` を付ける）。
- 配布: Google Play を使わず家庭内でサイドロードする。

## 実装済み機能要件

| ID | 要件 | 主なクラス |
| --- | --- | --- |
| FR-1 | 検知ルール（ブラウザ・ストア・インストーラー・一時的な前面・除外する遷移元・スケアウェア文言）を `assets/rules.json` から読み、読めない・版が違う・必須の項目が欠けるときはコード内の既定値で動く | `DetectionRules`、`RulesLoader` |
| FR-2 | 前面パッケージの変化から、通常のアプリ→ブラウザ/ストア/インストーラーの遷移を外部遷移と判定する。ランチャー・設定からの遷移、ブラウザ同士、ストア・インストーラーからの遷移は対象外。ブラウザ→ストア/インストーラーは対象。システム UI・キーボード・本アプリは前面の変化として扱わない | `PackageClassifier`、`ExternalTransitionDetector` |
| FR-3 | 外部遷移に、遷移元での直前（3秒以内）のクリックの有無と、同じ遷移元からの30秒以内の繰り返し回数を付ける。クリックの有無は判定に使わずログに残す。戻る操作の直後にストアなどが同じ遷移の画面イベントをもう一度出しても、同じ遷移元・遷移先の検知から 800ms 以内なら二重に数えない（戻る操作の二重実行とログの二重記録を防ぐ。BL-061） | `ExternalTransitionDetector` |
| FR-4 | スケアウェア文言を NFKC・小文字化・空白除去のうえ部分一致で照合する。ノードをまたいだ文言も照合する | `ScarewareMatcher` |
| FR-5 | スケアウェアの照合は前面アプリだけを対象に、前面の変化時と、文字の変化イベントが1秒以上空いたときに行う。同じアプリ・同じ文言の警告は60秒繰り返さない | `ProtectionEngine` |
| FR-6 | 利用モード（既定はこども）。こども: 外部遷移を自動で戻して警告（同じ遷移元から30秒以内の4回目以降は警告のみ）。おとな: 自動で戻さず「戻る」「このまま進む」を出す。スケアウェアはどちらも警告のみ | `UsageMode`、`InterventionPolicy`、`UsageModeStore` |
| FR-7 | 警告バナーを画面上部に出す。自分の領域のタッチだけを受け取り、外は下のアプリへ通す。警告は8秒、確認は20秒で自動で消える。文言はこども向け・おとな向けを文字列リソースに分ける | `BannerSpec`、`OverlayController`、`strings.xml` |
| FR-8 | 検知ログ（日時・遷移元/遷移先のパッケージ名・検知の種類・ルール ID・介入の内容・直前のクリックの有無・利用モード）をアプリ内ストレージへ最新500件まで保存する | `DetectionLog` |
| FR-9 | ホーム・設定・このアプリについて・検知ログの各画面（TopAppBar の歯車と「⋮」メニューで移動）。ホーム: 見張りの状態、アクセシビリティ設定を開くボタン、Android 13 以降でオフのときの「制限された設定」の解除手順とアプリ情報を開くボタン、利用モードの切替、検知ログの件数 | `MainActivity`、`SettingsController`、`HomeScreen`、`SettingsScreen`、`SetupStatus` |
| FR-10 | 保護者 PIN（4〜8桁の数字）。塩16バイトの PBKDF2WithHmacSHA256（反復10万回、保存形式 `v1:反復回数:塩:ハッシュ`）で保存し平文を残さない。5回続けて間違えたら30秒、以降は倍で上限30分待たせ、成功で戻す。保存値が壊れたら照合不能（PIN なしとはみなさない） | `PinHasher`、`PinManager` |
| FR-11 | PIN を設定している場合、保護を弱める操作（おとなモードへの切替・PIN の変更と削除・一時停止・ログの消去）で PIN を求める。こどもモードへの切替・再開・バッジの表示切替は求めない | `PinPolicy`、`SettingsController`、`PinDialogs` |
| FR-12 | 一時停止（15・30・60分）。停止中は検知も介入もせずバッジも消す。端末の時刻で期限を持ち再起動後も保つ。時刻が開始より前へ戻されたら停止を終える | `PauseStore` |
| FR-13 | ログ閲覧画面: 新しい順、日時・アプリ名（分からなければパッケージ名）・検知の種類・介入の内容。消去は PIN つき | `LogRows`、`LogScreen` |
| FR-14 | 広告画面の判定: 前面のクラス名が広告 SDK の接頭辞で始まる、または画面の文言の重みの合計が3以上。短いラベルはノードの文字全体の一致に限る。同じ ID の文言は1回だけ数える。広告画面はログに残さない | `AdScreenDetector`、`DetectionRules.adActivityPrefixes`・`adTextMarkers` |
| FR-15 | 広告画面のあいだ、画面下部に案内パネル（残り秒数・「さわらないでね」・大きな「もどる」）を出す。残り秒数は単位つきの表記（「あと5秒」「Skip in 5」等）から1〜120秒を読み、単位のない数字は読まない | `CountdownParser`、`AdPanelSpec`、`OverlayController` |
| FR-16 | こどもモードで全画面広告（クラス名で判定できたもの）のとき、半透明でタップを吸収する全画面のシールドを張り、案内パネルをその上に置く。同じ広告画面で60秒まで、時間切れ後はその広告画面が終わるまで張り直さない | `ShieldTimer`、`OverlayController` |
| FR-17 | 案内パネルの「もどる」を押したときだけ、閉じる・スキップの文言に完全一致し、クリック可能・表示中・画面内・大きさが画面の25%以下のノードへ ACTION_CLICK を送る（閉じるを優先）。無ければ GLOBAL_ACTION_BACK | `CloseButtonFinder`、`CloseButtonClicker` |
| FR-18 | みみづくバッジ: 64dp（ドラッグしやすい大きさ）。既定は右端の縦中央。表情は平常（うとうと）・広告画面（羽角が立つ）・危険な検知から10秒（目が大きい）。表示は設定または通知のボタンで切り替え（既定は表示）。画像とアプリアイコンは差し替え可能な仮の自作ベクター画像 | `BadgeExpression`、`BadgeSettings`、`BadgeOverlay`、`res/drawable/badge_owl_*.xml` |
| FR-19 | GitHub 版と Play 版を同じソースから作る。productFlavors（github・play、BuildConfig.DISTRIBUTION）で版を分け、GitHub 版の applicationId は `.full` を付ける。機能の違いは `FeatureSet` に集める。Play 版は、こどもモードでも自動で戻さず確認を出し、タップのあった外部遷移には介入せず、広告画面の判定を使う機能（案内パネル・シールド・代理タップ・バッジの広告反応）を外す。知らない版の値は Play 版として扱う。設定画面は版の表示と、版に合わせた説明を出す | `FeatureSet`、`InterventionPolicy.shouldIntervene`、`BannerSpec`、`SettingsScreen` |
| FR-20 | アクセシビリティ API の利用についての目立つ開示と同意（両方の版）。設定画面の最初に、読み取るもの・使い道・保存するもの・送信しないことを示し、チェックとボタンの操作で同意を記録する（開示文の版数 `ConsentStore.CURRENT_VERSION`。文面を変えたら上げて同意を取り直す）。同意するまではアクセシビリティ設定を開くボタンを出さず、サービスもイベントを処理しない（バッジも出さない） | `ConsentStore`、`SettingsScreen`（DisclosureCard）、`MimidukuAccessibilityService` |
| FR-21 | バッジの移動・最小化・通知操作。バッジは指でドラッグでき（タッチ判定は `touchSlop` 以上の移動）、離すと近い方の左右の端へ吸着し、縦位置は画面の高さに対する割合で保存する（縦向き・横向きのどちらでも同じ位置に出し、向きが変わったら `onConfigurationChanged` で置き直す）。中心が画面の外へ出る位置で離すとその端へ最小化し、端から 32dp だけを見せる（半透明。最小化中も上下・内側へドラッグでき、タップで元へ戻る。最小化中のバッジの中心は端の座標以上にあるため、最小化中に離したときは静止位置から内側へバッジの大きさの4分の1までのずれなら最小化を保ち、それより内側へ引き出すと通常の表示へ戻す。`BadgePlacement.fromRelease` の `peek` と `wasMinimized`、BL-059）。最小化していないバッジをタップすると隣（画面の中央に近い側）に 48dp の「×」が出て、押すとバッジを隠す設定（通知の「隠す」と同じ）に切り替える（4秒で自動的に消える。ドラッグ開始・再タップでも消える）。通知（既定でオン）に「バッジを隠す／出す」ボタンを付け、アプリを開かずに切り替える。通知は同意済み・設定オン・通知権限ありのときだけ出し、権限が無くても保護は動く。設定画面に通知のスイッチがあり、オンにしたとき Android 13 以降は `POST_NOTIFICATIONS` を求める。バッジの位置と通知の設定は再起動後も保つ | `BadgePlacement`、`BadgeOverlay`、`BadgeNotification`、`BadgeSettings`、`SettingsScreen` |
| FR-22 | 説明文を「必要な情報」と「補足説明」に分ける。カード（`SettingsCard`）の本文には状態と操作だけを置き、補足はタイトル右の「ⓘ」（`help` 引数）で開閉する色付きの面（secondaryContainer、bodyMedium）に出す（開閉は `rememberSaveable`）。利用モードの短い説明は選択中の項目だけに出す。アクセシビリティの開示と同意は要件上、常に全文を表示する（文字列リソース内の改行は `\n` で書く） | `SettingsCard`（HelpToggle・HelpPanel）、`HomeScreen`、`SettingsScreen` |
| FR-23 | 画面の見た目。テーマの文字を Material3 既定の 1.125 倍にし、カードは角を丸くして、タイトルの前に絵文字アイコン（丸い面・読み上げ対象外）を付ける。状態にはみみづくの表情（見張り中は目が大きい、停止中は眠い）と絵文字を添える。同意前のホームは開示だけを出す。回転しても開いていた画面とログの表示を保つ（`SettingsController.initialScreen`） | `MimidukuTheme`、`SettingsCard`、`MainActivity` |
| FR-24 | 版の表示とサポート。版の文言は「このアプリについて」の「配布の版」に出し、ホームの上部バーはアプリアイコン（`ic_launcher_foreground`。GitHub 版はタコを食べるみみづく、Play 版は通常のみみづく。1.6 倍に拡大）だけにして、版の違いをアイコンで表す。「このアプリについて」の一番下に、「困ったとき・ご意見」のカード（GitHub を知らない人にも分かる言葉で、開発ページ（GitHub のリポジトリ）をブラウザで開く）を両方の版で出す（`ACTION_VIEW` のみ。アプリ自身は通信しない） | `HomeScreen`（AboutScreen）、`MainActivity.openSupportPage` |
| FR-25 | 確認を省く対象（`exclusion/`）。大人モード・Play 版のこどもモードの「戻る／このまま進む」（`InterventionAction.ASK_USER`）に限り、登録済みのアプリ（遷移元のパッケージ名）またはサイト（ドメイン）への遷移で確認を出さず、ログに `SKIPPED_EXCLUDED`（登録済みのため確認なし）だけ残す。自動で戻す介入・警告のみ・スケアウェアの警告は省かない。アプリは `ExclusionPolicy.skipsByPackage`（インストーラーへの遷移は対象外）、サイトはブラウザへの遷移だけで、サイト登録があるとき遷移の1秒後（`DOMAIN_SETTLE_MILLIS`）に前面ブラウザのアドレスバー（`UrlBarIds` の既知のビュー ID、`flagReportViewIds`）を `UrlBarReader` で読み、`DomainNormalizer` でホスト名（`www.` と大文字小文字・ポート・パス・認証情報を除く。サブドメインは別扱い）にして `ExclusionStore` と照合する。読めない・ブラウザが前面でない場合は確認を出す。ドメインは、照合用の端末ごとのソルトつき SHA-256 と、一覧表示用の PIN から導いた鍵（PBKDF2、AES-256-GCM。`DomainVault`・`DomainSecrets`）で暗号化した控えにしてだけ保存し、一覧は設定画面の「サイトの一覧を見る」で PIN を入力したときだけ復号して表示する（個別に外せる。PIN の削除でサイトの登録はすべて消え、PIN の変更で暗号化し直す。控えの無い旧い登録は件数と全削除のみ。`GuardedAction.VIEW_EXCLUDED_DOMAINS`）、アプリはパッケージ名で保存する。それぞれ50件まで。追加は確認バナーの「今後は確認しない」（`OverlayController.show` の `onExclude`）から本体の設定画面へ候補を渡し（`MainActivity.exclusionIntent`、`launchMode=singleTop`）、登録するものを選ぶダイアログ（`AddExclusionDialog`）の後に保護者 PIN を必須とする（PIN 未設定なら登録せず PIN の設定を求める。`GuardedAction.ADD_EXCLUSION`）。外す操作は PIN 不要で、設定画面の「確認を省く対象」から行う。 | `ExclusionStore`、`ExclusionPolicy`、`DomainNormalizer`、`UrlBarIds`、`UrlBarReader`、`ProtectionEngine`、`AddExclusionDialog` |

## 設計方針

- アーキテクチャ方針
  - 判定は Android 非依存のクラス（`rules`・`detect`・`mode`・`log`・`pin`・`ProtectionEngine`・`BannerSpec`・`AdPanelSpec`・
    `ShieldTimer`・`BadgeExpression`・`SetupStatus`・`PinPolicy`・`SettingsController`・`LogRows`）に置き、JVM の単体テストで確かめる。
    `MimidukuAccessibilityService`・`OverlayController`・`CloseButtonClicker`・`MainActivity` は Android との受け渡しだけを持つ。
  - サービスはイベントを `EngineEvent` に変換して `ProtectionEngine.handle` へ渡し、返った `Detection` ごとに
    「（こどもモードなら）`GLOBAL_ACTION_BACK` → バナー表示 → ログ保存（単一スレッドの Executor）」を行う。
  - ブラウザ・ランチャー・キーボードの一覧は、サービス接続時に端末から取得してルールの一覧に足す
    （`queryIntentActivities` の `VIEW https` と `MAIN/HOME`、`InputMethodManager.inputMethodList`）。
  - `ProtectionEngine` は照合のたびに画面の文字を1回だけ読み、広告画面の判定とスケアウェアの照合に使う。サービスは
    処理のたびに `adScreen` を見て案内パネル・シールド・バッジを合わせる。一時停止中はイベントを処理せずバッジだけ消す。
  - 設定画面の状態は `SettingsController`（Compose の状態を持つが Android 非依存）、端末側の操作は `SettingsPlatform`
    （`MainActivity` が実装）に分ける。操作は `SettingsActions`・`LogActions` の interface。
- データ設計方針
  - ログは JSON 配列のファイル（`files/detection-log.json`）。一時ファイルへ書いて `ATOMIC_MOVE` で置き換える。
    読めないファイルは空として扱い、次の追記で置き換える。日時は実時刻、判定の時刻は `elapsedRealtime` を使う。
  - 設定は SharedPreferences（名前 `mimiduku`）。`usage_mode`（未知の値は既定のこどもへ）、`pin_hash`・`pin_failures`・
    `pin_locked_until`、`pause_started_at`・`pause_until`、`badge_visible`（非表示のときだけ `false`）、`accessibility_consent_version`。
    バッジは `badge_placement`（`側,縦位置の割合,最小化`。壊れた値と既定値は既定の置き場所＝保存なし）と `badge_notification`（オフのときだけ `false`）も持つ。
    読み書きは `StringStore` 経由にし、単体テストではメモリ上の実装を使う。
- UI/UX方針
  - オーバーレイは View（警告バナー `overlay_banner.xml`、案内パネル `overlay_ad_panel.xml`、シールド、バッジ）、
    アプリ内の画面は Jetpack Compose（Material 3）。配置は `OverlayLayout` にまとめる。バッジのウィンドウだけは
    ドラッグを受けるためタッチを受け取り（大きさはバッジ1つ分）、画面外へ置けるよう `FLAG_LAYOUT_NO_LIMITS` を付ける。
  - アプリ内の画面構成: ホーム（見張りの状態または開示・同意、「オンにできないとき」ガイド、一時停止、ログの件数）、
    設定（利用モード・保護者 PIN・みみづくバッジの表示と通知のスイッチ）、このアプリについて（開示文とログの扱い）、検知ログ。ナビゲーション
    ライブラリは使わず `SettingsController` の `screen`（`Screen`）と `logRows` で切り替える。TopAppBar の歯車で設定、
    「⋮」メニューで検知ログ・アプリ情報（OS の画面）・このアプリについてへ進み、設定・このアプリについて・ログでは
    戻る矢印とシステムの戻る操作でホームへ戻る。画面間の移動は `NavigationActions` に分ける。
  - テーマ: `MimidukuTheme`。アイコン・オーバーレイと同じネイビー（#1E3A5F）を基調にした独自のカラースキームで、
    端末のダーク設定に追従する。ダイナミックカラーは使わない。Activity の下地は `Theme.Mimiduku`（DayNight）。
  - オーバーレイの主ボタン（バナー・案内パネル）は、端末のテーマに依らず白地に紺の文字（`overlay_button_background`）。
  - みみづくバッジ・アプリアイコンは丸みを持たせた造形（丸い体、丸い羽角、ほおの赤み、目のハイライト）の仮の自作画像。
  - こども向けの文言はひらがな中心で怖がらせない。おとな向けは検知理由（遷移先の種類）を具体的に示す。

## 非機能要件

- 性能: ノードの走査は400ノード・深さ40まで。文字の変化イベントの照合は1秒間隔に間引く。`notificationTimeout` は200ms。
- 信頼性: ルールの読み込み失敗時は既定値で動く。ログは件数上限と置き換え書き込みで破損と肥大を防ぐ。
- セキュリティ/プライバシー: 画面の文字はメモリ上の照合にだけ使い、保存・送信しない。`INTERNET` を宣言しない。URL は保存しないが、確認を省くサイトだけは、
  ブラウザのアドレスバーから読んだホスト名を、ソルトつきハッシュ（照合用）と PIN で暗号化した控え（一覧表示用）にして保存する（FR-25）。
  ログはバックアップと端末間転送から除外する（`allowBackup="false"` と `dataExtractionRules`）。

## 実装制約

- 技術制約
  - `accessibility_service_config.xml`: イベントは `typeWindowStateChanged|typeWindowContentChanged|typeViewClicked`、
    `canRetrieveWindowContent="true"`、`flagIncludeNotImportantViews`。`canPerformGestures` と `isAccessibilityTool` は宣言しない。
  - 権限は `BIND_ACCESSIBILITY_SERVICE`（サービスの保護）、`POST_NOTIFICATIONS`（バッジの操作の通知。拒否されても保護は動く）と
    `<queries>`。`SYSTEM_ALERT_WINDOW`・`QUERY_ALL_PACKAGES`・フォアグラウンドサービスは使わない。
  - 広告の完了・クリックの偽装、広告の自動クリック、広告の除去は行わない。
- 運用制約
  - 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」（ktlint・detekt・両方の版の Android Lint・assembleDebug・両方の版の単体テスト・
    Markdown lint・記録ファイル検証）。
  - 実機での確認は `docs/TESTING.md` の手順で人が行う。

## エージェント実装指示

- 初回実装時の出力要件: 上記 FR-1〜FR-25 を満たし、判定ロジックごとに単体テストを置く。README にセットアップ・
  ビルド・サイドロード・更新・ロールバックの手順、`docs/TESTING.md` に実機確認の手順と記録欄を書く。
- 追加実装時の出力要件: 変更が判定に関わる場合は Android 非依存のクラスへ置いて単体テストを足す。
  版によって有無を変える機能は、`BuildConfig` を直接見ずに `FeatureSet` へ項目を足して切り替える。
  検知ルールを変えたら `rules.json` と `DetectionRules.DEFAULT` を同時に更新する（一致は `RulesLoaderTest` で検査）。
  文言を変えたら `strings.xml` だけを更新する。
- 要件トレーサビリティ要件: 未対応の事項は BACKLOG で扱う。アプリ内バナー広告の部分シールドは BL-025（人手検証）、
  Google Play での配布は事前相談なしで先に申請する方針（2026-11-08 決定。申請は BL-031。構成案と機能ごとのポリシー上の評価は `docs/design/DISTRIBUTION.md`）。
  目的は悪質な広告への対応で、通常の広告には介入しない。ただし全画面広告は、正常に見えても悪質な場合がありうるため、
  FR-15〜FR-17（案内パネル・こどもモードのシールド・代理タップ）は全画面広告でも作動させる（2026-10-19 のユーザー決定）。
  Play 版を作る場合は、広告の妨害を禁じるポリシーに合わせてこれらを外す（DISTRIBUTION.md 4章の案 C）。
<!-- COPILOT_RECORDS:END -->
