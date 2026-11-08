package com.mimiduku.mobile.exclusion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlBarIdsTest {
    @Test
    fun `known url bar ids of the browser itself match`() {
        assertTrue(UrlBarIds.isUrlBar("com.android.chrome:id/url_bar", "com.android.chrome"))
        assertTrue(UrlBarIds.isUrlBar("org.mozilla.firefox:id/mozac_browser_toolbar_url_view", "org.mozilla.firefox"))
        assertTrue(
            UrlBarIds.isUrlBar(
                "com.sec.android.app.sbrowser:id/location_bar_edit_text",
                "com.sec.android.app.sbrowser",
            ),
        )
    }

    @Test
    fun `other views and ids from another package do not match`() {
        assertFalse(UrlBarIds.isUrlBar(null, "com.android.chrome"))
        assertFalse(UrlBarIds.isUrlBar("com.android.chrome:id/title", "com.android.chrome"))
        assertFalse(UrlBarIds.isUrlBar("com.example.fake:id/url_bar", "com.android.chrome"))
    }
}
