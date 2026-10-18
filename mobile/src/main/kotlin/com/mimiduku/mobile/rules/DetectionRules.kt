package com.mimiduku.mobile.rules

import kotlinx.serialization.Serializable

/**
 * 検知ルール。`assets/rules.json` から読み込み、読めない場合は [DEFAULT] を使う。
 *
 * パッケージ名の一覧は端末から動的に得られるもの（ブラウザ・ランチャー・キーボード）と合わせて使う。
 * ここに書くのは、動的に得られない場合の補完と、端末に依らず固定のもの。
 */
@Serializable
data class DetectionRules(
    /** ルールの形式の版。読み込み時の検証に使う。 */
    val version: Int,
    /** 外部遷移先として扱うブラウザ。 */
    val browserPackages: List<String>,
    /** 外部遷移先として扱うアプリストア。 */
    val storePackages: List<String>,
    /** 外部遷移先として扱うパッケージインストーラー。 */
    val installerPackages: List<String>,
    /** 前面に出ても「前面アプリの変化」として扱わないもの（システム UI など、一時的に重なるもの）。 */
    val transientPackages: List<String>,
    /** ここからの遷移は利用者の意図した操作とみなし、外部遷移として扱わないもの。 */
    val ignoredSourcePackages: List<String>,
    /** スケアウェアの文言。 */
    val scarewarePhrases: List<ScarewarePhrase>,
) {
    companion object {
        /** 対応している形式の版。 */
        const val SUPPORTED_VERSION = 1

        /** 既定のルール。`assets/rules.json` と同じ内容を保つ（単体テストで一致を確認する）。 */
        val DEFAULT =
            DetectionRules(
                version = SUPPORTED_VERSION,
                browserPackages =
                    listOf(
                        "com.android.chrome",
                        "com.chrome.beta",
                        "org.mozilla.firefox",
                        "com.microsoft.emmx",
                        "com.brave.browser",
                        "com.opera.browser",
                        "com.sec.android.app.sbrowser",
                        "com.duckduckgo.mobile.android",
                        "jp.co.yahoo.android.ybrowser",
                    ),
                storePackages = listOf("com.android.vending"),
                installerPackages =
                    listOf(
                        "com.google.android.packageinstaller",
                        "com.android.packageinstaller",
                    ),
                transientPackages =
                    listOf(
                        "com.android.systemui",
                        "android",
                    ),
                ignoredSourcePackages =
                    listOf(
                        "com.android.settings",
                        "com.google.android.permissioncontroller",
                    ),
                scarewarePhrases =
                    listOf(
                        ScarewarePhrase("virus-infected-ja", "ウイルスに感染"),
                        ScarewarePhrase("virus-infected-ja", "ウィルスに感染"),
                        ScarewarePhrase("virus-detected-ja", "ウイルスが検出"),
                        ScarewarePhrase("virus-detected-ja", "ウイルスを検出"),
                        ScarewarePhrase("battery-damaged-ja", "バッテリーが損傷"),
                        ScarewarePhrase("battery-damaged-ja", "バッテリーが破損"),
                        ScarewarePhrase("phone-damaged-ja", "スマートフォンが損傷"),
                        ScarewarePhrase("phone-damaged-ja", "携帯電話が損傷"),
                        ScarewarePhrase("device-infected-en", "your device is infected"),
                        ScarewarePhrase("device-infected-en", "your phone is infected"),
                        ScarewarePhrase("device-infected-en", "has been infected with"),
                        ScarewarePhrase("virus-detected-en", "virus detected"),
                        ScarewarePhrase("virus-detected-en", "viruses detected"),
                        ScarewarePhrase("battery-damaged-en", "battery is damaged"),
                    ),
            )
    }
}

/** スケアウェアの文言1件。同じ意味の表記ゆれは同じ [id] を持たせ、ログでは [id] だけを残す。 */
@Serializable
data class ScarewarePhrase(
    val id: String,
    val text: String,
)
