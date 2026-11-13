package com.mimiduku.mobile.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.mimiduku.mobile.BuildConfig
import com.mimiduku.mobile.MainActivity
import com.mimiduku.mobile.detect.AdScreenDetector
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.detect.PackageClassifier
import com.mimiduku.mobile.exclusion.ExclusionCandidate
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.log.DetectionLog
import com.mimiduku.mobile.mode.BadgeSettings
import com.mimiduku.mobile.mode.ConsentStore
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageModeStore
import com.mimiduku.mobile.overlay.AdPanelSpec
import com.mimiduku.mobile.overlay.BadgeExpression
import com.mimiduku.mobile.overlay.BannerSpec
import com.mimiduku.mobile.overlay.OverlayController
import com.mimiduku.mobile.overlay.ShieldTimer
import com.mimiduku.mobile.rules.RulesLoader
import com.mimiduku.mobile.rules.ScarewareMatcher
import com.mimiduku.mobile.storage.SharedPreferencesStringStore
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * みみづくの本体。イベントを [ProtectionEngine] へ渡し、検知結果に従って戻る・警告・ログ保存を行う。
 *
 * 判定は [ProtectionEngine] 以下と、検知ごとの介入の内容を決める [InterventionPlanner]（どちらも Android 非依存）にあり、
 * このクラスは Android との受け渡しだけを持つ。
 */
class MimidukuAccessibilityService : AccessibilityService() {
    private var engine: ProtectionEngine? = null
    private var overlay: OverlayController? = null
    private val shieldTimer = ShieldTimer()
    private lateinit var badgeSettings: BadgeSettings
    private lateinit var badgeNotification: BadgeNotification

