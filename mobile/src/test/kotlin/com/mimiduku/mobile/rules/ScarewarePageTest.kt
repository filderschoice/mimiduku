package com.mimiduku.mobile.rules

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * 実機確認用のテストページ（`tools/verification/scareware-page/`、docs/TESTING.md 2.3 の T9〜T11。BL-073）の本文が、
 * 同梱の検知ルールで警告の対象になる（または、ならない）ことを確かめる。ページの文言や照合語を変えたときに、
 * 実機で確かめる前に食い違いに気づけるようにする。
 */
class ScarewarePageTest {
    private val matcher = ScarewareMatcher(DetectionRules.DEFAULT.scarewarePhrases)

    @Test
    fun `the page shown on load contains a scareware phrase`() {
        val html = page("index.html")

        assertNotNull(matcher.findMatch(listOf(visibleText(html))))
    }

    @Test
    fun `the scroll page has no phrase on load and shows one after scrolling`() {
        val html = page("scroll.html")
        val template =
            Regex("""<template id="late">(.*?)</template>""", RegexOption.DOT_MATCHES_ALL)
                .find(html)
                ?.groupValues
                ?.get(1)
                ?: error("scroll.html に <template id=\"late\"> が見つからない")

        assertNull(matcher.findMatch(listOf(visibleText(html))))
        assertNotNull(matcher.findMatch(listOf(visibleText(template))))
    }

    @Test
    fun `the link target page has no scareware phrase`() {
        assertNull(matcher.findMatch(listOf(visibleText(page("safe.html")))))
    }

    private fun page(name: String): String = File("../tools/verification/scareware-page/$name").readText()

    /** 読み込み直後に表示される文字だけを残す（コメント・head・script・template を除き、タグを外す）。 */
    private fun visibleText(html: String): String =
        html
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<head>.*?</head>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<script>.*?</script>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<template[^>]*>.*?</template>", RegexOption.DOT_MATCHES_ALL), " ")
            .replace(Regex("<[^>]+>"), " ")
}
