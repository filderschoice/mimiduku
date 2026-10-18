package com.mimiduku.mobile.detect

import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.rules.ScarewareMatcher

/** 前面の広告画面の状態。 */
data class AdScreenState(
    /** 画面の文字から読み取った残り秒数。読み取れなければ null。 */
    val countdownSeconds: Int?,
    val mode: UsageMode,
    /** 広告 SDK の Activity（全画面の広告）と判定できたか。 */
    val fullScreen: Boolean = false,
)

/**
 * 広告画面の文字から、スキップや報酬までの残り秒数を読み取る。
 *
 * 単位のない数字だけ（円の中の「5」など）はゲームの点数などと区別できないため読み取らない。
 */
object CountdownParser {
    const val MAX_SECONDS = 120

    private val patterns =
        listOf(
            Regex("あと(\\d{1,3})秒"),
            Regex("(\\d{1,3})秒後"),
            Regex("(\\d{1,3})秒で"),
            Regex("skipin(\\d{1,3})"),
            Regex("rewardin(\\d{1,3})"),
            Regex("closein(\\d{1,3})"),
            Regex("(\\d{1,3})(?:s|sec|seconds?)remaining"),
        )

    /** 最初に見つかった残り秒数。0 や [MAX_SECONDS] を超える値は誤読とみなして使わない。 */
    fun parse(texts: List<String>): Int? =
        texts
            .asSequence()
            .map(ScarewareMatcher::normalize)
            .flatMap { text -> patterns.asSequence().mapNotNull { it.find(text)?.groupValues?.get(1)?.toIntOrNull() } }
            .firstOrNull { it in 1..MAX_SECONDS }
}
