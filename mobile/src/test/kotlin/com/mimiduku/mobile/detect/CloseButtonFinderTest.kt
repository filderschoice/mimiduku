package com.mimiduku.mobile.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CloseButtonFinderTest {
    private fun node(
        text: String? = null,
        description: String? = null,
    ) = NodeSnapshot(
        text = text,
        contentDescription = description,
        clickable = true,
        visible = true,
        left = 900,
        top = 50,
        right = 1000,
        bottom = 150,
    )

    private fun find(vararg nodes: NodeSnapshot) = CloseButtonFinder.find(nodes.toList(), W, H)

    @Test
    fun `a small clickable close button is chosen`() {
        assertEquals(1, find(node(text = "広告"), node(text = "×")))
        assertEquals(0, find(node(description = "Close")))
        assertEquals(0, find(node(text = "閉じる")))
    }

    @Test
    fun `close is preferred over skip`() {
        assertEquals(1, find(node(text = "Skip Ad"), node(description = "close ad")))
    }

    @Test
    fun `skip is used when there is no close button`() {
        assertEquals(1, find(node(text = "今すぐプレイ"), node(text = "スキップ")))
    }

    @Test
    fun `sentences that only contain a close word are not chosen`() {
        assertNull(find(node(text = "今すぐインストールして閉じる"), node(text = "Close and install")))
    }

    @Test
    fun `nodes that cannot be clicked or are hidden are not chosen`() {
        assertNull(find(node(text = "×").copy(clickable = false), node(text = "×").copy(visible = false)))
    }

    @Test
    fun `nodes outside the screen or with no size are not chosen`() {
        assertNull(find(node(text = "×").copy(left = -10, right = 50), node(text = "×").copy(right = 900)))
        assertNull(find(node(text = "×").copy(top = 1900, bottom = 2100)))
    }

    @Test
    fun `a large area that may be the ad itself is not chosen`() {
        assertNull(find(node(text = "Close").copy(left = 0, top = 0, right = W, bottom = H / 2)))
    }

    @Test
    fun `no candidates yields null`() {
        assertNull(find())
    }

    private companion object {
        const val W = 1080
        const val H = 2000
    }
}
