package com.mimiduku.mobile.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mimiduku.mobile.log.DetectionLog
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.mode.UsageModeStore
import com.mimiduku.mobile.pin.PinCheck
import com.mimiduku.mobile.pin.PinManager

/** 設定画面が読み書きする保存先。 */
class SettingsStores(
    val modeStore: UsageModeStore,
    val detectionLog: DetectionLog,
    val pinManager: PinManager,
    val pauseStore: PauseStore,
)

/** 端末側の操作（Activity が実装する）。 */
interface SettingsPlatform {
    fun isServiceEnabled(): Boolean

    fun showRestrictedGuide(): Boolean

    fun openAccessibilitySettings()

    fun openAppDetails()
}

/** 表示中のダイアログ。 */
sealed interface SettingsDialog {
    /** PIN を求め、成功したら [onSuccess] を行う。 */
    class Entry(
        val onSuccess: () -> Unit,
    ) : SettingsDialog

    data object Setup : SettingsDialog
}

/**
 * 設定画面の状態と操作。保護を弱める操作は、PIN を設定している場合に PIN の入力を求める（[PinPolicy]）。
 *
 * Compose の状態（[state]・[dialog]）を持つが Android には依存しないため、単体テストで操作の流れを確かめられる。
 */
class SettingsController(
    private val stores: SettingsStores,
    private val platform: SettingsPlatform,
) : SettingsActions {
    var state by mutableStateOf(load())
        private set
    var dialog by mutableStateOf<SettingsDialog?>(null)
        private set

    /** 画面へ戻ったときなどに、保存先と端末の状態を読み直す。 */
    fun refresh() {
        state = load()
    }

    fun verifyPin(pin: String): PinCheck = stores.pinManager.verify(pin)

    fun onPinVerified() {
        val entry = dialog as? SettingsDialog.Entry ?: return
        // onSuccess の中で次のダイアログ（PIN の変更）を開く場合があるため、先に閉じる。
        dialog = null
        entry.onSuccess()
    }

    /** 新しい PIN を保存する。形式が合わなければ false。 */
    fun savePin(pin: String): Boolean {
        val saved = stores.pinManager.set(pin)
        if (saved) {
            dialog = null
            refresh()
        }
        return saved
    }

    fun dismissDialog() {
        dialog = null
    }

    override fun openAccessibilitySettings() = platform.openAccessibilitySettings()

    override fun openAppDetails() = platform.openAppDetails()

    override fun selectMode(mode: UsageMode) {
        val action = PinPolicy.forModeChange(state.mode, mode) ?: return
        guarded(action) {
            stores.modeStore.save(mode)
            refresh()
        }
    }

    override fun setPin() {
        dialog = SettingsDialog.Setup
    }

    override fun changePin() = guarded(GuardedAction.CHANGE_PIN) { dialog = SettingsDialog.Setup }

    override fun removePin() =
        guarded(GuardedAction.REMOVE_PIN) {
            stores.pinManager.clear()
            refresh()
        }

    override fun pause(minutes: Int) =
        guarded(GuardedAction.PAUSE_PROTECTION) {
            stores.pauseStore.pause(minutes * MILLIS_PER_MINUTE)
            refresh()
        }

    override fun resume() =
        guarded(GuardedAction.RESUME_PROTECTION) {
            stores.pauseStore.resume()
            refresh()
        }

    private fun guarded(
        action: GuardedAction,
        block: () -> Unit,
    ) {
        if (PinPolicy.requiresPin(action, stores.pinManager.isSet())) dialog = SettingsDialog.Entry(block) else block()
    }

    private fun load() =
        SettingsState(
            serviceEnabled = platform.isServiceEnabled(),
            showRestrictedGuide = platform.showRestrictedGuide(),
            mode = stores.modeStore.load(),
            logCount = stores.detectionLog.entries().size,
            pinSet = stores.pinManager.isSet(),
            pauseRemainingMinutes = toMinutesRoundedUp(stores.pauseStore.remainingMillis()),
        )

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L

        fun toMinutesRoundedUp(millis: Long): Int = ((millis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
    }
}
