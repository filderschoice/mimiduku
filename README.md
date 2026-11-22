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
- **画面の見かた**: 各カードの「ⓘ」を押すと補足説明が開きます。ホーム上部のアプリアイコンで版が分かり（GitHub 版はタコを食べるみみづく、Play 版は通常のみみづく）、
  版の文言・同意文・サポート（開発ページ＝GitHub をブラウザで開くボタン）は右上「⋮」の「このアプリについて」にあります。
  GitHub 版のアプリアイコンは、くちばしからタコの足が垂れたみみづくです（Play 版は通常のみみづく）。
- 子供がアクセシビリティ設定から本アプリをオフにすることは、本アプリでは防げません。Family Link などで設定画面の
  操作を制限してください。

設計の詳細と、Android のバージョンごとの差分は [docs/design/MVP_DESIGN.md](docs/design/MVP_DESIGN.md) にあります。

## 前提条件

- Android 10（API 29）以降
- 現在は GitHub からの配布（自分でビルドしてインストールするサイドロード）が前提です。Google Play 版は申請を予定していますが、
  公開はまだです
- Android 16 以降の「高度な保護（Advanced Protection）」を有効にした端末では、アクセシビリティ API が
  支援技術アプリに限られるため動作しない見込みです（未確認）
- Private DNS（広告ブロック DNS）や Family Link と併用すると取りこぼしが減ります。本アプリは「設定で防げない
  取りこぼしを拾う二段目」という位置づけです

## プライバシー

- 画面の文字は照合のためにメモリ上で使うだけで、保存も送信もしません
- インターネット権限（`INTERNET`）を持たないため、外部へデータを送れない構成です（「このアプリについて」の「開発ページを開く」は、
  端末のブラウザへ GitHub の URL を渡すだけで、アプリ自身は通信しません）
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
- release 版（GitHub 版 `com.mimiduku.mobile.full`、Play 版 `com.mimiduku.mobile`）は、次の「リリースビルド」の手順で
  同じ鍵で署名します。更新は versionCode が前の版以上の APK でしかできず、release 版では巻き戻し（`-d`）も使えないため、
  ロールバックは前の版のソースから versionCode を上げてビルドし直した版を配布して行います。

## リリースビルド

GitHub 版の配布用 APK と、Play 版の申請用 AAB（Android App Bundle）を署名つきで作る手順です。
鍵の作成・保管とリリースビルドの実行はユーザーが行います（エージェントは鍵とパスワードを扱わず、release ビルドを
実行しません。guardrails 12.5）。鍵が無い環境でも、debug ビルドと品質ゲートは今までどおり通ります。

### 1. 署名の鍵を作る（初回だけ）

```bash
keytool -genkeypair -v -keystore mimiduku-release.jks -alias <鍵の別名> \
  -keyalg RSA -keysize 2048 -validity 10000
```

- 鍵ファイルはリポジトリの外（例 ユーザーのホームの下の専用フォルダ）に置き、パスワードマネージャー等の安全な場所に
  バックアップとパスワードを保管します。**鍵を失うと、同じ applicationId のアプリを更新できなくなります**
  （GitHub 版は利用者にアンインストールからやり直してもらうことになる。Play 版はアップロード鍵のリセットを Google へ依頼する）。
- 鍵ファイル（`*.jks`・`*.keystore`・`*.p12`・`*.pfx`・`*.bks`）と `local.properties` は `.gitignore` の対象で、
  `python scripts/scan-secrets.py` もコミットへの追加を検出します。**絶対にコミットしないでください。**
- 同じ鍵を、GitHub 版の APK の署名と、Play 版の AAB のアップロード鍵の両方に使います（Play 版の配信時の署名は
  Play App Signing の鍵で、アップロード鍵とは別）。

### 2. 鍵の場所とパスワードを `local.properties` へ書く

`local.properties`（リポジトリの直下。`.gitignore` の対象）へ次のキーを足します。値はプレースホルダーで、実際の値に置き換えます。

```properties
RELEASE_STORE_FILE=/absolute/path/to/mimiduku-release.jks
RELEASE_STORE_PASSWORD=<鍵ストアのパスワード>
RELEASE_KEY_ALIAS=<鍵の別名>
RELEASE_KEY_PASSWORD=<鍵のパスワード>
```

