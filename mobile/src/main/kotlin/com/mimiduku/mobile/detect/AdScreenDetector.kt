package com.mimiduku.mobile.detect

import com.mimiduku.mobile.rules.AdTextMarker
import com.mimiduku.mobile.rules.DetectionRules
import com.mimiduku.mobile.rules.ScarewareMatcher

/** 広告画面の判定結果。 */
data class AdScreenVerdict(
    val isAd: Boolean,
    /** 広告 SDK の Activity 名で判定したか。 */
    val byActivity: Boolean,
    /** 一致した文言の ID（重複なし）。 */
    val markerIds: Set<String>,
)

/**
 * 前面の画面が広告かを判定する。
 *
 * 前面の Activity のクラス名が広告 SDK の接頭辞で始まれば広告画面とする。そうでなければ、画面内の文言の重みの合計が
 * [DetectionRules.AD_SCORE_THRESHOLD] 以上で広告画面とする。SDK の更新や難読化でクラス名が変わっても、
 * 「スキップ」「報酬を獲得」などの文言で拾えるようにするため（MVP_DESIGN.md 6章）。
 */
class AdScreenDetector(
    rules: DetectionRules,
) {
    private val prefixes = rules.adActivityPrefixes.filter { it.isNotBlank() }
    private val markers =
        rules.adTextMarkers
            .filter { it.weight > 0 }
            .map { it to ScarewareMatcher.normalize(it.text) }
            .filter { it.second.isNotEmpty() }

    fun evaluate(
        className: String?,
        texts: List<String>,
    ): AdScreenVerdict {
        val byActivity = className != null && prefixes.any { className.startsWith(it) }
        val normalizedTexts = texts.map(ScarewareMatcher::normalize).filter { it.isNotEmpty() }
        val matched =
            markers
                .filter { (marker, text) -> normalizedTexts.any { matches(marker.match, it, text) } }
                .map { it.first }
                .distinctBy { it.id }
        val score = matched.sumOf { it.weight }
        return AdScreenVerdict(
            isAd = byActivity || score >= DetectionRules.AD_SCORE_THRESHOLD,
            byActivity = byActivity,
            markerIds = matched.map { it.id }.toSet(),
        )
    }

    private fun matches(
        match: AdTextMarker.Match,
        nodeText: String,
        markerText: String,
    ): Boolean =
        when (match) {
            AdTextMarker.Match.EXACT -> nodeText == markerText
            AdTextMarker.Match.CONTAINS -> nodeText.contains(markerText)
        }
}
