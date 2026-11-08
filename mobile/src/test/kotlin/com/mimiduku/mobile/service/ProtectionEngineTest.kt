package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityEvent
import com.mimiduku.mobile.R
import com.mimiduku.mobile.detect.AdScreenDetector
import com.mimiduku.mobile.detect.AdScreenState
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.detect.PackageClassifier
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.overlay.BannerAction
import com.mimiduku.mobile.overlay.BannerSpec
import com.mimiduku.mobile.rules.DetectionRules
import com.mimiduku.mobile.rules.ScarewareMatcher
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEngineTest {
    private var mode = UsageMode.CHILD
    private var screen = listOf("今日のニュース")
    private var scans = 0
    private val exclusions = ExclusionStore(MemoryStringStore(), saltGenerator = { "salt" })

    private val engine =
        ProtectionEngine(
            detector = ExternalTransitionDetector(PackageClassifier(DetectionRules.DEFAULT, OWN)),
            matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases),
            modeProvider = { mode },
            adDetector = AdScreenDetector(DetectionRules.DEFAULT),
            exclusions = exclusions,
        )

    private fun handle(event: EngineEvent) =
        engine.handle(event) {
            scans++
            screen
        }

    @Test
    fun `a browser that comes to the front without a window event is detected`() {
        handle(EngineEvent.WindowChanged(GAME, 0))

        // ウィンドウ状態の変化が届かず、文字の変化だけが届く。実際のアクティブなウィンドウはブラウザ。
        val event = engine.promoteToForeground(EngineEvent.ContentChanged(CHROME, 1_000)) { CHROME }
        val detections = handle(event)

        assertEquals(EngineEvent.WindowChanged(CHROME, 1_000), event)
        assertEquals(1, detections.size)
        assertEquals(GAME, detections.single().transition?.sourcePackage)
    }

    @Test
    fun `a content change in the background is not taken as the foreground`() {
        handle(EngineEvent.WindowChanged(GAME, 0))

        val event = EngineEvent.ContentChanged(CHROME, 1_000)

        assertEquals(event, engine.promoteToForeground(event) { GAME })
        assertEquals(event, engine.promoteToForeground(event) { null })
    }

    @Test
    fun `the active window is not looked up for the foreground app or transient windows`() {
        handle(EngineEvent.WindowChanged(GAME, 0))
        var lookups = 0
        val active = {
            lookups++
            GAME
        }

        engine.promoteToForeground(EngineEvent.ContentChanged(GAME, 100), active)
        engine.promoteToForeground(EngineEvent.ContentChanged("com.android.systemui", 200), active)
        engine.promoteToForeground(EngineEvent.ContentChanged(OWN, 300), active)
        engine.promoteToForeground(EngineEvent.Clicked(CHROME, 400), active)

        assertEquals(0, lookups)
    }

    @Test
    fun `accessibility events are converted to engine events`() {
        val window = EngineEvent.from(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, GAME, 1)
        val content = EngineEvent.from(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, GAME, 2)
        val click = EngineEvent.from(AccessibilityEvent.TYPE_VIEW_CLICKED, GAME, 3)

        assertEquals(EngineEvent.WindowChanged(GAME, 1), window)
        assertEquals(EngineEvent.ContentChanged(GAME, 2), content)
        assertEquals(EngineEvent.Clicked(GAME, 3), click)
    }

    @Test
    fun `unused event types and missing package names are ignored`() {
        assertNull(EngineEvent.from(AccessibilityEvent.TYPE_VIEW_SCROLLED, GAME, 1))
        assertNull(EngineEvent.from(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, null, 1))
        assertNull(EngineEvent.from(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, "", 1))
    }

    @Test
    fun `child mode goes back and logs an external transition`() {
        handle(EngineEvent.WindowChanged(GAME, 0))
        handle(EngineEvent.Clicked(GAME, 500))

        val detection = handle(EngineEvent.WindowChanged(CHROME, 1_000)).single()

        assertEquals(InterventionAction.BACK_AND_WARN, detection.intervention.action)
        assertEquals(DetectionKind.EXTERNAL_TRANSITION, detection.logEntry.kind)
        assertEquals(GAME, detection.logEntry.sourcePackage)
        assertEquals(CHROME, detection.logEntry.targetPackage)
        assertEquals(true, detection.logEntry.clickedRecently)
        assertEquals(UsageMode.CHILD, detection.logEntry.usageMode)
        assertEquals(R.string.child_transition_blocked_title, BannerSpec.from(detection).titleRes)
    }

    @Test
    fun `adult mode asks the user with go back and proceed buttons`() {
        mode = UsageMode.ADULT
        handle(EngineEvent.WindowChanged(GAME, 0))

        val detection = handle(EngineEvent.WindowChanged(CHROME, 1_000)).single()
        val banner = BannerSpec.from(detection)

        assertEquals(InterventionAction.ASK_USER, detection.intervention.action)
        assertEquals(BannerAction.GO_BACK, banner.primaryAction)
        assertEquals(R.string.adult_proceed, banner.secondaryLabelRes)
        assertEquals(R.string.target_browser, banner.formatArgRes)
    }

    @Test
    fun `adult mode skips the confirmation for an excluded source app and logs it`() {
        mode = UsageMode.ADULT
        exclusions.addPackage(GAME)
        handle(EngineEvent.WindowChanged(GAME, 0))

        val detection = handle(EngineEvent.WindowChanged(CHROME, 1_000)).single()

        assertTrue(detection.skipConfirmation)
        assertEquals(InterventionAction.SKIPPED_EXCLUDED, detection.logEntry.action)
        assertEquals(GAME, detection.logEntry.sourcePackage)
    }

    @Test
    fun `an excluded source app does not skip automatic back in child mode`() {
        exclusions.addPackage(GAME)
        handle(EngineEvent.WindowChanged(GAME, 0))

        val detection = handle(EngineEvent.WindowChanged(CHROME, 1_000)).single()

        assertEquals(InterventionAction.BACK_AND_WARN, detection.intervention.action)
        assertEquals(false, detection.skipConfirmation)
    }

    @Test
    fun `a source app that is not excluded is still confirmed`() {
        mode = UsageMode.ADULT
        exclusions.addPackage("com.example.other")
        handle(EngineEvent.WindowChanged(GAME, 0))

        val detection = handle(EngineEvent.WindowChanged(CHROME, 1_000)).single()

        assertEquals(false, detection.skipConfirmation)
        assertEquals(InterventionAction.ASK_USER, detection.logEntry.action)
    }

    @Test
    fun `scareware text in the foreground app is warned and logged without the text`() {
        handle(EngineEvent.WindowChanged(GAME, 0))
        screen = listOf("警告", "お使いの端末はウイルスに感染しています")

        val detection = handle(EngineEvent.ContentChanged(GAME, 2_000)).single()

        assertEquals(InterventionAction.WARN_ONLY, detection.intervention.action)
        assertEquals(DetectionKind.SCAREWARE, detection.logEntry.kind)
        assertEquals("virus-infected-ja", detection.logEntry.ruleId)
        assertEquals(R.string.child_scareware_title, BannerSpec.from(detection).titleRes)
        assertTrue(detection.logEntry.toString().contains("ウイルス").not())
    }

    @Test
    fun `scareware is checked when the window changes`() {
        screen = listOf("Your device is infected")

        val detection = handle(EngineEvent.WindowChanged(CHROME, 0)).single()

        assertEquals("device-infected-en", detection.logEntry.ruleId)
    }

    @Test
    fun `content changes are scanned at most once per interval`() {
        handle(EngineEvent.WindowChanged(GAME, 0))
        val afterWindow = scans

        handle(EngineEvent.ContentChanged(GAME, 100))
        handle(EngineEvent.ContentChanged(GAME, 500))
        assertEquals(afterWindow, scans)

        handle(EngineEvent.ContentChanged(GAME, 1_100))
        assertEquals(afterWindow + 1, scans)
    }

    @Test
    fun `the same scareware warning is not repeated within the repeat window`() {
        screen = listOf("ウイルスに感染")
        handle(EngineEvent.WindowChanged(GAME, 0))

        assertTrue(handle(EngineEvent.ContentChanged(GAME, 2_000)).isEmpty())
        assertEquals(1, handle(EngineEvent.ContentChanged(GAME, 61_000)).size)
    }

    @Test
    fun `transient windows are not scanned`() {
        handle(EngineEvent.WindowChanged(GAME, 0))
        screen = listOf("ウイルスに感染")

        assertTrue(handle(EngineEvent.ContentChanged("com.android.systemui", 5_000)).isEmpty())
        assertTrue(handle(EngineEvent.WindowChanged(OWN, 6_000)).isEmpty())
    }

    @Test
    fun `clicks produce no detection`() {
        assertTrue(handle(EngineEvent.Clicked(GAME, 0)).isEmpty())
    }

    @Test
    fun `the class name of the window is passed through`() {
        assertEquals(
            EngineEvent.WindowChanged(GAME, 1, AD_ACTIVITY),
            EngineEvent.from(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, GAME, 1, AD_ACTIVITY),
        )
    }

    @Test
    fun `an ad activity in the foreground sets the ad screen state`() {
        screen = listOf("あと5秒でスキップ")

        handle(EngineEvent.WindowChanged(GAME, 0, AD_ACTIVITY))

        assertEquals(AdScreenState(countdownSeconds = 5, mode = UsageMode.CHILD, fullScreen = true), engine.adScreen)
    }

    @Test
    fun `the countdown follows content changes and the state clears when the ad closes`() {
        screen = listOf("Ad", "Skip in 5")
        handle(EngineEvent.WindowChanged(GAME, 0, "com.example.game.MainActivity"))
        assertEquals(5, engine.adScreen?.countdownSeconds)

        screen = listOf("Ad", "Skip in 4")
        handle(EngineEvent.ContentChanged(GAME, 1_000))
        assertEquals(4, engine.adScreen?.countdownSeconds)

        screen = listOf("スタート")
        handle(EngineEvent.ContentChanged(GAME, 2_000))
        assertNull(engine.adScreen)
    }

    @Test
    fun `a keyboard over the ad keeps the ad screen state`() {
        handle(EngineEvent.WindowChanged(GAME, 0, AD_ACTIVITY))

        handle(EngineEvent.WindowChanged("com.android.systemui", 100, "android.widget.FrameLayout"))

        assertTrue(engine.adScreen != null)
    }

    @Test
    fun `an external transition clears the ad screen state`() {
        handle(EngineEvent.WindowChanged(GAME, 0, AD_ACTIVITY))

        handle(EngineEvent.WindowChanged(CHROME, 100))

        assertNull(engine.adScreen)
    }

    private companion object {
        const val OWN = "com.mimiduku.mobile"
        const val GAME = "com.example.game"
        const val CHROME = "com.android.chrome"
        const val AD_ACTIVITY = "com.google.android.gms.ads.AdActivity"
    }
}
