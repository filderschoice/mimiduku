package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PauseStoreTest {
    private val store = MemoryStringStore()
    private var now = 1_000_000L
    private val pause = PauseStore(store) { now }

    @Test
    fun `protection is not paused by default`() {
        assertFalse(pause.isPaused())
        assertEquals(0L, pause.remainingMillis())
    }

    @Test
    fun `a pause lasts for the chosen duration`() {
        pause.pause(15 * 60_000L)

        now += 10 * 60_000L
        assertTrue(pause.isPaused())
        assertEquals(5 * 60_000L, pause.remainingMillis())

        now += 5 * 60_000L
        assertFalse(pause.isPaused())
    }

    @Test
    fun `an expired pause is cleared from the store`() {
        pause.pause(1_000)
        now += 1_000

        pause.isPaused()

        assertTrue(store.values.isEmpty())
    }

    @Test
    fun `moving the clock back before the start ends the pause`() {
        pause.pause(60 * 60_000L)

        now -= 1

        assertFalse(pause.isPaused())
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun `resume ends the pause`() {
        pause.pause(60_000)

        pause.resume()

        assertFalse(pause.isPaused())
    }

    @Test
    fun `the pause survives a new store instance as after a reboot`() {
        pause.pause(60_000)

        assertTrue(PauseStore(store) { now + 30_000 }.isPaused())
    }

    @Test
    fun `a corrupted stored value is treated as not paused`() {
        store.values["pause_started_at"] = "x"
        store.values["pause_until"] = "y"

        assertFalse(pause.isPaused())
        assertTrue(store.values.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a non positive duration is rejected`() {
        pause.pause(0)
    }
}
