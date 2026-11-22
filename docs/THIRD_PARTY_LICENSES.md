# 同梱するオープンソースのライセンス

みみづく（本アプリ）の APK・AAB に同梱するライブラリのライセンスの一覧です。本アプリ自身のライセンスは [LICENSE](../LICENSE)
（Apache License 2.0）です。

## 確かめ方と範囲

- 対象は GitHub 版・Play 版の release ビルドに入るライブラリです。一覧は、ビルド済みの AGP の依存レポート
  （`mobile/build/intermediates/metadata_library_dependencies_report/githubRelease/.../dependencies.pb`。Play 版のレポートとバイト列が一致）から
  取りました。直接の依存は `gradle/libs.versions.toml` の AndroidX core-ktx・lifecycle-runtime-compose・activity-compose・Compose BOM 配下の
  ui と ui-tooling-preview・Material3・kotlinx-serialization-json で、残りは推移的な依存です（BOM は POM のみで、コードは同梱しません）。
- ライセンスは、ローカルの Gradle キャッシュ（`~/.gradle/caches/modules-2/files-2.1`）にある POM の `licenses` 要素で確かめました。
  POM に無いときは親 POM を見ました。外部への問い合わせはしていません。
- NOTICE は、キャッシュにある AAR・JAR の最上位の `NOTICE`・`LICENSE` の有無を調べました（AAR の中の `classes.jar` の中までは見ていません）。
- テスト・ビルド専用の依存（junit・ktlint・detekt・AGP など）は APK・AAB に入らないため対象外です。
- 依存の一覧の取得に `./gradlew` の依存一覧のタスクは使っていません（品質ゲートの表に無いコマンドのため）。依存を変えたら、本書も更新してください。

## 結果

- 確かめられた依存はすべて Apache License 2.0（POM の表記は「The Apache Software License, Version 2.0」または
  「The Apache License, Version 2.0」）です。Apache-2.0 以外のライセンスは見つかっていません。
- NOTICE ファイルを含む依存は見つかりませんでした。AAR に含まれていたのは `core` と `core-ktx` の `META-INF/androidx/core/.../LICENSE.txt`
  （Apache-2.0 の本文）だけです。そのため、確かめた範囲では、本アプリの再配布で Apache-2.0 の 4(d)（NOTICE の引き継ぎ）に基づいて追加で載せるものは
  現時点でありません。
- 「未確認」の行は、マルチプラットフォーム版の統括モジュール（実体は同名の `-android`・`-jvm` 版）で、ローカルのキャッシュに POM が
  無かったものです。同じグループの実体の依存はいずれも Apache-2.0 ですが、この行自体の POM は確かめていません。
- アプリ内にライセンスを表示するかは、リポジトリの `LICENSE` と本書だけで足りるとの判断です（アプリ内には出しません）。

## 一覧

「直接」は `gradle/libs.versions.toml` で宣言した依存です。

