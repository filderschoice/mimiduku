package com.mimiduku.mobile.overlay

import com.mimiduku.mobile.mode.BadgeSettings
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgeExpressionTest {
    private fun of(
        onAdScreen: Boolean = false,
        lastDangerAt: Long? = null,
        now: Long = 100_000,
    ) = BadgeExpression.of(
        visible = true,
        paused = false,
        onAdScreen = onAdScreen,
        lastDangerAtMillis = lastDangerAt,
        nowMillis = now,
    )

    @Test
    fun `the owl dozes when nothing happens`() {
        assertEquals(BadgeExpression.SLEEPY, of())
    }

    @Test
    fun `the ears rise on an ad screen`() {
        assertEquals(BadgeExpression.ALERT, of(onAdScreen = true))
    }

    @Test
    fun `the eyes widen right after a dangerous detection and go back after ten seconds`() {
        assertEquals(BadgeExpression.WIDE_EYED, of(onAdScreen = true, lastDangerAt = 95_000))
        assertEquals(BadgeExpression.ALERT, of(onAdScreen = true, lastDangerAt = 90_000))
        assertEquals(BadgeExpression.SLEEPY, of(lastDangerAt = 90_000))
    }

    @Test
    fun `a danger time in the future is ignored`() {
        assertEquals(BadgeExpression.SLEEPY, of(lastDangerAt = 200_000))
    }

    @Test
    fun `the badge is hidden when turned off or paused`() {
        assertNull(BadgeExpression.of(false, false, true, null, 0))
        assertNull(BadgeExpression.of(true, true, true, null, 0))
    }

    @Test
    fun `the badge setting defaults to visible and can be turned off`() {
        val store = MemoryStringStore()
        val settings = BadgeSettings(store)

        assertTrue(settings.isVisible())
        settings.setVisible(false)
        assertFalse(BadgeSettings(store).isVisible())
        settings.setVisible(true)
        assertTrue(store.values.isEmpty())
    }
}
