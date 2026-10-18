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
| FR-3 | 外部遷移に、遷移元での直前（3秒以内）のクリックの有無と、同じ遷移元からの30秒以内の繰り返し回数を付ける。クリックの有無は判定に使わずログに残す | `ExternalTransitionDetector` |
| FR-4 | スケアウェア文言を NFKC・小文字化・空白除去のうえ部分一致で照合する。ノードをまたいだ文言も照合する | `ScarewareMatcher` |
| FR-5 | スケアウェアの照合は前面アプリだけを対象に、前面の変化時と、文字の変化イベントが1秒以上空いたときに行う。同じアプリ・同じ文言の警告は60秒繰り返さない | `ProtectionEngine` |
| FR-6 | 利用モード（既定はこども）。こども: 外部遷移を自動で戻して警告（同じ遷移元から30秒以内の4回目以降は警告のみ）。おとな: 自動で戻さず「戻る」「このまま進む」を出す。スケアウェアはどちらも警告のみ | `UsageMode`、`InterventionPolicy`、`UsageModeStore` |
| FR-7 | 警告バナーを画面上部に出す。自分の領域のタッチだけを受け取り、外は下のアプリへ通す。警告は8秒、確認は20秒で自動で消える。文言はこども向け・おとな向けを文字列リソースに分ける | `BannerSpec`、`OverlayController`、`strings.xml` |
| FR-8 | 検知ログ（日時・遷移元/遷移先のパッケージ名・検知の種類・ルール ID・介入の内容・直前のクリックの有無・利用モード）をアプリ内ストレージへ最新500件まで保存する | `DetectionLog` |
| FR-9 | 設定画面: 見張りの状態、アクセシビリティ設定を開くボタン、Android 13 以降でオフのときの「制限された設定」の解除手順とアプリ情報を開くボタン、利用モードの切替、検知ログの件数 | `MainActivity`、`SetupStatus` |

## 設計方針

- アーキテクチャ方針
  - 判定は Android 非依存のクラス（`rules`・`detect`・`mode`・`log`・`ProtectionEngine`・`BannerSpec`・`SetupStatus`）に
    置き、JVM の単体テストで確かめる。`MimidukuAccessibilityService` と `OverlayController` は Android との受け渡しだけを持つ。
  - サービスはイベントを `EngineEvent` に変換して `ProtectionEngine.handle` へ渡し、返った `Detection` ごとに
    「（こどもモードなら）`GLOBAL_ACTION_BACK` → バナー表示 → ログ保存（単一スレッドの Executor）」を行う。
  - ブラウザ・ランチャー・キーボードの一覧は、サービス接続時に端末から取得してルールの一覧に足す
    （`queryIntentActivities` の `VIEW https` と `MAIN/HOME`、`InputMethodManager.inputMethodList`）。
- データ設計方針
  - ログは JSON 配列のファイル（`files/detection-log.json`）。一時ファイルへ書いて `ATOMIC_MOVE` で置き換える。
    読めないファイルは空として扱い、次の追記で置き換える。日時は実時刻、判定の時刻は `elapsedRealtime` を使う。
  - 利用モードは SharedPreferences（`mimiduku` / `usage_mode`）。未知の値は既定（こども）に戻す。
- UI/UX方針
  - オーバーレイは View（`overlay_banner.xml`）、設定画面は Jetpack Compose（Material 3）。
  - こども向けの文言はひらがな中心で怖がらせない。おとな向けは検知理由（遷移先の種類）を具体的に示す。

## 非機能要件

- 性能: ノードの走査は400ノード・深さ40まで。文字の変化イベントの照合は1秒間隔に間引く。`notificationTimeout` は200ms。
- 信頼性: ルールの読み込み失敗時は既定値で動く。ログは件数上限と置き換え書き込みで破損と肥大を防ぐ。
- セキュリティ/プライバシー: 画面の文字はメモリ上の照合にだけ使い、保存・送信しない。`INTERNET` を宣言しない。
  ログはバックアップと端末間転送から除外する（`allowBackup="false"` と `dataExtractionRules`）。

## 実装制約

- 技術制約
  - `accessibility_service_config.xml`: イベントは `typeWindowStateChanged|typeWindowContentChanged|typeViewClicked`、
    `canRetrieveWindowContent="true"`、`flagIncludeNotImportantViews`。`canPerformGestures` と `isAccessibilityTool` は宣言しない。
  - 権限は `BIND_ACCESSIBILITY_SERVICE`（サービスの保護）と `<queries>` のみ。`SYSTEM_ALERT_WINDOW`・`QUERY_ALL_PACKAGES`・
    `POST_NOTIFICATIONS`・フォアグラウンドサービスは使わない。
  - 広告の完了・クリックの偽装、広告の自動クリック、広告の除去は行わない。
- 運用制約
  - 品質ゲートは `CLAUDE.md`「本リポジトリの品質ゲート定義」（ktlint・detekt・Android Lint・assembleDebug・単体テスト・
    Markdown lint・記録ファイル検証）。
  - 実機での確認は `docs/TESTING.md` の手順で人が行う。

## エージェント実装指示

- 初回実装時の出力要件: 上記 FR-1〜FR-9 を満たし、判定ロジックごとに単体テストを置く。README にセットアップ・
  ビルド・サイドロード・更新・ロールバックの手順、`docs/TESTING.md` に実機確認の手順と記録欄を書く。
- 追加実装時の出力要件: 変更が判定に関わる場合は Android 非依存のクラスへ置いて単体テストを足す。
  検知ルールを変えたら `rules.json` と `DetectionRules.DEFAULT` を同時に更新する（一致は `RulesLoaderTest` で検査）。
  文言を変えたら `strings.xml` だけを更新する。
- 要件トレーサビリティ要件: 未着手の範囲（広告画面の判定・タップシールド・代理タップ・保護者 PIN・みみづくバッジ）は
  BACKLOG の BL-014〜BL-022 で扱う。PIN を入れる際は、こども→おとなへのモード切替を PIN で保護する。
<!-- COPILOT_RECORDS:END -->
