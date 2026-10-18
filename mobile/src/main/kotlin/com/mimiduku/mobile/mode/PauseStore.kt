package com.mimiduku.mobile.mode

import com.mimiduku.mobile.storage.StringStore

/**
 * 保護の一時停止。停止中はサービスが検知も介入もしない。
 *
 * 再起動をまたいで保つため端末の時刻で期限を持つ。時刻が停止を始めた時点より前へ戻された場合は、
 * 期限を延ばす操作とみなして停止を終える（保護を弱める方向に倒さない）。
 */
class PauseStore(
    private val store: StringStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun pause(durationMillis: Long) {
        require(durationMillis > 0) { "durationMillis must be positive" }
        val now = clock()
        store.putString(KEY_STARTED_AT, now.toString())
        store.putString(KEY_UNTIL, (now + durationMillis).toString())
    }

    fun resume() {
        store.remove(KEY_STARTED_AT)
        store.remove(KEY_UNTIL)
    }

    /** 停止中なら残りの時間、そうでなければ 0。期限切れ・時刻の巻き戻し・壊れた保存値では停止を終える。 */
    fun remainingMillis(): Long {
        val rawStartedAt = store.getString(KEY_STARTED_AT)
        val rawUntil = store.getString(KEY_UNTIL)
        val startedAt = rawStartedAt?.toLongOrNull()
        val until = rawUntil?.toLongOrNull()
        val now = clock()
        val active = startedAt != null && until != null && now >= startedAt && now < until
        if (!active) {
            if (rawStartedAt != null || rawUntil != null) resume()
            return 0
        }
        return until!! - now
    }

    fun isPaused(): Boolean = remainingMillis() > 0

    companion object {
        /** 設定画面で選べる停止時間。 */
        val DURATIONS_MINUTES = listOf(15, 30, 60)

        private const val KEY_STARTED_AT = "pause_started_at"
        private const val KEY_UNTIL = "pause_until"
    }
}
