package com.mimiduku.mobile.detect

import com.mimiduku.mobile.rules.DetectionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTransitionDetectorTest {
    private val classifier =
        PackageClassifier(
            rules = DetectionRules.DEFAULT,
            ownPackage = OWN,
            deviceBrowsers = setOf("example.browser"),
            launchers = setOf(LAUNCHER),
            inputMethods = setOf(KEYBOARD),
        )
    private val detector = ExternalTransitionDetector(classifier)

    @Test
    fun `an app opening the browser is detected`() {
        detector.onWindowStateChanged(GAME, 0)

        val transition = detector.onWindowStateChanged(CHROME, 100)

        assertEquals(ExternalTransition(GAME, CHROME, TransitionTarget.BROWSER, false, 1), transition)
    }

    @Test
    fun `an app opening the store or the installer is detected`() {
        detector.onWindowStateChanged(GAME, 0)
        assertEquals(TransitionTarget.STORE, detector.onWindowStateChanged("com.android.vending", 100)?.target)

        detector.onWindowStateChanged(GAME, 200)
        val installer = detector.onWindowStateChanged("com.google.android.packageinstaller", 300)
        assertEquals(TransitionTarget.INSTALLER, installer?.target)
    }

    @Test
    fun `a browser found on the device is treated as a browser`() {
        detector.onWindowStateChanged(GAME, 0)

        assertEquals(TransitionTarget.BROWSER, detector.onWindowStateChanged("example.browser", 100)?.target)
    }

    @Test
    fun `opening the browser from the launcher or settings is not detected`() {
        detector.onWindowStateChanged(LAUNCHER, 0)
        assertNull(detector.onWindowStateChanged(CHROME, 100))

        detector.onWindowStateChanged("com.android.settings", 200)
        assertNull(detector.onWindowStateChanged(CHROME, 300))
    }

    @Test
    fun `moving between browsers is not detected`() {
        detector.onWindowStateChanged(CHROME, 0)

        assertNull(detector.onWindowStateChanged("org.mozilla.firefox", 100))
    }

    @Test
    fun `a browser opening the store is detected`() {
        detector.onWindowStateChanged(CHROME, 0)

        assertEquals(TransitionTarget.STORE, detector.onWindowStateChanged("com.android.vending", 100)?.target)
    }

    @Test
    fun `the store opening the browser is not detected`() {
        detector.onWindowStateChanged("com.android.vending", 0)

        assertNull(detector.onWindowStateChanged(CHROME, 100))
    }

    @Test
    fun `transient windows do not change the foreground`() {
        detector.onWindowStateChanged(GAME, 0)
        detector.onWindowStateChanged(KEYBOARD, 50)
        detector.onWindowStateChanged("com.android.systemui", 60)
        detector.onWindowStateChanged(OWN, 70)

        assertEquals(GAME, detector.currentForeground)
        assertEquals(GAME, detector.onWindowStateChanged(CHROME, 100)?.sourcePackage)
    }

    @Test
    fun `the same package again is not a transition`() {
        detector.onWindowStateChanged(CHROME, 0)

        assertNull(detector.onWindowStateChanged(CHROME, 100))
    }

    @Test
    fun `the first window is never a transition`() {
        assertNull(detector.onWindowStateChanged(CHROME, 0))
    }

    @Test
    fun `returning from the browser to the app is not detected`() {
        detector.onWindowStateChanged(GAME, 0)
        detector.onWindowStateChanged(CHROME, 100)

        assertNull(detector.onWindowStateChanged(GAME, 200))
    }

    @Test
    fun `a recent click in the source app is reported`() {
        detector.onWindowStateChanged(GAME, 0)
        detector.onViewClicked(GAME, 1_000)

        assertTrue(detector.onWindowStateChanged(CHROME, 2_000)!!.clickedRecently)
    }

    @Test
    fun `an old click or a click in another app is not reported as recent`() {
        detector.onWindowStateChanged(GAME, 0)
        detector.onViewClicked(GAME, 1_000)
        assertFalse(detector.onWindowStateChanged(CHROME, 5_000)!!.clickedRecently)

        detector.onWindowStateChanged(GAME, 6_000)
        detector.onViewClicked(OTHER_APP, 6_500)
        assertFalse(detector.onWindowStateChanged(CHROME, 7_000)!!.clickedRecently)
    }

    @Test
    fun `repeated transitions from the same source are counted within the window`() {
        val counts =
            (0 until 3).map { i ->
                detector.onWindowStateChanged(GAME, i * 1_000L)
                detector.onWindowStateChanged(CHROME, i * 1_000L + 100)!!.repeatCount
            }

        assertEquals(listOf(1, 2, 3), counts)
    }

    @Test
    fun `the repeat count resets after the window and is kept per source`() {
        detector.onWindowStateChanged(GAME, 0)
        detector.onWindowStateChanged(CHROME, 100)
        detector.onWindowStateChanged(OTHER_APP, 200)
        assertEquals(1, detector.onWindowStateChanged(CHROME, 300)!!.repeatCount)

        detector.onWindowStateChanged(GAME, 40_000)
        assertEquals(1, detector.onWindowStateChanged(CHROME, 40_100)!!.repeatCount)
    }

    private companion object {
        const val OWN = "com.mimiduku.mobile"
        const val GAME = "com.example.game"
        const val OTHER_APP = "com.example.other"
        const val CHROME = "com.android.chrome"
        const val LAUNCHER = "com.example.launcher"
        const val KEYBOARD = "com.example.keyboard"
    }
}
