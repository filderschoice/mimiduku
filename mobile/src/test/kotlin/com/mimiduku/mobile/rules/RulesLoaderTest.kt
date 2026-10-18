package com.mimiduku.mobile.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RulesLoaderTest {
    @Test
    fun `the bundled rules json matches the default rules`() {
        // 単体テストの作業ディレクトリはモジュール（mobile/）。
        val text = File("src/main/assets/rules.json").readText()

        val result = RulesLoader.parse(text)

        assertFalse(result.usedFallback)
        assertEquals(DetectionRules.DEFAULT, result.rules)
    }

    @Test
    fun `blank or null text falls back to the default rules`() {
        assertTrue(RulesLoader.parse(null).usedFallback)
        assertTrue(RulesLoader.parse("  ").usedFallback)
    }

    @Test
    fun `broken json falls back to the default rules`() {
        val result = RulesLoader.parse("{\"version\": 1, \"browserPackages\": [")

        assertTrue(result.usedFallback)
        assertEquals(DetectionRules.DEFAULT, result.rules)
    }

    @Test
    fun `missing required keys fall back to the default rules`() {
        assertTrue(RulesLoader.parse("{\"version\": 1}").usedFallback)
    }

    @Test
    fun `an unsupported version falls back to the default rules`() {
        val text = File("src/main/assets/rules.json").readText().replace("\"version\": 1", "\"version\": 99")

        assertTrue(RulesLoader.parse(text).usedFallback)
    }

    @Test
    fun `rules with a blank phrase are rejected`() {
        val text =
            """
            {"version": 1, "browserPackages": ["a"], "storePackages": [], "installerPackages": [],
             "transientPackages": [], "ignoredSourcePackages": [],
             "scarewarePhrases": [{"id": "x", "text": ""}]}
            """.trimIndent()

        assertTrue(RulesLoader.parse(text).usedFallback)
    }

    @Test
    fun `unknown keys are ignored`() {
        val text =
            """
            {"version": 1, "browserPackages": ["a"], "storePackages": [], "installerPackages": [],
             "transientPackages": [], "ignoredSourcePackages": [],
             "scarewarePhrases": [{"id": "x", "text": "y"}], "futureKey": true}
            """.trimIndent()

        val result = RulesLoader.parse(text)

        assertFalse(result.usedFallback)
        assertEquals(listOf("a"), result.rules.browserPackages)
    }
}