    /** 通知の「バッジを隠す／出す」ボタンの受け口（アプリの中からの合図だけを受ける）。 */
    private val toggleReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                badgeSettings.setVisible(!badgeSettings.isVisible())
                refreshBadge()
            }
        }
    private lateinit var consentStore: ConsentStore
    private var lastDangerAt: Long? = null
    private lateinit var detectionLog: DetectionLog
    private lateinit var modeStore: UsageModeStore
    private lateinit var pauseStore: PauseStore
    private lateinit var exclusionStore: ExclusionStore
    private val domainCheck = DelayedAction()
    private val planner by lazy { InterventionPlanner(exclusionStore) }
    private val domainWait by lazy {
        DomainWait(
            planner = planner,
            timer = domainCheck,
            readDomain = { UrlBarReader.read(rootInActiveWindow, it) },
            protectionStopped = { protectionStopped },
            carryOut = ::carryOut,
            record = ::record,
        )
    }
    private val scanSettle = DelayedAction()

    /** ログの書き込みは画面の処理を止めないよう別スレッドで行う。 */
    private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onServiceConnected() {
        super.onServiceConnected()
        detectionLog = detectionLog(this)
        modeStore = usageModeStore(this)
        pauseStore = pauseStore(this)
        exclusionStore = exclusionStore(this)
        badgeSettings = badgeSettings(this)
        consentStore = consentStore(this)
        badgeNotification = BadgeNotification(this)
        ContextCompat.registerReceiver(
            this,
            toggleReceiver,
            IntentFilter(BadgeNotification.ACTION_TOGGLE_BADGE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val rules = RulesLoader.parse(readRulesAsset()).rules
        val classifier =
            PackageClassifier(
                rules = rules,
                ownPackage = packageName,
                deviceBrowsers =
                    DevicePackages.resolve(
                        this,
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
                    ),
                launchers =
                    DevicePackages.resolve(
                        this,
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                    ),
                inputMethods = DevicePackages.inputMethods(this),
            )
        engine =
            ProtectionEngine(
                detector = ExternalTransitionDetector(classifier),
                matcher = ScarewareMatcher(rules.scarewarePhrases),
                modeProvider = modeStore::load,
                adDetector = AdScreenDetector(rules).takeIf { FEATURES.needsAdScreenDetection },
                features = FEATURES,
                exclusions = exclusionStore,
            )
        overlay =
            OverlayController(
                context = this,
                onBadgePlacementChanged = badgeSettings::setPlacement,
                onGoBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
                onAdClose = ::closeAd,
                onBadgeClose = {
                    badgeSettings.setVisible(false)
                    refreshBadge()
                },
            )
        refreshBadge()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val engine = engine ?: return
        val paused = protectionStopped
        // 処理したイベントでは、広告画面の表示と一緒にバッジも合わせ終えている。
        val synced = !paused && process(engine, event)
        if (!synced) updateBadge(paused, onAdScreen = !paused && FEATURES.badgeReactsToAds && engine.adScreen != null)
    }

    /**
     * 保護者が PIN で一時停止している間と、アクセシビリティ API の利用に同意していない間は、
     * 検知も介入もしない（バッジも消す）。同意なしでシステムの設定から直接オンにされた場合に備える。
     */
    private val protectionStopped: Boolean
        get() = pauseStore.isPaused() || !consentStore.hasConsented()

    /** 判定に使うイベントなら処理して true を返す。判定に使わないイベントなら何もせず false。 */
    private fun process(
        engine: ProtectionEngine,
        event: AccessibilityEvent,
    ): Boolean {
        val engineEvent =
            EngineEvent.from(event.eventType, event.packageName, SystemClock.elapsedRealtime(), event.className)
                ?.let { engine.promoteToForeground(it) { rootInActiveWindow?.packageName?.toString() } }
                ?: return false
        val detections = engine.handle(engineEvent) { NodeTextCollector.collect(rootInActiveWindow) }
        detections.forEach { carryOut(planner.plan(it)) }
        syncAdScreen(engine, engineEvent.atMillis)
        // 照合が間引かれた画面は、文字の変化が止まってから 1 回だけ照合する（読み込みが終わったページを見逃さない）。
        // 待っている間に一時停止・同意の取り消しがあれば行わない。
        if (engine.hasPendingScan) {
            scanSettle.schedule(ProtectionEngine.SCAN_INTERVAL_MILLIS) {
                if (!protectionStopped) {
                    val now = SystemClock.elapsedRealtime()
                    val settled = engine.settle(now) { NodeTextCollector.collect(rootInActiveWindow) }
                    settled.forEach { carryOut(planner.plan(it)) }
                    syncAdScreen(engine, now)
                }
            }
        }
        return true
    }

    /** 広告画面の判定結果に、タップシールド・案内パネル・バッジを合わせる（保護が止まっていないときに呼ぶ）。 */
    private fun syncAdScreen(
        engine: ProtectionEngine,
        nowMillis: Long,
    ) {
        val adScreen = engine.adScreen
        overlay?.setShield(FEATURES.tapShield && shieldTimer.shouldShield(adScreen, nowMillis))
        overlay?.showAdPanel(adScreen?.takeIf { FEATURES.adScreenPanel }?.let(AdPanelSpec::from))
        updateBadge(paused = false, onAdScreen = FEATURES.badgeReactsToAds && adScreen != null)
    }

    /** [InterventionPlanner] が決めた介入を、Android の操作（戻る・バナー・ログ保存・待機）として行う。 */
    private fun carryOut(plan: InterventionPlan) {
        when (plan) {
            is InterventionPlan.LogOnly -> record(plan.detection)
            // アドレスバーが落ち着くのを待ってから読み、登録済みのサイトなら確認を省く（待ちの扱いは DomainWait）。
            is InterventionPlan.AwaitDomain -> domainWait.await(plan)
            is InterventionPlan.Present -> present(plan)
        }
    }

    private fun present(plan: InterventionPlan.Present) {
        lastDangerAt = SystemClock.elapsedRealtime()
        if (plan.goBack) performGlobalAction(GLOBAL_ACTION_BACK)
        // 「今後は確認しない」は、登録の確認（PIN）を本体の設定画面で行うため、そこへ候補を渡して開く。
        // サイト名は、サイトの登録がまだ無いと遷移の時点では読んでいないので、ボタンが押された時点で読む。
        val onExclude =
            plan.exclusionTarget?.let {
                {
                    val site = plan.domain ?: UrlBarReader.readTarget(rootInActiveWindow, it)
                    val intent = MainActivity.exclusionIntent(this, ExclusionCandidate(it.sourcePackage, site))
                    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        overlay?.show(BannerSpec.from(plan.detection), onExclude)
        record(plan.detection)
    }

    /** ログの日時は経過時間ではなく実時刻で残す（保護者が後から読むため）。 */
    private fun record(detection: Detection) {
        val entry = detection.logEntry.copy(atMillis = System.currentTimeMillis())
        logExecutor.execute { runCatching { detectionLog.append(entry) } }
    }

    /** 通知のボタンなど、イベントとは別の契機でバッジを合わせ直す。 */
    private fun refreshBadge() {
        val paused = protectionStopped
        updateBadge(paused, onAdScreen = !paused && FEATURES.badgeReactsToAds && engine?.adScreen != null)
    }

    private fun updateBadge(
        paused: Boolean,
        onAdScreen: Boolean,
    ) {
        val expression =
            BadgeExpression.of(
                visible = badgeSettings.isVisible(),
                paused = paused,
                onAdScreen = onAdScreen,
                lastDangerAtMillis = lastDangerAt,
                nowMillis = SystemClock.elapsedRealtime(),
            )
        overlay?.setBadge(expression, badgeSettings.placement())
        // 同意前は通知も出さない（同意なしでサービスだけ有効にされた場合に備える）。
        badgeNotification.sync(
            enabled = badgeSettings.isNotificationEnabled() && consentStore.hasConsented(),
            badgeVisible = badgeSettings.isVisible(),
        )
    }

    /** 利用者が案内パネルの「もどる」を押したとき。本物の閉じるボタンを押せなければ「戻る」で代える。 */
    private fun closeAd() {
        val (width, height) = screenSize()
        val clicked =
            FEATURES.closeButtonProxyTap && CloseButtonClicker.clickClose(rootInActiveWindow, width, height)
        if (!clicked) performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun screenSize(): Pair<Int, Int> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
            bounds.width() to bounds.height()
        } else {
            resources.displayMetrics.let { it.widthPixels to it.heightPixels }
        }

    /** 縦向き・横向きの切り替えで画面の大きさが変わるので、バッジを端へ合わせ直す。 */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        overlay?.relayoutBadge()
    }

    override fun onInterrupt() {
        overlay?.dismissAll()
    }

    override fun onDestroy() {
        // サイトの判定待ちの検知は、バナーを出さずにログだけ残す（BL-081）。書き込みは下の shutdown より前に依頼する
        // （shutdown の後の execute は拒否される）。接続前（engine が無い）なら待ちも無い。
        if (engine != null) domainWait.finish() else domainCheck.cancel()
        scanSettle.cancel()
        unregisterReceiver(toggleReceiver)
        badgeNotification.cancel()
        overlay?.dismissAll()
        overlay = null
        engine = null
        logExecutor.shutdown()
        // 依頼済みの書き込みは shutdown 後も実行されるが、サービスの終了後はプロセスがいつ止められてもよい状態になるため、
        // 書き終えるまで少しだけ待つ（通常は待ち行列が空で、すぐに戻る）。
        runCatching { logExecutor.awaitTermination(LOG_FLUSH_MILLIS, TimeUnit.MILLISECONDS) }
        super.onDestroy()
    }

    private fun readRulesAsset(): String? =
        runCatching { assets.open(RULES_ASSET).bufferedReader().use { it.readText() } }
            .onFailure { if (it !is IOException) throw it }
            .getOrNull()

    companion object {
        private const val RULES_ASSET = "rules.json"

        /** サービスの終了時に、依頼済みの検知ログの書き込みを待つ上限。 */
        private const val LOG_FLUSH_MILLIS = 1_000L

        fun detectionLog(context: Context) = DetectionLog(File(context.filesDir, DetectionLog.FILE_NAME))

        fun usageModeStore(context: Context) = UsageModeStore(SharedPreferencesStringStore.of(context))

        /** この版で有効な機能（ビルドの種類で決まる）。 */
        val FEATURES: FeatureSet = FeatureSet.forBuild(BuildConfig.DISTRIBUTION)

        fun pauseStore(context: Context) = PauseStore(SharedPreferencesStringStore.of(context))

        fun exclusionStore(context: Context) = ExclusionStore(SharedPreferencesStringStore.of(context))

        fun badgeSettings(context: Context) = BadgeSettings(SharedPreferencesStringStore.of(context))

        fun consentStore(context: Context) = ConsentStore(SharedPreferencesStringStore.of(context))
    }
}