`mobile/build.gradle.kts` は4つのキーがすべてあるときだけ release の署名を設定します。すべて無い場合、release の成果物は
署名なしのままで、`assembleDebug` などの通常のビルドには影響しません。一部だけある場合は、release のビルドを実行したときに
足りないキーの名前を示して失敗します（debug のビルドと品質ゲートは通る）。`RELEASE_STORE_FILE` を相対パスで書いた場合は、
リポジトリの直下（`local.properties` と同じフォルダ）を基準に解決します。

### 3. 版を上げてビルドする（`scripts/release-build.bat`）

現在の版は `scripts/version.properties`（`VERSION_CODE`・`VERSION_NAME`）に記録し、ビルドが成功したときだけ更新されます。
GitHub 版と Play 版は applicationId が異なるため、版は両方で共通の1系列です。

```bat
rem versionCode を 1 つ上げてビルドする（versionName は変えない）
scripts\release-build.bat

rem versionCode・versionName を指定してビルドする（上げない。現在値より小さい versionCode は中止する）
scripts\release-build.bat -VersionCode 2 -VersionName 0.2.0
```

- versionCode は、配布するたびに前の版より大きい整数にします（既定は1つ上げる）。versionName は利用者に見える版で、
  両方の版で共通です（現在 `0.1.0`）
- `scripts/version.properties` の記録は `VERSION_CODE=1` から始まるため、引数なしで実行すると**最初の配布の versionCode は 2**
  になります。最初の配布を 1 にしたい場合は、初回だけ `scripts\release-build.bat -VersionCode 1` と指定します
- `-VersionCode` に 0 以下・整数でない値を指定した場合と、`scripts/version.properties` に読めない行がある場合は、
  ビルドに進まずに中止します（読めない行は行番号で示す）
- スクリプトは Gradle へ `-PappVersionCode`・`-PappVersionName` を渡して上書きします。指定しない通常のビルドは
  `mobile/build.gradle.kts` の既定値（versionCode 1・versionName `0.1.0`）のままです
- ビルドには数分かかります。失敗した場合は `scripts/version.properties` は更新されないため、そのまま再実行できます

| 成果物 | 出力先 | 配布先 |
| --- | --- | --- |
| GitHub 版の APK | `mobile/build/outputs/apk/github/release/mobile-github-release.apk` | GitHub Releases など |
| Play 版の AAB | `mobile/build/outputs/bundle/playRelease/mobile-play-release.aab` | Google Play Console |

ビルド後は、両方の成果物の証明書の SHA-256 が同じ（作った鍵のもの）であることを確かめます。AAB は
`keytool -printcert -jarfile <AAB>` で読めます。**APK には使えません**（minSdk 29 のため v2 署名だけで、`keytool` が読む JAR 署名
（v1）が無く、何も出力されません）。APK は Android SDK の `build-tools/<版>/apksigner`（Windows は `apksigner.bat`）で
`apksigner verify --print-certs -v <APK>` を実行し、`Verifies` と `Signer #1 certificate SHA-256 digest` を見ます
（`keytool -list -v` が出す鍵ストアの指紋は、コロン区切りの大文字で、`apksigner` は小文字の連続である点だけが違います）。
リリースのときは、利用者向けの変更点を [docs/RELEASE_NOTES.md](docs/RELEASE_NOTES.md) へ追記し、
`scripts/version.properties` の更新をコミットします。

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

## ライセンス

Apache License 2.0 です。全文は [LICENSE](LICENSE) にあります。

Copyright 2026 filderschoice

## 不具合・要望・脆弱性

- 不具合や要望は GitHub の [Issues](https://github.com/filderschoice/mimiduku/issues) へ書いてください（テンプレートがあります）。
  Issues は公開されるため、個人情報を書かないでください。
- 脆弱性は公開の Issue に書かず、[SECURITY.md](SECURITY.md) の手順（Private vulnerability reporting）で知らせてください。

## 開発の進め方

`CONTRIBUTING.md`・`rules/`・`.claude/`・`docs/records/` などは、本リポジトリの開発に使う AI エージェント向けの規約と記録です。
利用だけであれば読む必要はありません。開発に参加する場合は、ブランチ・コミット・レビューの規約を
[CONTRIBUTING.md](CONTRIBUTING.md) で、未対応の事項を [docs/records/managed/BACKLOG.md](docs/records/managed/BACKLOG.md) で確かめてください。
