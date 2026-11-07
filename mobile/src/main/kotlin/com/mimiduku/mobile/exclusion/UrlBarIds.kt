package com.mimiduku.mobile.exclusion

/**
 * ブラウザのアドレスバーのビューを見分ける。ビュー ID（`パッケージ名:id/名前`）の名前で判定する。
 *
 * ブラウザごとに名前が違い、アプリの更新で変わりうるため、分かっているものだけを対象にする。
 * 見つからなければ URL は分からないものとして扱い、確認を省かない（保護を弱める方向に倒さない）。
 * 別のアプリのウィンドウを読み違えないよう、ID のパッケージ名がブラウザ自身のものであることも確かめる。
 */
object UrlBarIds {
    private val names =
        setOf(
            // Chrome・Edge・Brave・Vivaldi など Chromium 系
            "url_bar",
            // Firefox
            "mozac_browser_toolbar_url_view",
            "url_bar_title",
            // Samsung Internet
            "location_bar_edit_text",
        )

    fun isUrlBar(
        viewId: String?,
        browserPackage: String,
    ): Boolean {
        val id = viewId ?: return false
        val prefix = "$browserPackage:id/"
        return id.startsWith(prefix) && id.removePrefix(prefix) in names
    }
}
