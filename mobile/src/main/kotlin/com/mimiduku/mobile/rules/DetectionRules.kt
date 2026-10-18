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
    /** 広告 SDK の Activity のクラス名の接頭辞。前面のクラス名がこれで始まれば広告画面とみなす。 */
    val adActivityPrefixes: List<String> = emptyList(),
    /** 広告画面に出る文言。重みの合計が [AD_SCORE_THRESHOLD] 以上なら広告画面とみなす。 */
    val adTextMarkers: List<AdTextMarker> = emptyList(),
) {
    companion object {
        /** 対応している形式の版。 */
        const val SUPPORTED_VERSION = 1

        /** 文言による広告画面の判定に要る重みの合計。「広告」の表示だけ（重み1）では広告画面とみなさない。 */
        const val AD_SCORE_THRESHOLD = 3

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
                adActivityPrefixes =
                    listOf(
                        "com.google.android.gms.ads.",
                        "com.applovin.",
                        "com.unity3d.services.ads.",
                        "com.unity3d.ads.",
                        "com.ironsource.",
                        "com.facebook.ads.",
                        "com.vungle.",
                        "com.mbridge.msdk.",
                        "com.bytedance.sdk.openadsdk.",
                        "com.chartboost.sdk.",
                        "com.five_corp.ad.",
                        "jp.maio.sdk.",
                        "net.nend.android.",
                    ),
                adTextMarkers =
                    listOf(
                        AdTextMarker("ad-label", "広告", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("ad-label", "Ad", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("ad-label", "AD", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("ad-label", "スポンサー", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("ad-label", "Sponsored", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("skip", "スキップ", AdTextMarker.Match.EXACT, 2),
                        AdTextMarker("skip", "広告をスキップ", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("skip", "Skip", AdTextMarker.Match.EXACT, 2),
                        AdTextMarker("skip", "Skip Ad", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("skip", "Skip ad", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("skip", "Skip in", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("skip", "でスキップ", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("skip", "後にスキップ", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("reward", "報酬を獲得", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("reward", "Reward in", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("reward", "Reward granted", AdTextMarker.Match.CONTAINS, 2),
                        AdTextMarker("install", "インストール", AdTextMarker.Match.EXACT, 1),
                        AdTextMarker("install", "Install", AdTextMarker.Match.EXACT, 1),
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

/**
 * 広告画面に出る文言1件。短いラベル（「Ad」「スキップ」など）は単語の一部に当たらないよう [Match.EXACT]
 * （ノードの文字全体が一致）で、長い言い回しは [Match.CONTAINS]（部分一致）で照合する。
 * 同じ [id] の文言は何個一致しても1回分の重みしか数えない。
 */
@Serializable
data class AdTextMarker(
    val id: String,
    val text: String,
    val match: Match,
    val weight: Int,
) {
    enum class Match {
        EXACT,
        CONTAINS,
    }
}
