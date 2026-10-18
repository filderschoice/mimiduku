package com.mimiduku.mobile.detect

import com.mimiduku.mobile.R
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.overlay.AdPanelSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountdownParserTest {
    @Test
    fun `japanese countdowns are read`() {
        assertEquals(5, CountdownParser.parse(listOf("あと5秒でスキップできます")))
        assertEquals(12, CountdownParser.parse(listOf("報酬まで", "12秒後に獲得")))
        assertEquals(3, CountdownParser.parse(listOf("3 秒で閉じられます")))
    }

    @Test
    fun `english countdowns are read regardless of case and spacing`() {
        assertEquals(5, CountdownParser.parse(listOf("Skip in 5")))
        assertEquals(30, CountdownParser.parse(listOf("Reward in 30 s")))
        assertEquals(8, CountdownParser.parse(listOf("8 seconds remaining")))
    }

    @Test
    fun `full width digits are read`() {
        assertEquals(7, CountdownParser.parse(listOf("あと７秒")))
    }

    @Test
    fun `bare numbers and out of range values are ignored`() {
        assertNull(CountdownParser.parse(listOf("5", "スコア 1200")))
        assertNull(CountdownParser.parse(listOf("あと0秒")))
        assertNull(CountdownParser.parse(listOf("あと999秒")))
        assertNull(CountdownParser.parse(emptyList()))
    }

    @Test
    fun `the panel wording follows the usage mode`() {
        val child = AdPanelSpec.from(AdScreenState(countdownSeconds = 5, mode = UsageMode.CHILD))
        val adult = AdPanelSpec.from(AdScreenState(countdownSeconds = null, mode = UsageMode.ADULT))

        assertEquals(R.string.child_ad_panel_countdown, child.countdownMessageRes)
        assertEquals(5, child.countdownSeconds)
        assertEquals(R.string.adult_ad_panel_message, adult.messageRes)
        assertEquals(R.string.adult_go_back, adult.backLabelRes)
    }
}
