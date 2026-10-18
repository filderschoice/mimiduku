package com.mimiduku.mobile.overlay

import com.mimiduku.mobile.detect.AdScreenState
import com.mimiduku.mobile.mode.UsageMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldTimerTest {
    private val timer = ShieldTimer(maxMillis = 60_000)
    private val childFullScreen = AdScreenState(countdownSeconds = 5, mode = UsageMode.CHILD, fullScreen = true)

    @Test
    fun `a full screen ad in child mode is shielded`() {
        assertTrue(timer.shouldShield(childFullScreen, 0))
    }

    @Test
    fun `adult mode and in app ads are not shielded`() {
        assertFalse(timer.shouldShield(childFullScreen.copy(mode = UsageMode.ADULT), 0))
        assertFalse(timer.shouldShield(childFullScreen.copy(fullScreen = false), 0))
        assertFalse(timer.shouldShield(null, 0))
    }

    @Test
    fun `the shield is released after the limit and not raised again for the same ad`() {
        timer.shouldShield(childFullScreen, 0)

        assertTrue(timer.shouldShield(childFullScreen, 59_999))
        assertFalse(timer.shouldShield(childFullScreen, 60_000))
        assertFalse(timer.shouldShield(childFullScreen, 90_000))
    }

    @Test
    fun `a new ad after the previous one ended is shielded again`() {
        timer.shouldShield(childFullScreen, 0)
        timer.shouldShield(childFullScreen, 70_000)

        timer.shouldShield(null, 71_000)

        assertTrue(timer.shouldShield(childFullScreen, 72_000))
    }
}
