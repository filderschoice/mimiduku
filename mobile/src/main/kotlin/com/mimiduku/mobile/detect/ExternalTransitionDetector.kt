package com.mimiduku.mobile.detect

/** 検知した外部遷移。 */
data class ExternalTransition(
    val sourcePackage: String,
    val targetPackage: String,
    val target: TransitionTarget,
    /** 遷移元のアプリで、直前（[ExternalTransitionDetector.clickWindowMillis] 以内）にクリックがあったか。ログ用。 */
    val clickedRecently: Boolean,
    /** 同じ遷移元から [ExternalTransitionDetector.repeatWindowMillis] 以内に検知した回数（今回を含む）。 */
    val repeatCount: Int,
)

/**
 * 前面パッケージの変化から外部遷移を判定する。
 *
 * 対象は「通常のアプリ」から「ブラウザ・ストア・インストーラー」への遷移。次は対象外とする。
 * - 遷移元がランチャー・設定などの除外対象（利用者が意図して開いたとみなす）
 * - 遷移元と遷移先が同じ種類（ブラウザ同士など）、または遷移元がストア・インストーラー
 * - システム UI・キーボード・本アプリなど一時的に重なるもの（前面の変化として扱わない）
 *
 * PLAN 6章の「直前のクリックの有無」は、クリックの直後もクリックなしも対象になるため判定には使わず、
 * ログに残す情報として [ExternalTransition.clickedRecently] に入れる。
 */
class ExternalTransitionDetector(
    private val classifier: PackageClassifier,
    val clickWindowMillis: Long = DEFAULT_CLICK_WINDOW_MILLIS,
    val repeatWindowMillis: Long = DEFAULT_REPEAT_WINDOW_MILLIS,
) {
    private var foregroundPackage: String? = null
    private var lastClickPackage: String? = null
    private var lastClickAt = Long.MIN_VALUE
    private val recentDetections = ArrayDeque<Pair<String, Long>>()

    /** 現在の前面パッケージ（一時的に重なるものを除く）。 */
    val currentForeground: String? get() = foregroundPackage

    fun onViewClicked(
        packageName: String,
        nowMillis: Long,
    ) {
        if (classifier.isTransient(packageName)) return
        lastClickPackage = packageName
        lastClickAt = nowMillis
    }

    /** 前面が変わったときに呼ぶ。外部遷移と判定したらその内容を返す。 */
    fun onWindowStateChanged(
        packageName: String,
        nowMillis: Long,
    ): ExternalTransition? {
        if (classifier.isTransient(packageName)) return null
        val source = foregroundPackage
        foregroundPackage = packageName
        val target = classifier.targetOf(packageName)
        val detected = source != null && target != null && source != packageName && isEligibleSource(source, target)
        return if (detected) {
            ExternalTransition(
                sourcePackage = source!!,
                targetPackage = packageName,
                target = target!!,
                clickedRecently = source == lastClickPackage && nowMillis - lastClickAt in 0..clickWindowMillis,
                repeatCount = countRepeat(source, nowMillis),
            )
        } else {
            null
        }
    }

    private fun isEligibleSource(
        source: String,
        target: TransitionTarget,
    ): Boolean {
        val sourceKind = classifier.targetOf(source)
        return when {
            classifier.isIgnoredSource(source) -> false
            sourceKind == null -> true
            // ブラウザから開いたストア・インストーラーは、悪質なページによる誘導がありうるため対象にする。
            else -> sourceKind == TransitionTarget.BROWSER && target != TransitionTarget.BROWSER
        }
    }

    private fun countRepeat(
        source: String,
        nowMillis: Long,
    ): Int {
        while (recentDetections.isNotEmpty() && nowMillis - recentDetections.first().second > repeatWindowMillis) {
            recentDetections.removeFirst()
        }
        recentDetections.addLast(source to nowMillis)
        return recentDetections.count { it.first == source }
    }

    companion object {
        const val DEFAULT_CLICK_WINDOW_MILLIS = 3_000L
        const val DEFAULT_REPEAT_WINDOW_MILLIS = 30_000L
    }
}
