package com.mimiduku.mobile.mode

import com.mimiduku.mobile.overlay.BadgePlacement
import com.mimiduku.mobile.storage.StringStore

/** みみづくバッジの設定。出すかどうか（既定は出す）と、画面上の置き場所。 */
class BadgeSettings(
    private val store: StringStore,
) {
    fun isVisible(): Boolean = store.getString(KEY) != HIDDEN

    fun setVisible(visible: Boolean) {
        if (visible) store.remove(KEY) else store.putString(KEY, HIDDEN)
    }

    /** 通知にバッジの操作を出すか。既定は出す。 */
    fun isNotificationEnabled(): Boolean = store.getString(NOTIFICATION_KEY) != OFF

    fun setNotificationEnabled(enabled: Boolean) {
        if (enabled) store.remove(NOTIFICATION_KEY) else store.putString(NOTIFICATION_KEY, OFF)
    }

    /** バッジの置き場所。未保存なら右端の中央。 */
    fun placement(): BadgePlacement = BadgePlacement.decode(store.getString(PLACEMENT_KEY))

    fun setPlacement(placement: BadgePlacement) {
        if (placement == BadgePlacement.DEFAULT) {
            store.remove(PLACEMENT_KEY)
        } else {
            store.putString(PLACEMENT_KEY, placement.encode())
        }
    }

    private companion object {
        const val KEY = "badge_visible"
        const val HIDDEN = "false"
        const val NOTIFICATION_KEY = "badge_notification"
        const val OFF = "false"
        const val PLACEMENT_KEY = "badge_placement"
    }
}
