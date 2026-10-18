package com.mimiduku.mobile.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScarewareMatcherTest {
    private val matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases)

    @Test
    fun `a japanese phrase is detected`() {
        val match = matcher.findMatch(listOf("警告！", "お使いの端末はウイルスに感染しています"))

        assertEquals("virus-infected-ja", match?.id)
    }

    @Test
    fun `an english phrase is detected regardless of case and spacing`() {
        val match = matcher.findMatch(listOf("WARNING", "Your   Device  is INFECTED!"))

        assertEquals("device-infected-en", match?.id)
    }

    @Test
    fun `full width letters and half width kana are normalized`() {
        assertEquals("virus-detected-en", matcher.findMatch(listOf("ＶＩＲＵＳ ＤＥＴＥＣＴＥＤ"))?.id)
        assertEquals("battery-damaged-ja", matcher.findMatch(listOf("ﾊﾞｯﾃﾘｰが損傷しています"))?.id)
    }

    @Test
    fun `a phrase split across nodes is detected`() {
        val match = matcher.findMatch(listOf("バッテリーが", "損傷しています"))

        assertEquals("battery-damaged-ja", match?.id)
    }

    @Test
    fun `unrelated text is not detected`() {
        assertNull(matcher.findMatch(listOf("今日の天気は晴れです", "ウイルス対策のニュース")))
        assertNull(matcher.findMatch(emptyList()))
    }

    @Test
    fun `a blank phrase never matches`() {
        val matcher = ScarewareMatcher(listOf(ScarewarePhrase("blank", " ")))

        assertNull(matcher.findMatch(listOf("何かの文字")))
    }
}
