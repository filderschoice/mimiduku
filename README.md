# mimiduku（みみづく）

mimiduku（みみづく）は、Android の画面に止まって見張り、悪質な広告に気づいて知らせるアプリです。
子供の端末を守ることを主な目的にしていますが、大人も広告対策として使えるよう「利用モード」を備えています。

> 名前の由来：「ミミズク」の旧仮名遣い「みみづく」から。耳ざとく、先に気づく——そんな見張り番を目指しています。
> （旧仮名遣いであることの辞書での確認は未確認。BACKLOG の BL-010 で確認予定）

## できること

| 機能 | こどもモード（既定） | おとなモード |
| --- | --- | --- |
| 外部遷移の検知（アプリからブラウザ・Play ストア・インストーラーが開かれた） | 自動で戻り、やさしい言葉で知らせる | 自動では戻さず、「戻る」「このまま進む」を選べる |
| スケアウェア文言の検知（「ウイルスに感染」「your device is infected」など） | 警告を出す | 警告を出す |
| 広告画面の検知（広告 SDK の画面、「スキップ」「報酬を獲得」などの表示） | 残り秒数・「さわらないでね」と大きな「もどる」を出す。全画面広告はタップシールドで覆う | 残り秒数と「戻る」を出す |
| 「もどる」を押したとき | 本物の閉じる・スキップボタンがあれば押し、なければ戻る | 同左 |
| みみづくバッジ（初期位置は画面の右端） | 平常はうとうと、広告で耳が立ち、危ない動きを止めると目が大きくなる。指で動かせ、画面の外へ引き出すと端に最小化する | 同左（設定または通知のボタンで非表示にできる） |
| 検知ログ（日時・アプリ名・検知の種類のみ） | 保存し、ホームの「ログを見る」または「⋮」メニューから一覧で見られる | 同左 |

- 同じアプリから30秒以内に4回以上外部遷移が繰り返された場合、4回目以降は自動で戻さず警告だけにします
  （正規のアプリと「戻る」を取り合い続けないため）。
- ランチャー（ホーム画面）・設定アプリからブラウザを開く操作や、ブラウザ同士の移動は対象外です。
- タップシールドは誤って操作できなくならないよう、同じ広告では最大60秒で外れます。アプリ内のバナー広告は覆いません。
- 閉じるボタンの代理タップは、利用者が「もどる」を押したときだけ行います。自動では押しません。
- 広告そのものの除去、ブラウザ内のページ遷移の検知、広告のクリック・完了の偽装は行いません（[PLAN.md](PLAN.md) 7章）。

### 版による機能の違い

GitHub 版と Google Play 版は同じソースから作り、機能の違いは `FeatureSet`
（[mobile/src/main/kotlin/com/mimiduku/mobile/feature/FeatureSet.kt](mobile/src/main/kotlin/com/mimiduku/mobile/feature/FeatureSet.kt)）
だけで切り替えます。Play 版は Google Play のポリシー（他のアプリの広告表示の妨害の禁止）に合わせ、
悪質さのシグナルへの対応だけを残します（理由は [docs/design/DISTRIBUTION.md](docs/design/DISTRIBUTION.md)）。

| 機能 | GitHub 版 | Play 版 |
| --- | --- | --- |
| スケアウェア文言の警告 | あり | あり |
| タップなしの外部遷移 | こどもモードは自動で戻る | どちらのモードも「戻る／このまま進む」を出す |
| タップした後の外部遷移（通常の広告クリックを含む） | 介入する | 介入しない |
| 広告画面の案内パネル・タップシールド・閉じるボタンの代理タップ | あり | なし |
| バッジの表情 | 広告画面で耳が立つ | 危険な検知の直後に目が大きくなるだけ |
| 保護者 PIN・一時停止・ログ | あり | あり |

### 保護者向けの機能

- **保護者 PIN**（4〜8桁）: 設定すると、おとなモードへの切替・一時停止・ログの消去・PIN の変更と削除に PIN が必要に
  なります。5回続けて間違えると30秒待たされ、以降は間違えるたびに倍（最大30分）になります。PIN はハッシュ化して
  保存し、平文は残しません。忘れた場合はアプリを入れ直してください（設定とログは消えます）。
- **一時停止**（15・30・60分）: 保護者が一緒に使うときなどに見張りを止めます。端末の時刻を巻き戻すと停止は終わります。
- **ログの閲覧**: ホームの「ログを見る」（または右上「⋮」メニューの「検知ログ」）から、新しい順に一覧できます。
- 子供がアクセシビリティ設定から本アプリをオフにすることは、本アプリでは防げません。Family Link などで設定画面の
  操作を制限してください。

設計の詳細と、Android のバージョンごとの差分は [docs/design/MVP_DESIGN.md](docs/design/MVP_DESIGN.md) にあります。

## 前提条件

- Android 10（API 29）以降
- Google Play では配布しません。家庭内でのサイドロード（自分でビルドしてインストール）を前提にしています
- Android 16 以降の「高度な保護（Advanced Protection）」を有効にした端末では、アクセシビリティ API が
  支援技術アプリに限られるため動作しない見込みです（未確認）
