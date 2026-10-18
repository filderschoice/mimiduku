package com.mimiduku.mobile.pin

import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinManagerTest {
    private val store = MemoryStringStore()
    private var now = 1_000_000L

    // テストを速くするため反復回数を下げる（形式と照合の流れは本番と同じ）。
    private val manager = PinManager(store, clock = { now }, hasher = PinHasher(iterations = 1_000))

    @Test
    fun `a pin that is set can be verified`() {
        assertTrue(manager.set("1234"))

        assertTrue(manager.isSet())
        assertEquals(PinCheck.Success, manager.verify("1234"))
    }

    @Test
    fun `a wrong pin is rejected`() {
        manager.set("1234")

        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 4), manager.verify("4321"))
    }

    @Test
    fun `verifying without a pin reports not set`() {
        assertFalse(manager.isSet())
        assertEquals(PinCheck.NotSet, manager.verify("1234"))
    }

    @Test
    fun `the pin format is four to eight digits`() {
        assertFalse(manager.set("123"))
        assertFalse(manager.set("123456789"))
        assertFalse(manager.set("12a4"))
        assertFalse(manager.set("１２３４"))
        assertTrue(manager.set("12345678"))
    }

    @Test
    fun `the plain pin is never stored`() {
        manager.set("246810")

        assertTrue(store.values.values.none { it.contains("246810") })
    }

    @Test
    fun `the same pin hashes differently each time because of the salt`() {
        val hasher = PinHasher(iterations = 1_000)

        assertNotEquals(hasher.hash("1234"), hasher.hash("1234"))
    }

    @Test
    fun `five failures lock the pin for thirty seconds`() {
        manager.set("1234")
        repeat(4) { manager.verify("0000") }

        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 0), manager.verify("0000"))
        assertEquals(PinCheck.Locked(30_000), manager.verify("1234"))

        now += 30_000
        assertEquals(PinCheck.Success, manager.verify("1234"))
    }

    @Test
    fun `the lock doubles with each further failure up to thirty minutes`() {
        assertEquals(0L, PinManager.lockMillisFor(4))
        assertEquals(30_000L, PinManager.lockMillisFor(5))
        assertEquals(60_000L, PinManager.lockMillisFor(6))
        assertEquals(120_000L, PinManager.lockMillisFor(7))
        assertEquals(30 * 60_000L, PinManager.lockMillisFor(11))
        assertEquals(30 * 60_000L, PinManager.lockMillisFor(100))
    }

    @Test
    fun `a success resets the failure count`() {
        manager.set("1234")
        repeat(4) { manager.verify("0000") }
        manager.verify("1234")

        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 4), manager.verify("0000"))
    }

    @Test
    fun `the lock survives a new manager over the same store`() {
        manager.set("1234")
        repeat(5) { manager.verify("0000") }

        val reopened = PinManager(store, clock = { now }, hasher = PinHasher(iterations = 1_000))

        assertTrue(reopened.verify("1234") is PinCheck.Locked)
    }

    @Test
    fun `a corrupted stored value cannot be verified and still counts as set`() {
        store.values["pin_hash"] = "broken"

        assertTrue(manager.isSet())
        assertEquals(PinCheck.Corrupted, manager.verify("1234"))
    }

    @Test
    fun `clearing the pin removes it`() {
        manager.set("1234")

        manager.clear()

        assertFalse(manager.isSet())
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun `a pin stored with another iteration count still verifies`() {
        store.values["pin_hash"] = PinHasher(iterations = 2_000).hash("5678")

        assertEquals(PinCheck.Success, manager.verify("5678"))
    }
}
