package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityEvent
import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.mode.Intervention
import com.mimiduku.mobile.mode.InterventionPolicy
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.rules.ScarewareMatcher

/** サービスが受け取ったイベントのうち、判定に使うもの。 */
sealed interface EngineEvent {
    val packageName: String
    val atMillis: Long

    data class WindowChanged(
        override val packageName: String,
        override val atMillis: Long,
    ) : EngineEvent

    data class ContentChanged(
        override val packageName: String,
        override val atMillis: Long,
    ) : EngineEvent

    data class Clicked(
        override val packageName: String,
        override val atMillis: Long,
    ) : EngineEvent

    companion object {
        /** AccessibilityEvent の種類から変換する。判定に使わない種類やパッケージ名の無いものは null。 */
        fun from(
            eventType: Int,
            packageName: CharSequence?,
            atMillis: Long,
        ): EngineEvent? {
            val name = packageName?.toString()?.takeIf { it.isNotEmpty() } ?: return null
            return when (eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> WindowChanged(name, atMillis)
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> ContentChanged(name, atMillis)
                AccessibilityEvent.TYPE_VIEW_CLICKED -> Clicked(name, atMillis)
                else -> null
            }
        }
    }
}

/** 検知1件。サービスは [intervention] に従って介入し、[logEntry] を保存する。 */
data class Detection(
    val intervention: Intervention,
    val logEntry: DetectionLogEntry,
    /** 外部遷移のときの遷移内容。スケアウェアのときは null。 */
    val transition: ExternalTransition? = null,
)

/**
 * イベントから検知結果を出す（Android の画面操作は行わない）。
 *
 * スケアウェアの照合は、前面が変わったときと、文字の変化イベントが [scanIntervalMillis] 以上空いたときに行う。
 * 同じアプリ・同じ文言の警告は [scarewareRepeatMillis] の間は繰り返さない（警告が出続けて操作できなくなるのを防ぐ）。
 */
class ProtectionEngine(
    private val detector: ExternalTransitionDetector,
    private val matcher: ScarewareMatcher,
    private val modeProvider: () -> UsageMode,
    private val scanIntervalMillis: Long = DEFAULT_SCAN_INTERVAL_MILLIS,
    private val scarewareRepeatMillis: Long = DEFAULT_SCAREWARE_REPEAT_MILLIS,
) {
    private var lastScanPackage: String? = null
    private var lastScanAt = Long.MIN_VALUE
    private val lastScarewareWarnedAt = mutableMapOf<Pair<String, String>, Long>()

    /** [textsProvider] は前面の画面の文字を返す。照合が要るときだけ呼ぶ。 */
    fun handle(
        event: EngineEvent,
        textsProvider: () -> List<String>,
    ): List<Detection> =
        when (event) {
            is EngineEvent.Clicked -> {
                detector.onViewClicked(event.packageName, event.atMillis)
                emptyList()
            }
            is EngineEvent.WindowChanged -> {
                val transition = detector.onWindowStateChanged(event.packageName, event.atMillis)
                if (transition != null) {
                    listOf(transitionDetection(transition, event.atMillis))
                } else {
                    listOfNotNull(scanScareware(event, textsProvider, force = true))
                }
            }
            is EngineEvent.ContentChanged -> listOfNotNull(scanScareware(event, textsProvider, force = false))
        }

    private fun transitionDetection(
        transition: ExternalTransition,
        atMillis: Long,
    ): Detection {
        val mode = modeProvider()
        val intervention = InterventionPolicy.forExternalTransition(mode, transition)
        val entry =
            DetectionLogEntry(
                atMillis = atMillis,
                kind = DetectionKind.EXTERNAL_TRANSITION,
                sourcePackage = transition.sourcePackage,
                targetPackage = transition.targetPackage,
                action = intervention.action,
                clickedRecently = transition.clickedRecently,
                usageMode = mode,
            )
        return Detection(intervention, entry, transition)
    }

    private fun scanScareware(
        event: EngineEvent,
        textsProvider: () -> List<String>,
        force: Boolean,
    ): Detection? {
        val packageName = event.packageName
        // 一時的に重なるもの（システム UI・キーボード・本アプリ）は照合しない。前面のアプリだけを見る。
        val due =
            packageName == detector.currentForeground &&
                (force || packageName != lastScanPackage || event.atMillis - lastScanAt >= scanIntervalMillis)
        if (!due) return null
        lastScanPackage = packageName
        lastScanAt = event.atMillis
        return matcher
            .findMatch(textsProvider())
            ?.takeIf { shouldWarn(packageName, it.id, event.atMillis) }
            ?.let { phrase -> scarewareDetection(packageName, phrase.id, event.atMillis) }
    }

    private fun shouldWarn(
        packageName: String,
        ruleId: String,
        atMillis: Long,
    ): Boolean {
        val key = packageName to ruleId
        val warnedAt = lastScarewareWarnedAt[key]
        val warn = warnedAt == null || atMillis - warnedAt >= scarewareRepeatMillis
        if (warn) lastScarewareWarnedAt[key] = atMillis
        return warn
    }

    private fun scarewareDetection(
        packageName: String,
        ruleId: String,
        atMillis: Long,
    ): Detection {
        val mode = modeProvider()
        val intervention = InterventionPolicy.forScareware(mode)
        val entry =
            DetectionLogEntry(
                atMillis = atMillis,
                kind = DetectionKind.SCAREWARE,
                sourcePackage = packageName,
                ruleId = ruleId,
                action = intervention.action,
                usageMode = mode,
            )
        return Detection(intervention, entry)
    }

    companion object {
        const val DEFAULT_SCAN_INTERVAL_MILLIS = 1_000L
        const val DEFAULT_SCAREWARE_REPEAT_MILLIS = 60_000L
    }
}
