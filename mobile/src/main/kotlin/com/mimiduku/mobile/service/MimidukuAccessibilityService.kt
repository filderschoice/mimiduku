package com.mimiduku.mobile.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.mimiduku.mobile.detect.AdScreenDetector
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.detect.PackageClassifier
import com.mimiduku.mobile.log.DetectionLog
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageModeStore
import com.mimiduku.mobile.overlay.AdPanelSpec
import com.mimiduku.mobile.overlay.BannerSpec
import com.mimiduku.mobile.overlay.OverlayController
import com.mimiduku.mobile.rules.RulesLoader
import com.mimiduku.mobile.rules.ScarewareMatcher
import com.mimiduku.mobile.storage.SharedPreferencesStringStore
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * みみづくの本体。イベントを [ProtectionEngine] へ渡し、検知結果に従って戻る・警告・ログ保存を行う。
 *
 * 判定は [ProtectionEngine] 以下（Android 非依存）にあり、このクラスは Android との受け渡しだけを持つ。
 */
class MimidukuAccessibilityService : AccessibilityService() {
    private var engine: ProtectionEngine? = null
    private var overlay: OverlayController? = null
    private lateinit var detectionLog: DetectionLog
    private lateinit var modeStore: UsageModeStore
    private lateinit var pauseStore: PauseStore

    /** ログの書き込みは画面の処理を止めないよう別スレッドで行う。 */
    private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() {
        super.onServiceConnected()
        detectionLog = detectionLog(this)
        modeStore = usageModeStore(this)
        pauseStore = pauseStore(this)
        val rules = RulesLoader.parse(readRulesAsset()).rules
        val classifier =
            PackageClassifier(
                rules = rules,
                ownPackage = packageName,
                deviceBrowsers = resolvePackages(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))),
                launchers = resolvePackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)),
                inputMethods = inputMethodPackages(),
            )
        engine =
            ProtectionEngine(
                detector = ExternalTransitionDetector(classifier),
                matcher = ScarewareMatcher(rules.scarewarePhrases),
                modeProvider = modeStore::load,
                adDetector = AdScreenDetector(rules),
            )
        overlay = OverlayController(this) { performGlobalAction(GLOBAL_ACTION_BACK) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 保護者が PIN で一時停止している間は、検知も介入もしない。
        val engine = engine?.takeUnless { pauseStore.isPaused() } ?: return
        val engineEvent =
            EngineEvent.from(event.eventType, event.packageName, SystemClock.elapsedRealtime(), event.className)
                ?: return
        val detections = engine.handle(engineEvent) { NodeTextCollector.collect(rootInActiveWindow) }
        detections.forEach(::intervene)
        overlay?.showAdPanel(engine.adScreen?.let(AdPanelSpec::from))
    }

    private fun intervene(detection: Detection) {
        if (detection.intervention.action == InterventionAction.BACK_AND_WARN) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
        overlay?.show(BannerSpec.from(detection))
        // ログの日時は経過時間ではなく実時刻で残す（保護者が後から読むため）。
        val entry = detection.logEntry.copy(atMillis = System.currentTimeMillis())
        logExecutor.execute { runCatching { detectionLog.append(entry) } }
    }

    override fun onInterrupt() {
        overlay?.dismissAll()
    }

    override fun onDestroy() {
        overlay?.dismissAll()
        overlay = null
        engine = null
        logExecutor.shutdown()
        super.onDestroy()
    }

    private fun readRulesAsset(): String? =
        runCatching { assets.open(RULES_ASSET).bufferedReader().use { it.readText() } }
            .onFailure { if (it !is IOException) throw it }
            .getOrNull()

    private fun resolvePackages(intent: Intent): Set<String> =
        packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

    private fun inputMethodPackages(): Set<String> =
        getSystemService(InputMethodManager::class.java)
            ?.inputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()

    companion object {
        private const val RULES_ASSET = "rules.json"

        fun detectionLog(context: Context) = DetectionLog(File(context.filesDir, DetectionLog.FILE_NAME))

        fun usageModeStore(context: Context) = UsageModeStore(SharedPreferencesStringStore.of(context))

        fun pauseStore(context: Context) = PauseStore(SharedPreferencesStringStore.of(context))
    }
}
