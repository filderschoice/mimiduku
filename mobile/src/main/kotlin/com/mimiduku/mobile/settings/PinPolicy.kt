package com.mimiduku.mobile.settings

import androidx.annotation.StringRes
import com.mimiduku.mobile.R
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.pin.PinCheck

/** 設定画面の操作のうち、PIN で守るかを判定する対象。 */
enum class GuardedAction {
    SWITCH_TO_ADULT,
    SWITCH_TO_CHILD,
    CHANGE_PIN,
    REMOVE_PIN,
    PAUSE_PROTECTION,
    RESUME_PROTECTION,
    CLEAR_LOG,

    /** 確認を省く対象（アプリ・サイト）の追加。PIN を設定していないと追加できない（[SettingsController.confirmAddExclusion]）。 */
    ADD_EXCLUSION,
}

/**
 * どの操作で PIN を求めるか。
 *
 * 保護を弱める操作（おとなモードへの切替・PIN の変更と削除・一時停止・ログの消去・確認を省く対象の追加）だけを守る。
 * 確認を省く対象の削除は保護を強めるため、PIN なしで行える。
 * こどもモードへの切替と保護の再開は保護を強めるため、PIN なしで行える。
 */
object PinPolicy {
    private val guarded =
        setOf(
            GuardedAction.SWITCH_TO_ADULT,
            GuardedAction.CHANGE_PIN,
            GuardedAction.REMOVE_PIN,
            GuardedAction.PAUSE_PROTECTION,
            GuardedAction.CLEAR_LOG,
            GuardedAction.ADD_EXCLUSION,
        )

    fun requiresPin(
        action: GuardedAction,
        pinSet: Boolean,
    ): Boolean = pinSet && action in guarded

    /** モードの切替を表す操作。同じモードなら null（何もしない）。 */
    fun forModeChange(
        from: UsageMode,
        to: UsageMode,
    ): GuardedAction? =
        when {
            from == to -> null
            to == UsageMode.ADULT -> GuardedAction.SWITCH_TO_ADULT
            else -> GuardedAction.SWITCH_TO_CHILD
        }

    /** 照合結果を画面に出す文言。成功のときは null。 */
    @StringRes
    fun messageFor(check: PinCheck): Int? =
        when (check) {
            PinCheck.Success -> null
            is PinCheck.Wrong ->
                if (check.attemptsBeforeLock > 0) R.string.pin_error_wrong else R.string.pin_error_locked_now
            is PinCheck.Locked -> R.string.pin_error_locked
            PinCheck.NotSet -> R.string.pin_error_not_set
            PinCheck.Corrupted -> R.string.pin_error_corrupted
        }
}
