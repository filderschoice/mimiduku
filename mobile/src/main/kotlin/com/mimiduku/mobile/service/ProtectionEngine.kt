package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityEvent
import com.mimiduku.mobile.detect.AdScreenDetector
import com.mimiduku.mobile.detect.AdScreenState
import com.mimiduku.mobile.detect.CountdownParser
import com.mimiduku.mobile.detect.ExternalTransition
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.exclusion.ExclusionPolicy
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.DetectionLogEntry
import com.mimiduku.mobile.log.InterventionAction
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
        /** 前面に出たウィンドウのクラス名（Activity 名など）。広告画面の判定に使う。 */
        val className: String? = null,
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
            className: CharSequence? = null,
        ): EngineEvent? {
            val name = packageName?.toString()?.takeIf { it.isNotEmpty() } ?: return null
            return when (eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> WindowChanged(name, atMillis, className?.toString())
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
    /** 除外の登録に当たるため確認を出さない。サービスはログだけ残す。 */
    val skipConfirmation: Boolean = false,
) {
    /** 除外に当たるため確認を省いたものに変える（ログの介入は [InterventionAction.SKIPPED_EXCLUDED]）。 */
    fun asSkipped() =
        copy(skipConfirmation = true, logEntry = logEntry.copy(action = InterventionAction.SKIPPED_EXCLUDED))
}

/**
 * イベントから検知結果を出す（Android の画面操作は行わない）。
 *
 * スケアウェアの照合は、前面が変わったときと、文字の変化イベントが [SCAN_INTERVAL_MILLIS] 以上空いたときに行う。
 * 同じアプリ・同じ文言の警告は [SCAREWARE_REPEAT_MILLIS] の間は繰り返さない（警告が出続けて操作できなくなるのを防ぐ）。
 * 同じ時機に広告画面の判定も行い、結果を [adScreen] に置く（ログには残さない。広告そのものは悪質とは限らないため）。
 */
class ProtectionEngine(
    private val detector: ExternalTransitionDetector,
    private val matcher: ScarewareMatcher,
    private val modeProvider: () -> UsageMode,
    private val adDetector: AdScreenDetector? = null,
    private val features: FeatureSet = FeatureSet.GITHUB,
    private val exclusions: ExclusionStore? = null,
) {
    /** 前面が広告画面なら、その状態。広告画面でなければ null。サービスは処理のたびにこれを見て表示を合わせる。 */
    var adScreen: AdScreenState? = null
        private set
    private var foregroundClassName: String? = null

    /** 文字の変化で照合が間引かれ、まだ照合していない画面があるか。変化が止まったあとの [settle] の要否に使う。 */
    var hasPendingScan: Boolean = false
        private set
    private var lastScanPackage: String? = null
    private var lastScanAt = Long.MIN_VALUE
    private val lastScarewareWarnedAt = mutableMapOf<Pair<String, String>, Long>()

    /**
     * 前面に出たのに「ウィンドウ状態の変化」が届かないアプリ（起動済みのブラウザなど）を見逃さないための補正。
     * 追跡中の前面と違うパッケージの文字の変化は、そのアプリが実際のアクティブなウィンドウ（[activePackage]）のときだけ、
     * 前面の変化として扱う。背景のアプリが出す文字の変化は、アクティブなウィンドウと一致しないので前面にしない。
     * 一時的に重なるもの（システム UI・キーボード・本アプリ）は対象にせず、[activePackage] も呼ばない。
     */
    fun promoteToForeground(
        event: EngineEvent,
        activePackage: () -> String?,
    ): EngineEvent {
        val candidate = event is EngineEvent.ContentChanged && isForegroundCandidate(event.packageName)
        return if (candidate && activePackage() == event.packageName) {
            EngineEvent.WindowChanged(event.packageName, event.atMillis)
        } else {
            event
        }
    }

    private fun isForegroundCandidate(packageName: String) =
        packageName != detector.currentForeground && !detector.isTransient(packageName)

    /**
     * 照合が間引かれた文字の変化のあと、変化が止まった時点で 1 回だけ照合する。ページの読み込みが終わった画面は、次の変化が
     * 来るまで照合されないため（BL-063）。間引かれた照合が残っていないときは何もしない。
     */
    fun settle(
        atMillis: Long,
        textsProvider: () -> List<String>,
    ): List<Detection> {
        val foreground = detector.currentForeground
        return if (hasPendingScan && foreground != null) {
            listOfNotNull(scan(EngineEvent.ContentChanged(foreground, atMillis), textsProvider, force = true))
        } else {
            emptyList()
        }
    }

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
                if (event.packageName == detector.currentForeground) foregroundClassName = event.className
                if (transition != null) {
                    adScreen = null
                    if (InterventionPolicy.shouldIntervene(transition, features)) {
                        listOf(transitionDetection(transition, event.atMillis))
                    } else {
                        emptyList()
                    }
                } else {
                    listOfNotNull(scan(event, textsProvider, force = true))
                }
            }
            is EngineEvent.ContentChanged -> listOfNotNull(scan(event, textsProvider, force = false))
        }

    private fun transitionDetection(
        transition: ExternalTransition,
        atMillis: Long,
    ): Detection {
        val mode = modeProvider()
        val intervention = InterventionPolicy.forExternalTransition(mode, transition, features)
        // 確認を出す介入だけが除外の対象（自動で戻す・警告のみの介入は省かない）。
        val skip =
            intervention.action == InterventionAction.ASK_USER &&
                exclusions?.let { ExclusionPolicy.skipsByPackage(it, transition) } == true
        val detection =
            Detection(intervention, transitionEntry(transition, atMillis, mode, intervention.action), transition)
        return if (skip) detection.asSkipped() else detection
    }

    private fun transitionEntry(
        transition: ExternalTransition,
        atMillis: Long,
        mode: UsageMode,
        action: InterventionAction,
    ) = DetectionLogEntry(
        atMillis = atMillis,
        kind = DetectionKind.EXTERNAL_TRANSITION,
        sourcePackage = transition.sourcePackage,
        targetPackage = transition.targetPackage,
        action = action,
        clickedRecently = transition.clickedRecently,
        usageMode = mode,
    )

    /** 画面の文字を1回だけ読み、広告画面の判定とスケアウェアの照合に使う。 */
    private fun scan(
        event: EngineEvent,
        textsProvider: () -> List<String>,
        force: Boolean,
    ): Detection? {
        val packageName = event.packageName
        // 一時的に重なるもの（システム UI・キーボード・本アプリ）は照合しない。前面のアプリだけを見る。
        val due =
            packageName == detector.currentForeground &&
                (force || packageName != lastScanPackage || event.atMillis - lastScanAt >= SCAN_INTERVAL_MILLIS)
        if (!due) {
            // 前面の画面が変わったのに間引かれたとき。読み込みの最後の変化が間引かれると、その後イベントが来ない限り照合されない。
            if (packageName == detector.currentForeground) hasPendingScan = true
            return null
        }
        hasPendingScan = false
        lastScanPackage = packageName
        lastScanAt = event.atMillis
        val texts = textsProvider()
        updateAdScreen(texts)
        return matcher
            .findMatch(texts)
            ?.takeIf { shouldWarn(packageName, it.id, event.atMillis) }
            ?.let { phrase -> scarewareDetection(packageName, phrase.id, event.atMillis) }
    }

    private fun updateAdScreen(texts: List<String>) {
        val verdict = adDetector?.evaluate(foregroundClassName, texts)
        adScreen =
            if (verdict?.isAd == true) {
                AdScreenState(
                    countdownSeconds = CountdownParser.parse(texts),
                    mode = modeProvider(),
                    fullScreen = verdict.byActivity,
                )
            } else {
                null
            }
    }

    private fun shouldWarn(
        packageName: String,
        ruleId: String,
        atMillis: Long,
    ): Boolean {
        val key = packageName to ruleId
        val warnedAt = lastScarewareWarnedAt[key]
        val warn = warnedAt == null || atMillis - warnedAt >= SCAREWARE_REPEAT_MILLIS
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
        const val SCAN_INTERVAL_MILLIS = 1_000L
        const val SCAREWARE_REPEAT_MILLIS = 60_000L
    }
}
