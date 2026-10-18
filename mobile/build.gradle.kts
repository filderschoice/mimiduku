plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mimiduku.mobile"
    compileSdk = 36

    defaultConfig {
        // sesami-wear（com.sesamiwear.mobile）と同じ「com.アプリ名.モジュール名」の形に揃える。
        applicationId = "com.mimiduku.mobile"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
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
