package com.mimiduku.mobile.feature

/**
 * 配布の版ごとに有効にする機能。GitHub 版と Google Play 版は同じソースから作り、違いはここに集める
 * （docs/design/DISTRIBUTION.md 3章・4章の案 C）。
 *
 * Play 版で外すのは、Google Play の「他のアプリの広告表示の妨害」の禁止に当たる可能性が高い機能と、
 * 通常の広告クリックの結果を止めてしまう介入。悪質さのシグナル（偽の警告の文言・タップなしの外部遷移）への
 * 対応はどちらの版にも残す。
 */
data class FeatureSet(
    val distribution: Distribution,
    /** 外部遷移をこどもモードで自動で戻すか。false なら、こどもモードでも「戻る／このまま進む」を出す。 */
    val autoBackOnTransition: Boolean,
    /** 遷移元で直前にタップがあった外部遷移（通常の広告クリックを含む）にも介入するか。 */
    val interveneOnTappedTransition: Boolean,
    /** 広告画面の案内パネル（残り秒数・大きな「もどる」）。 */
    val adScreenPanel: Boolean,
    /** 全画面広告のタップシールド。 */
    val tapShield: Boolean,
    /** 案内パネルの「もどる」での閉じるボタンの代理タップ。 */
    val closeButtonProxyTap: Boolean,
    /** バッジの「広告画面で羽角が立つ」表情。 */
    val badgeReactsToAds: Boolean,
) {
    /** 広告画面の判定を行う必要があるか（判定を使う機能が1つでも有効なとき）。 */
    val needsAdScreenDetection: Boolean
        get() = adScreenPanel || tapShield || closeButtonProxyTap || badgeReactsToAds

    enum class Distribution(
        /** build.gradle.kts の BuildConfig.DISTRIBUTION の値。 */
        val buildValue: String,
    ) {
        GITHUB("github"),
        PLAY("play"),
    }

    companion object {
        val GITHUB =
            FeatureSet(
                distribution = Distribution.GITHUB,
                autoBackOnTransition = true,
                interveneOnTappedTransition = true,
                adScreenPanel = true,
                tapShield = true,
                closeButtonProxyTap = true,
                badgeReactsToAds = true,
            )

        val PLAY =
            FeatureSet(
                distribution = Distribution.PLAY,
                autoBackOnTransition = false,
                interveneOnTappedTransition = false,
                adScreenPanel = false,
                tapShield = false,
                closeButtonProxyTap = false,
                badgeReactsToAds = false,
            )

        /**
         * ビルドの種類の値から機能の組を選ぶ。知らない値は Play 版として扱う（Play のポリシーに反する機能を
         * 誤って有効にしない側に倒す）。
         */
        fun forBuild(value: String): FeatureSet =
            when (value) {
                Distribution.GITHUB.buildValue -> GITHUB
                else -> PLAY
            }
    }
}