- Private DNS（広告ブロック DNS）や Family Link と併用すると取りこぼしが減ります。本アプリは「設定で防げない
  取りこぼしを拾う二段目」という位置づけです

## プライバシー

- 画面の文字は照合のためにメモリ上で使うだけで、保存も送信もしません
- インターネット権限（`INTERNET`）を持たないため、外部へデータを送れない構成です
- ログはアプリ内ストレージに保存し、バックアップ・機種変更時の転送の対象から外しています

## 開発環境のセットアップ

| 必要なもの | 版 |
| --- | --- |
| JDK | 17 |
| Android SDK | Platform 36、Build-Tools 35 以降 |
| Gradle | Wrapper（8.13）を同梱。別途のインストールは不要 |

1. Android SDK の場所を `local.properties` に書きます（このファイルは Git 管理外です）。

   ```properties
   sdk.dir=C\:/Android/sdk
   ```

2. ビルドと品質ゲートを実行します。

   ```sh
   ./gradlew assembleDebug
   ./gradlew ktlintCheck detekt lintGithubDebug lintPlayDebug testGithubDebugUnitTest testPlayDebugUnitTest
   ```

   品質ゲートの定義（合否基準）は [CLAUDE.md](CLAUDE.md)「本リポジトリの品質ゲート定義」が正本です。

## インストール（サイドロード）

1. 端末の「開発者向けオプション」で USB デバッグを有効にし、PC と接続します。
2. GitHub 版（全機能）の debug 版をインストールします（applicationId は `com.mimiduku.mobile.full.debug`）。

   ```sh
   ./gradlew assembleGithubDebug
   adb install -r mobile/build/outputs/apk/github/debug/mobile-github-debug.apk
   ```

   Play 版と同じ機能の範囲を確かめたいときは、`assemblePlayDebug` でビルドし
   `mobile/build/outputs/apk/play/debug/mobile-play-debug.apk`（`com.mimiduku.mobile.debug`）を入れます。
   2つの版は applicationId が異なるため、同じ端末に両方入れられます（アクセシビリティは片方だけオンにしてください）。

3. アプリ「みみづく」を開き、最初に出る「画面の読み取りについて」の説明を確認して同意します。
   続けて「アクセシビリティ設定を開く」から「みみづく（広告対策）」をオンにします
   （同意する前にシステムの設定から直接オンにしても、同意するまでは何もしません）。
4. Android 13 以降でオンにできない場合は、アプリ内の「オンにできないとき」の手順で「制限された設定」を許可します
   （ブラウザやファイルマネージャーから APK を入れた場合に必要。adb でのインストールでも必要かは未確認）。
5. 端末によっては省電力機能でサービスが止められることがあります。「電池の最適化」の対象から外してください。

## 更新とロールバック

- **更新**: 新しい APK を `adb install -r` で上書きします。設定と検知ログは残ります。
  debug 版の署名鍵は PC ごとに異なるため、**同じ PC でビルドした APK で更新**してください
  （別の PC の APK は署名が合わず上書きできません。その場合はアンインストールが必要で、設定とログは消えます）。
- **ロールバック**: 以前のコミットを `git checkout` してビルドし、`adb install -r -d` で入れ直します
  （`-d` は版の巻き戻しを許可する指定で、debug 版で使えます）。
- **アンインストール**: `adb uninstall com.mimiduku.mobile.full.debug`（Play 版は `com.mimiduku.mobile.debug`）、
  または端末の設定から行います。
- release 版（GitHub 版 `com.mimiduku.mobile.full`、Play 版 `com.mimiduku.mobile`）の署名設定は未整備です。

## テスト

- 単体テスト: `./gradlew testGithubDebugUnitTest testPlayDebugUnitTest`
- 実機での確認手順と記録欄: [docs/TESTING.md](docs/TESTING.md)

## 検知ルールの更新

検知するブラウザ・ストアのパッケージ名と、スケアウェアの文言は
[mobile/src/main/assets/rules.json](mobile/src/main/assets/rules.json) にあります。編集してビルドし直すと反映されます。
同じ内容の既定値が `DetectionRules.DEFAULT` にあり、JSON が読めない場合はそちらで動きます（両者の一致は単体テストで確認）。
警告文言は [mobile/src/main/res/values/strings.xml](mobile/src/main/res/values/strings.xml) にあります。

## ディレクトリ構成

```text
mobile/src/main/kotlin/com/mimiduku/mobile/
  MainActivity.kt        画面の切替（ホーム・設定・このアプリについて・ログ。Compose）
  rules/                 検知ルールの読み込み、スケアウェア文言の照合
  detect/                外部遷移の判定
  mode/                  利用モード（こども／おとな）と介入内容の決定
  log/                   検知ログの保存
  service/               AccessibilityService、イベント処理（ProtectionEngine）
  overlay/               警告バナー（TYPE_ACCESSIBILITY_OVERLAY）
docs/design/MVP_DESIGN.md  PLAN.md への回答と MVP の設計
docs/records/managed/      エージェントの記録ファイル（BACKLOG・DESIGN・EXECUTE）
```

## 開発の進め方

ブランチ・コミット・レビューの規約は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。
未対応の事項は [docs/records/managed/BACKLOG.md](docs/records/managed/BACKLOG.md) にあります。
