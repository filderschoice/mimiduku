package com.mimiduku.mobile.exclusion

/**
 * ブラウザのアドレスバーの文字から、除外の照合に使うドメイン（ホスト名）を取り出す。
 *
 * 保存・照合するのはホスト名だけで、パス・クエリ・ポート・認証情報は捨てる。`www.` は同じサイトとして扱うため外す。
 * サブドメインは別のサイトとして扱う（広く許可しすぎないため）。ホスト名として読めない文字（検索語など）は null。
 */
object DomainNormalizer {
    private const val MAX_LENGTH = 253
    private val scheme = Regex("^[a-z][a-z0-9+.-]*://")
    private val port = Regex(":\\d{1,5}$")
    private const val CHAR = "a-z0-9\u0080-\uffff"
    private val host = Regex("^[$CHAR](?:[$CHAR\\-]*[$CHAR])?(?:\\.[$CHAR\\-]+)+$")

    fun fromUrlText(text: String?): String? {
        val value = text?.trim()?.lowercase()?.takeIf { it.isNotEmpty() && it.none(Char::isWhitespace) } ?: return null
        return hostPart(value).takeIf { it.length <= MAX_LENGTH && host.matches(it) }
    }

    private fun hostPart(value: String): String =
        value
            .replace(scheme, "")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            // 「good.com@evil.com」のような書き方では、実際に開かれるのは @ より後ろ。
            .substringAfterLast('@')
            .replace(port, "")
            .trimEnd('.')
            .removePrefix("www.")
}
