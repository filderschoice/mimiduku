import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// release 版の署名情報は local.properties（.gitignore の対象。鍵と値はユーザーが作成・保管する。BL-075）から読む。
// sesami-wear と同じキー名（RELEASE_STORE_FILE・RELEASE_STORE_PASSWORD・RELEASE_KEY_ALIAS・RELEASE_KEY_PASSWORD）。
// 未設定でも assembleDebug などの通常のビルドと品質ゲートには影響しない（release 版は署名なしのまま）。
val releaseKeystoreProperties =
    Properties().apply {
        val propertiesFile = rootProject.file("local.properties")
        if (propertiesFile.exists()) {
            FileInputStream(propertiesFile).use { load(it) }
        }
    }
val hasReleaseSigningConfig = releaseKeystoreProperties.containsKey("RELEASE_STORE_FILE")

android {
    namespace = "com.mimiduku.mobile"
    compileSdk = 36

    defaultConfig {
        // sesami-wear（com.sesamiwear.mobile）と同じ「com.アプリ名.モジュール名」の形に揃える。
        applicationId = "com.mimiduku.mobile"
        minSdk = 29
        targetSdk = 36
        // リリースビルドでは scripts/release-build.bat が scripts/version.properties を基に -PappVersionCode・
        // -PappVersionName を渡して上書きする（sesami-wear と同じ方式。BL-075）。未指定なら従来の既定値のまま。
        versionCode = (findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = findProperty("appVersionName") as String? ?: "0.1.0"
    }

    // GitHub 版（全機能）と Google Play 版（機能限定）を同じソースから作る（docs/design/DISTRIBUTION.md）。
    // 機能の違いは FeatureSet に集め、ここでは BuildConfig.DISTRIBUTION の値だけを渡す。
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            // Play 版（Play App Signing の鍵）と GitHub 版（自前の鍵）は署名が異なり上書き更新できないため、
            // applicationId を分けて同じ端末に両方入れても衝突しないようにする。
            applicationIdSuffix = ".full"
            buildConfigField("String", "DISTRIBUTION", "\"github\"")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"play\"")
        }
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("RELEASE_STORE_FILE"))
                storePassword = releaseKeystoreProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = releaseKeystoreProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = releaseKeystoreProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // 家庭内サイドロードのリリース版と同一端末へ併存インストールできるようにする。
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            // サイドロード配布のみで配布サイズの制約が小さいため、MVP では縮小・難読化を行わない。
            isMinifyEnabled = false
            // GitHub 版の APK は配布用の署名鍵、Play 版の AAB はアップロード鍵として同じ鍵で署名する（Play 版の配信時の
            // 署名は Play App Signing の鍵）。鍵が未設定なら署名しない。
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // 検知ルール（assets/rules.json）と検知ログの読み書きに使う。
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
