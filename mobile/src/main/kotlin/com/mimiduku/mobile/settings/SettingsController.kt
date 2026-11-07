package com.mimiduku.mobile.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mimiduku.mobile.exclusion.ExclusionCandidate
import com.mimiduku.mobile.exclusion.ExclusionStore
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.log.DetectionLog
import com.mimiduku.mobile.mode.BadgeSettings
import com.mimiduku.mobile.mode.ConsentStore
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.mode.UsageModeStore
import com.mimiduku.mobile.pin.PinCheck
import com.mimiduku.mobile.pin.PinManager
import java.time.ZoneId

/** 設定画面が読み書きする保存先。 */
class SettingsStores(
    val modeStore: UsageModeStore,
    val detectionLog: DetectionLog,
    val pinManager: PinManager,
    val overrides: ProtectionOverrides,
    val badgeSettings: BadgeSettings,
    val consentStore: ConsentStore,
)

/** 保護を一時的に止める・確認を省くといった、保護の例外にあたる保存先。 */
class ProtectionOverrides(
    val pauseStore: PauseStore,
    val exclusionStore: ExclusionStore,
)

/** 端末側の操作（Activity が実装する）。 */
interface SettingsPlatform {
    fun isServiceEnabled(): Boolean

    fun showRestrictedGuide(): Boolean

    fun openAccessibilitySettings()

    fun openAppDetails()

    /** サポート（GitHub のリポジトリ）をブラウザで開く。 */
    fun openSupportPage()

    /** アプリ名。パッケージ可視性などで分からなければ null。 */
    fun appLabel(packageName: String): String?

    /** 通知の権限を求める（Android 13 以降。許可済み・不要な版では何もしない）。 */
    fun requestNotificationPermission()
}

/** アプリ内で開く画面（検知ログは [SettingsController.logRows] で別に持つ）。 */
enum class Screen { HOME, SETTINGS, ABOUT }

/** 表示中のダイアログ。 */
sealed interface SettingsDialog {
    /** PIN を求め、成功したら [onSuccess] を行う。 */
    class Entry(
        val onSuccess: () -> Unit,
    ) : SettingsDialog

    data object Setup : SettingsDialog

    /** 確認を省く対象の追加。[candidate] のうち登録するものを選ばせる。 */
    class AddExclusion(
        val candidate: ExclusionCandidate,
        val appLabel: String?,
    ) : SettingsDialog
}

/**
 * 設定画面の状態と操作。保護を弱める操作は、PIN を設定している場合に PIN の入力を求める（[PinPolicy]）。
 *
 * Compose の状態（[state]・[dialog]）を持つが Android には依存しないため、単体テストで操作の流れを確かめられる。
 */
class SettingsController(
    private val stores: SettingsStores,
    private val platform: SettingsPlatform,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val features: FeatureSet = FeatureSet.GITHUB,
    initialScreen: Screen = Screen.HOME,
) : SettingsActions,
    LogActions,
    ExclusionActions,
    ConsentActions,
    NavigationActions {
    var state by mutableStateOf(load())
        private set
    var dialog by mutableStateOf<SettingsDialog?>(null)
        private set

    var screen by mutableStateOf(initialScreen)
        private set

    /** ログ閲覧画面に出す行。閲覧画面を閉じているときは null。 */
    var logRows by mutableStateOf<List<LogRow>?>(null)
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

    /** バナーの「今後は確認しない」から開いたとき。追加するアプリ・サイトを選ぶダイアログを出す。 */
    fun proposeExclusion(candidate: ExclusionCandidate) {
        screen = Screen.SETTINGS
        dialog = SettingsDialog.AddExclusion(candidate, platform.appLabel(candidate.sourcePackage))
    }

    /**
     * 選んだものを登録する。PIN を設定していなければ登録せず PIN の設定を求める（確認を省く対象は PIN で守る）。
     * 登録できるのは遷移元のアプリと、読めたドメインだけ。
     */
    fun confirmAddExclusion(
        addPackage: Boolean,
        addDomainChecked: Boolean,
    ) {
        val proposal = dialog as? SettingsDialog.AddExclusion ?: return
        // 読めなかったドメインは登録できない（選ばれていても無視する）。
        val addDomain = addDomainChecked && proposal.candidate.domain != null
        when {
            !addPackage && !addDomain -> dialog = null
            !stores.pinManager.isSet() -> dialog = SettingsDialog.Setup
            else ->
                guarded(GuardedAction.ADD_EXCLUSION) {
                    if (addPackage) stores.overrides.exclusionStore.addPackage(proposal.candidate.sourcePackage)
                    proposal.candidate.domain?.takeIf { addDomain }?.let(stores.overrides.exclusionStore::addDomain)
                    refresh()
                }
        }
    }

    override fun removeExcludedPackage(packageName: String) {
        stores.overrides.exclusionStore.removePackage(packageName)
        refresh()
    }

    override fun clearExcludedDomains() {
        stores.overrides.exclusionStore.clearDomains()
        refresh()
    }

    override fun openAccessibilitySettings() = platform.openAccessibilitySettings()

    override fun openAppDetails() = platform.openAppDetails()

    override fun openSupportPage() = platform.openSupportPage()

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
            stores.overrides.pauseStore.pause(minutes * MILLIS_PER_MINUTE)
            refresh()
        }

    override fun resume() =
        guarded(GuardedAction.RESUME_PROTECTION) {
            stores.overrides.pauseStore.resume()
            refresh()
        }

    override fun setBadgeVisible(visible: Boolean) {
        stores.badgeSettings.setVisible(visible)
        refresh()
    }

    override fun setBadgeNotification(enabled: Boolean) {
        stores.badgeSettings.setNotificationEnabled(enabled)
        if (enabled) platform.requestNotificationPermission()
        refresh()
    }

    override fun acceptDisclosure() {
        stores.consentStore.accept()
        refresh()
    }

    override fun openSettings() {
        screen = Screen.SETTINGS
    }

    override fun openAbout() {
        screen = Screen.ABOUT
    }

    /** 設定・このアプリについてからホームへ戻る。 */
    override fun navigateUp() {
        screen = Screen.HOME
        refresh()
    }

    override fun openLog() {
        logRows = LogRows.from(stores.detectionLog.entries(), zone(), platform::appLabel)
    }

    override fun closeLog() {
        logRows = null
        refresh()
    }

    override fun clearLog() =
        guarded(GuardedAction.CLEAR_LOG) {
            stores.detectionLog.clear()
            openLog()
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
            pauseRemainingMinutes = toMinutesRoundedUp(stores.overrides.pauseStore.remainingMillis()),
            badgeVisible = stores.badgeSettings.isVisible(),
            badgeNotification = stores.badgeSettings.isNotificationEnabled(),
            features = features,
            consented = stores.consentStore.hasConsented(),
            excludedPackages =
                stores.overrides.exclusionStore.packages().map { ExcludedApp(it, platform.appLabel(it) ?: it) },
            excludedDomainCount = stores.overrides.exclusionStore.domainCount(),
        )

    companion object {
        const val MILLIS_PER_MINUTE = 60_000L

        fun toMinutesRoundedUp(millis: Long): Int = ((millis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt()
    }
}
