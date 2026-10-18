package com.mimiduku.mobile.pin

import com.mimiduku.mobile.storage.StringStore

/** PIN の照合結果。 */
sealed interface PinCheck {
    data object Success : PinCheck

    /** 間違い。[attemptsBeforeLock] はロックまでに残る回数（ロック中の再失敗では 0）。 */
    data class Wrong(
        val attemptsBeforeLock: Int,
    ) : PinCheck

    /** 連続失敗で待ち時間中。照合は行わない。 */
    data class Locked(
        val retryAfterMillis: Long,
    ) : PinCheck

    data object NotSet : PinCheck

    /** 保存値が壊れていて照合できない。復旧はアプリの再インストール（データ消去）。 */
    data object Corrupted : PinCheck
}

/**
 * 保護者 PIN（4〜8桁の数字）の設定と照合。
 *
 * [MAX_FAILURES_BEFORE_LOCK] 回続けて間違えると [FIRST_LOCK_MILLIS] 待たせ、以降は間違えるたびに待ち時間を倍にする
 * （上限 [MAX_LOCK_MILLIS]）。成功すると失敗回数を戻す。保存値が壊れた場合は「PIN なし」とみなさず照合できない状態にする
 * （PIN の保護を外す方向に倒さないため）。
 */
class PinManager(
    private val store: StringStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val hasher: PinHasher = PinHasher(),
) {
    fun isSet(): Boolean = store.getString(KEY_HASH) != null

    /** PIN を設定する（既存の PIN は置き換える）。形式が合わなければ false。 */
    fun set(pin: String): Boolean {
        if (!isValidFormat(pin)) return false
        store.putString(KEY_HASH, hasher.hash(pin))
        resetFailures()
        return true
    }

    fun clear() {
        store.remove(KEY_HASH)
        resetFailures()
    }

    /** ロック中なら残りの待ち時間、そうでなければ 0。 */
    fun lockRemainingMillis(): Long {
        val lockedUntil = store.getString(KEY_LOCKED_UNTIL)?.toLongOrNull() ?: return 0
        return (lockedUntil - clock()).coerceAtLeast(0)
    }

    fun verify(pin: String): PinCheck {
        val stored = store.getString(KEY_HASH) ?: return PinCheck.NotSet
        val remaining = lockRemainingMillis()
        return when {
            remaining > 0 -> PinCheck.Locked(remaining)
            else ->
                when (hasher.matches(pin, stored)) {
                    true -> PinCheck.Success.also { resetFailures() }
                    false -> recordFailure()
                    null -> PinCheck.Corrupted
                }
        }
    }

    private fun recordFailure(): PinCheck {
        val failures = (store.getString(KEY_FAILURES)?.toIntOrNull() ?: 0) + 1
        store.putString(KEY_FAILURES, failures.toString())
        val lockMillis = lockMillisFor(failures)
        if (lockMillis > 0) store.putString(KEY_LOCKED_UNTIL, (clock() + lockMillis).toString())
        return PinCheck.Wrong(attemptsBeforeLock = (MAX_FAILURES_BEFORE_LOCK - failures).coerceAtLeast(0))
    }

    private fun resetFailures() {
        store.remove(KEY_FAILURES)
        store.remove(KEY_LOCKED_UNTIL)
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        const val MAX_FAILURES_BEFORE_LOCK = 5
        const val FIRST_LOCK_MILLIS = 30_000L
        const val MAX_LOCK_MILLIS = 30 * 60_000L

        private const val KEY_HASH = "pin_hash"
        private const val KEY_FAILURES = "pin_failures"
        private const val KEY_LOCKED_UNTIL = "pin_locked_until"

        fun isValidFormat(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

        /** [failures] 回目の失敗の後に待たせる時間。 */
        fun lockMillisFor(failures: Int): Long {
            if (failures < MAX_FAILURES_BEFORE_LOCK) return 0
            val doublings = (failures - MAX_FAILURES_BEFORE_LOCK).coerceAtMost(MAX_DOUBLINGS)
            return (FIRST_LOCK_MILLIS shl doublings).coerceAtMost(MAX_LOCK_MILLIS)
        }

        /** 30秒を6回倍にすると32分で上限を超えるため、それ以上の計算は不要。 */
        private const val MAX_DOUBLINGS = 6
    }
}
