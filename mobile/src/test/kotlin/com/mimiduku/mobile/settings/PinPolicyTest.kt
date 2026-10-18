package com.mimiduku.mobile.settings

import com.mimiduku.mobile.R
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.pin.PinCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinPolicyTest {
    @Test
    fun `actions that weaken protection require the pin when it is set`() {
        listOf(
            GuardedAction.SWITCH_TO_ADULT,
            GuardedAction.CHANGE_PIN,
            GuardedAction.REMOVE_PIN,
            GuardedAction.PAUSE_PROTECTION,
            GuardedAction.CLEAR_LOG,
        ).forEach { assertTrue(it.name, PinPolicy.requiresPin(it, pinSet = true)) }
    }

    @Test
    fun `actions that strengthen protection never require the pin`() {
        listOf(GuardedAction.SWITCH_TO_CHILD, GuardedAction.RESUME_PROTECTION).forEach {
            assertFalse(it.name, PinPolicy.requiresPin(it, pinSet = true))
        }
    }

    @Test
    fun `nothing requires the pin when it is not set`() {
        GuardedAction.entries.forEach { assertFalse(it.name, PinPolicy.requiresPin(it, pinSet = false)) }
    }

    @Test
    fun `mode changes map to guarded actions`() {
        assertEquals(GuardedAction.SWITCH_TO_ADULT, PinPolicy.forModeChange(UsageMode.CHILD, UsageMode.ADULT))
        assertEquals(GuardedAction.SWITCH_TO_CHILD, PinPolicy.forModeChange(UsageMode.ADULT, UsageMode.CHILD))
        assertNull(PinPolicy.forModeChange(UsageMode.CHILD, UsageMode.CHILD))
    }

    @Test
    fun `each check result has a message except success`() {
        assertNull(PinPolicy.messageFor(PinCheck.Success))
        assertEquals(R.string.pin_error_wrong, PinPolicy.messageFor(PinCheck.Wrong(3)))
        assertEquals(R.string.pin_error_locked_now, PinPolicy.messageFor(PinCheck.Wrong(0)))
        assertEquals(R.string.pin_error_locked, PinPolicy.messageFor(PinCheck.Locked(1_000)))
        assertEquals(R.string.pin_error_not_set, PinPolicy.messageFor(PinCheck.NotSet))
        assertEquals(R.string.pin_error_corrupted, PinPolicy.messageFor(PinCheck.Corrupted))
    }
}
