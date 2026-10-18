package com.mimiduku.mobile.detect

import com.mimiduku.mobile.rules.DetectionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdScreenDetectorTest {
    private val detector = AdScreenDetector(DetectionRules.DEFAULT)

    @Test
    fun `a known ad sdk activity is an ad screen`() {
        val verdict = detector.evaluate("com.google.android.gms.ads.AdActivity", emptyList())

        assertTrue(verdict.isAd)
        assertTrue(verdict.byActivity)
    }

    @Test
    fun `an ordinary activity without markers is not an ad screen`() {
        assertFalse(detector.evaluate("com.example.game.MainActivity", listOf("スタート", "設定")).isAd)
    }

    @Test
    fun `an ad label alone is not enough`() {
        val verdict = detector.evaluate(null, listOf("広告", "今日のおすすめ"))

        assertFalse(verdict.isAd)
        assertEquals(setOf("ad-label"), verdict.markerIds)
    }

    @Test
    fun `an ad label with a skip button is an ad screen`() {
        val verdict = detector.evaluate("x.y.Obfuscated", listOf("Ad", "Skip"))

        assertTrue(verdict.isAd)
        assertFalse(verdict.byActivity)
        assertEquals(setOf("ad-label", "skip"), verdict.markerIds)
    }

    @Test
    fun `a reward countdown with an install button is an ad screen`() {
        assertTrue(detector.evaluate(null, listOf("あと5秒で報酬を獲得", "インストール")).isAd)
    }

    @Test
    fun `short labels only match the whole node text`() {
        // 「Ad」が単語の一部（Address・Download）に含まれても数えない。
        val verdict = detector.evaluate(null, listOf("Address", "Download", "Skipping rope"))

        assertTrue(verdict.markerIds.isEmpty())
    }

    @Test
    fun `the same marker id is counted once`() {
        val verdict = detector.evaluate(null, listOf("広告", "Ad", "AD", "スポンサー"))

        assertFalse(verdict.isAd)
    }

    @Test
    fun `rules without ad entries never detect an ad screen`() {
        val rules = DetectionRules.DEFAULT.copy(adActivityPrefixes = emptyList(), adTextMarkers = emptyList())

        assertFalse(AdScreenDetector(rules).evaluate("com.google.android.gms.ads.AdActivity", listOf("Skip")).isAd)
    }
}
