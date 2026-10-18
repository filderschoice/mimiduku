package com.mimiduku.mobile.service

import android.view.accessibility.AccessibilityEvent
import com.mimiduku.mobile.R
import com.mimiduku.mobile.detect.ExternalTransitionDetector
import com.mimiduku.mobile.detect.PackageClassifier
import com.mimiduku.mobile.log.DetectionKind
import com.mimiduku.mobile.log.InterventionAction
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.overlay.BannerAction
import com.mimiduku.mobile.overlay.BannerSpec
import com.mimiduku.mobile.rules.DetectionRules
import com.mimiduku.mobile.rules.ScarewareMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionEngineTest {
    private var mode = UsageMode.CHILD
    private var screen = listOf("今日のニュース")
    private var scans = 0

    private val engine =
        ProtectionEngine(
            detector = ExternalTransitionDetector(PackageClassifier(DetectionRules.DEFAULT, OWN)),
            matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases),
            modeProvider = { mode },
        )

    private fun handle(event: EngineEvent) =
        engine.handle(event) {
            scans++
            screen
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

    private companion object {
        const val OWN = "com.mimiduku.mobile"
        const val GAME = "com.example.game"
        const val CHROME = "com.android.chrome"
    }
}
