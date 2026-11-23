package com.mimiduku.mobile.overlay

import com.mimiduku.mobile.detect.AdScreenState
import com.mimiduku.mobile.mode.UsageMode

/**
 * 広告画面のタップシールドを張るかを決める。
 *
 * 張るのは、こどもモードで全画面の広告（広告 SDK の Activity と判定できたもの）のときだけ。アプリ内のバナー広告は
 * ノードの位置で覆う必要があり、領域を誤ると子供が操作できなくなるおそれがあるため張らない（2026-11-23 の実機調査で見送りと決めた。BL-025）。
 * 判定の誤りで子供が操作できなくならないよう、同じ広告画面で張り続けるのは [maxMillis] までにする。
 * 時間切れの後は、その広告画面が終わるまで張り直さない。
 */
class ShieldTimer(
    private val maxMillis: Long = MAX_SHIELD_MILLIS,
) {
    private var since: Long? = null

    fun shouldShield(
        state: AdScreenState?,
        nowMillis: Long,
    ): Boolean {
        val wanted = state != null && state.fullScreen && state.mode == UsageMode.CHILD
        if (!wanted) {
            since = null
            return false
        }
        val start = since ?: nowMillis.also { since = it }
        return nowMillis - start < maxMillis
    }

    companion object {
        const val MAX_SHIELD_MILLIS = 60_000L
    }
}
