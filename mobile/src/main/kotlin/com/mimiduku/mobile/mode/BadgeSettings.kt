package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.StringStore

/** みみづくバッジを画面に出すかの設定。既定は出す。 */
class BadgeSettings(
    private val store: StringStore,
) {
    fun isVisible(): Boolean = store.getString(KEY) != HIDDEN

    fun setVisible(visible: Boolean) {
        if (visible) store.remove(KEY) else store.putString(KEY, HIDDEN)
    }

    private companion object {
        const val KEY = "badge_visible"
        const val HIDDEN = "false"
    }
}