| 依存（group:artifact） | 版 | 直接 | ライセンス | 根拠 | NOTICE・LICENSE の同梱 |
| --- | --- | --- | --- | --- | --- |
| androidx.activity:activity | 1.9.3 |  | Apache-2.0 | POM | なし |
| androidx.activity:activity-compose | 1.9.3 | ○ | Apache-2.0 | POM | なし |
| androidx.activity:activity-ktx | 1.9.3 |  | Apache-2.0 | POM | なし |
| androidx.annotation:annotation | 1.8.1 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.annotation:annotation-experimental | 1.4.1 |  | Apache-2.0 | POM | なし |
| androidx.annotation:annotation-jvm | 1.8.1 |  | Apache-2.0 | POM | なし |
| androidx.arch.core:core-common | 2.2.0 |  | Apache-2.0 | POM | なし |
| androidx.arch.core:core-runtime | 2.2.0 |  | Apache-2.0 | POM | なし |
| androidx.autofill:autofill | 1.0.0 |  | Apache-2.0 | POM | なし |
| androidx.collection:collection | 1.4.4 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.collection:collection-jvm | 1.4.4 |  | Apache-2.0 | POM | なし |
| androidx.collection:collection-ktx | 1.4.4 |  | Apache-2.0 | POM | なし |
| androidx.compose:compose-bom | 2024.12.01 | ○ | Apache-2.0 | POM | 対象なし（POM のみ） |
| androidx.compose.animation:animation | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.animation:animation-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.animation:animation-core | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.animation:animation-core-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.foundation:foundation | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.foundation:foundation-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.foundation:foundation-layout | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.foundation:foundation-layout-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.material:material-icons-core | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.material:material-icons-core-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.material:material-ripple | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.material:material-ripple-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.material3:material3 | 1.3.1 | ○ | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.material3:material3-android | 1.3.1 |  | Apache-2.0 | POM | なし |
| androidx.compose.runtime:runtime | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.runtime:runtime-android | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.runtime:runtime-saveable | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.runtime:runtime-saveable-android | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui | 1.7.6 | ○ | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-geometry | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-geometry-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-graphics | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-graphics-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-text | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-text-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-tooling-preview | 1.7.6 | ○ | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-tooling-preview-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-unit | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-unit-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.compose.ui:ui-util | 1.7.6 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.compose.ui:ui-util-android | 1.7.6 |  | Apache-2.0 | POM | なし |
| androidx.concurrent:concurrent-futures | 1.1.0 |  | Apache-2.0 | POM | なし |
| androidx.core:core | 1.15.0 |  | Apache-2.0 | POM | META-INF/androidx/core/core/LICENSE.txt |
| androidx.core:core-ktx | 1.15.0 | ○ | Apache-2.0 | POM | META-INF/androidx/core/core-ktx/LICENSE.txt |
| androidx.customview:customview-poolingcontainer | 1.0.0 |  | Apache-2.0 | POM | なし |
| androidx.emoji2:emoji2 | 1.3.0 |  | Apache-2.0 | POM | なし |
| androidx.graphics:graphics-path | 1.0.1 |  | Apache-2.0 | POM | なし |
| androidx.interpolator:interpolator | 1.0.0 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-common | 2.8.7 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.lifecycle:lifecycle-common-java8 | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-common-jvm | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-livedata-core | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-process | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-runtime | 2.8.7 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.lifecycle:lifecycle-runtime-android | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-runtime-compose | 2.8.7 | ○ | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.lifecycle:lifecycle-runtime-compose-android | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-runtime-ktx | 2.8.7 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.lifecycle:lifecycle-runtime-ktx-android | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-viewmodel | 2.8.7 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| androidx.lifecycle:lifecycle-viewmodel-android | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-viewmodel-ktx | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.lifecycle:lifecycle-viewmodel-savedstate | 2.8.7 |  | Apache-2.0 | POM | なし |
| androidx.profileinstaller:profileinstaller | 1.3.1 |  | Apache-2.0 | POM | なし |
| androidx.savedstate:savedstate | 1.2.1 |  | Apache-2.0 | POM | なし |
| androidx.savedstate:savedstate-ktx | 1.2.1 |  | Apache-2.0 | POM | なし |
| androidx.startup:startup-runtime | 1.1.1 |  | Apache-2.0 | POM | なし |
| androidx.tracing:tracing | 1.2.0 |  | Apache-2.0 | POM | なし |
| androidx.versionedparcelable:versionedparcelable | 1.1.1 |  | Apache-2.0 | POM | なし |
| com.google.guava:listenablefuture | 1.0 |  | Apache-2.0 | 親POM guava-parent | なし |
| org.jetbrains:annotations | 23.0.0 |  | Apache-2.0 | POM | なし |
| org.jetbrains.kotlin:kotlin-stdlib | 2.0.21 |  | Apache-2.0 | POM | なし |
| org.jetbrains.kotlin:kotlin-stdlib-common | 2.0.21 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlin:kotlin-stdlib-jdk7 | 1.8.20 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlin:kotlin-stdlib-jdk8 | 1.8.20 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-coroutines-android | 1.7.3 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-coroutines-bom | 1.7.3 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-coroutines-core | 1.7.3 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm | 1.7.3 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-serialization-bom | 1.7.3 |  | Apache-2.0 | POM | 対象なし（POM のみ） |
| org.jetbrains.kotlinx:kotlinx-serialization-core | 1.7.3 |  | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-serialization-core-jvm | 1.7.3 |  | Apache-2.0 | POM | なし |
| org.jetbrains.kotlinx:kotlinx-serialization-json | 1.7.3 | ○ | 未確認 | キャッシュに POM なし | 対象なし（キャッシュに無い） |
| org.jetbrains.kotlinx:kotlinx-serialization-json-jvm | 1.7.3 |  | Apache-2.0 | POM | なし |
