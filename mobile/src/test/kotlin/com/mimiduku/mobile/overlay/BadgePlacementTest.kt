package com.mimiduku.mobile.overlay

import com.mimiduku.mobile.mode.BadgeSettings
import com.mimiduku.mobile.storage.MemoryStringStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BadgePlacementTest {
    private val width = 1000
    private val height = 2000
    private val size = 100
    private val peek = 60

    private fun release(
        x: Int,
        y: Int,
        w: Int = width,
        h: Int = height,
    ) = BadgePlacement.fromRelease(x, y, w, h, size)

    @Test
    fun `default is the vertical center of the right edge`() {
        val placement = BadgePlacement.DEFAULT

        assertEquals(width - size, placement.windowX(width, size, peek))
        assertEquals((height - size) / 2, placement.windowY(height, size))
    }

    @Test
    fun `releasing near an edge snaps to the nearest side`() {
        assertEquals(BadgePlacement(BadgeSide.LEFT, 0.5f), release(x = 100, y = 950))
        assertEquals(BadgePlacement(BadgeSide.RIGHT, 0.5f), release(x = 700, y = 950))
    }

    @Test
    fun `pulling the badge past the edge minimizes it on that side`() {
        assertEquals(BadgePlacement(BadgeSide.LEFT, 0f, minimized = true), release(x = -60, y = 0))
        assertEquals(BadgePlacement(BadgeSide.RIGHT, 1f, minimized = true), release(x = 960, y = 1900))
    }

    @Test
    fun `badge only half out of the screen is not minimized`() {
        assertEquals(false, release(x = -49, y = 0).minimized)
        assertEquals(false, release(x = 899, y = 0).minimized)
    }

    @Test
    fun `minimized badge leaves only the peek inside the screen`() {
        val left = BadgePlacement(BadgeSide.LEFT, 0.5f, minimized = true)
        val right = BadgePlacement(BadgeSide.RIGHT, 0.5f, minimized = true)

        assertEquals(peek, left.windowX(width, size, peek) + size)
        assertEquals(width - peek, right.windowX(width, size, peek))
    }

    @Test
    fun `vertical position is clamped to the screen`() {
        assertEquals(0f, release(x = 0, y = -300).verticalFraction)
        assertEquals(1f, release(x = 0, y = 5000).verticalFraction)
    }

    @Test
    fun `the same placement fits both portrait and landscape`() {
        val placement = BadgePlacement(BadgeSide.RIGHT, 1f, minimized = true)

        // 横向き（幅と高さが入れ替わる）でも、右端の外側・下端に収まる。
        assertEquals(2000 - peek, placement.windowX(2000, size, peek))
        assertEquals(1000 - size, placement.windowY(1000, size))
        assertTrue(placement.windowY(1000, size) + size <= 1000)
    }

    @Test
    fun `placement survives encoding and broken values fall back to default`() {
        val placement = BadgePlacement(BadgeSide.LEFT, 0.25f, minimized = true)

        assertEquals(placement, BadgePlacement.decode(placement.encode()))
        assertEquals(BadgePlacement.DEFAULT, BadgePlacement.decode(null))
        assertEquals(BadgePlacement.DEFAULT, BadgePlacement.decode("TOP,0.5,false"))
        assertEquals(BadgePlacement.DEFAULT, BadgePlacement.decode("LEFT,2.0,false"))
        assertEquals(BadgePlacement.DEFAULT, BadgePlacement.decode("LEFT,0.5"))
    }

    @Test
    fun `settings keep the placement and drop the entry for the default`() {
        val store = MemoryStringStore()
        val settings = BadgeSettings(store)
        val moved = BadgePlacement(BadgeSide.LEFT, 0.8f)

        settings.setPlacement(moved)
        assertEquals(moved, BadgeSettings(store).placement())

        settings.setPlacement(BadgePlacement.DEFAULT)
        assertEquals(BadgePlacement.DEFAULT, settings.placement())
        assertTrue(store.values.isEmpty())
    }
}
