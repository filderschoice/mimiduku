package com.mimiduku.mobile.rules

import java.text.Normalizer
import java.util.Locale

/**
 * 画面の文字とスケアウェアの文言を照合する。
 *
 * 全角半角（NFKC）・大文字小文字・空白の違いを吸収してから部分一致で比べる。文言がノードをまたいで
 * 分かれて表示される場合に備え、ノードごとの文字をつないだものとも比べる。
 */
class ScarewareMatcher(
    phrases: List<ScarewarePhrase>,
) {
    private val normalizedPhrases = phrases.map { it to normalize(it.text) }.filter { it.second.isNotEmpty() }

    /** 最初に一致した文言を返す。一致しなければ null。 */
    fun findMatch(texts: List<String>): ScarewarePhrase? {
        if (texts.isEmpty()) return null
        val joined = normalize(texts.joinToString(separator = ""))
        return normalizedPhrases.firstOrNull { (_, phrase) -> joined.contains(phrase) }?.first
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")

        fun normalize(text: String): String =
            Normalizer
                .normalize(text, Normalizer.Form.NFKC)
                .lowercase(Locale.ROOT)
                .replace(WHITESPACE, "")
    }
}
