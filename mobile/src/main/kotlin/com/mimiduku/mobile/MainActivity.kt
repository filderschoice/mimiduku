package com.mimiduku.mobile

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mimiduku.mobile.exclusion.ExclusionCandidate
import com.mimiduku.mobile.pin.PinManager
import com.mimiduku.mobile.service.MimidukuAccessibilityService
import com.mimiduku.mobile.settings.AboutScreen
import com.mimiduku.mobile.settings.AddExclusionDialog
import com.mimiduku.mobile.settings.ExcludedDomainsDialog
import com.mimiduku.mobile.settings.HomeScreen
import com.mimiduku.mobile.settings.LogScreen
import com.mimiduku.mobile.settings.PinEntryDialog
import com.mimiduku.mobile.settings.PinSetupDialog
import com.mimiduku.mobile.settings.ProtectionOverrides
import com.mimiduku.mobile.settings.Screen
import com.mimiduku.mobile.settings.SettingsController
import com.mimiduku.mobile.settings.SettingsDialog
import com.mimiduku.mobile.settings.SettingsPlatform
import com.mimiduku.mobile.settings.SettingsScreen
import com.mimiduku.mobile.settings.SettingsStores
import com.mimiduku.mobile.settings.SetupStatus
import com.mimiduku.mobile.storage.SharedPreferencesStringStore
import com.mimiduku.mobile.ui.MimidukuTheme

/** サポートの窓口（GitHub のリポジトリ）。両方の版で同じ。 */
private const val SUPPORT_URL = "https://github.com/filderschoice/mimiduku"

/** 保護者・利用者向けの設定画面。状態と操作は [SettingsController] が持つ。 */
class MainActivity :
    ComponentActivity(),
    SettingsPlatform {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    /** バナーの「今後は確認しない」から渡された、登録の候補。画面が受け取ったら null に戻す。 */
    private var exclusionCandidate by mutableStateOf<ExclusionCandidate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) exclusionCandidate = candidateFrom(intent)
        val stores =
            SettingsStores(
                modeStore = MimidukuAccessibilityService.usageModeStore(this),
                detectionLog = MimidukuAccessibilityService.detectionLog(this),
                pinManager = PinManager(SharedPreferencesStringStore.of(this)),
                overrides =
                    ProtectionOverrides(
                        pauseStore = MimidukuAccessibilityService.pauseStore(this),
                        exclusionStore = MimidukuAccessibilityService.exclusionStore(this),
                    ),
                badgeSettings = MimidukuAccessibilityService.badgeSettings(this),
                consentStore = MimidukuAccessibilityService.consentStore(this),
            )
        setContent {
            // 画面の回転でアクティビティが作り直されても、開いていた画面とログの表示を保つ。
            var savedScreen by rememberSaveable { mutableStateOf(Screen.HOME.name) }
            var savedLogOpen by rememberSaveable { mutableStateOf(false) }
            val controller =
                remember {
                    SettingsController(
                        stores,
                        this,
                        features = MimidukuAccessibilityService.FEATURES,
                        initialScreen = Screen.valueOf(savedScreen),
                    ).also { if (savedLogOpen) it.openLog() }
                }
            savedScreen = controller.screen.name
            savedLogOpen = controller.logRows != null
            LaunchedEffect(exclusionCandidate) {
                exclusionCandidate?.let {
                    controller.proposeExclusion(it)
                    exclusionCandidate = null
                }
            }
            // 設定画面から戻ったときに状態を読み直す。
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.refresh() }
            MimidukuTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val logRows = controller.logRows
                    val state = controller.state
                    when {
                        logRows != null -> LogScreen(rows = logRows, actions = controller)
                        controller.screen == Screen.SETTINGS ->
                            SettingsScreen(
                                state = state,
                                actions = controller,
                                navigation = controller,
                                exclusions = controller,
                            )
                        controller.screen == Screen.ABOUT -> AboutScreen(state = state, navigation = controller)
                        else -> HomeScreen(state, controller, controller, controller)
                    }
                    SettingsDialogHost(controller)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        candidateFrom(intent)?.let { exclusionCandidate = it }
    }

    private fun candidateFrom(intent: Intent?): ExclusionCandidate? {
        val source = intent?.getStringExtra(EXTRA_EXCLUDE_PACKAGE) ?: return null
        return ExclusionCandidate(source, intent.getStringExtra(EXTRA_EXCLUDE_DOMAIN))
    }

    override fun isServiceEnabled(): Boolean =
        SetupStatus.isServiceEnabled(
            enabledServices =
                Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName = packageName,
            className = MimidukuAccessibilityService::class.java.name,
        )

    override fun showRestrictedGuide(): Boolean = SetupStatus.needsRestrictedSettingsGuide(Build.VERSION.SDK_INT)

    override fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    override fun appLabel(packageName: String): String? =
        runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.onFailure { if (it !is PackageManager.NameNotFoundException) throw it }
            .getOrNull()

    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun openSupportPage() {
        // ブラウザが無い端末では何もしない（アプリの動作には影響しない）。
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SUPPORT_URL)))
        } catch (_: ActivityNotFoundException) {
            // 開く先が無いだけなので、通知もしない。
        }
    }

    override fun openAppDetails() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    companion object {
        private const val EXTRA_EXCLUDE_PACKAGE = "com.mimiduku.mobile.EXCLUDE_PACKAGE"
        private const val EXTRA_EXCLUDE_DOMAIN = "com.mimiduku.mobile.EXCLUDE_DOMAIN"

        /** バナーの「今後は確認しない」から本体の設定画面を開く Intent（明示的で、本アプリの中だけに届く）。 */
        fun exclusionIntent(
            context: Context,
            candidate: ExclusionCandidate,
        ): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_EXCLUDE_PACKAGE, candidate.sourcePackage)
                .putExtra(EXTRA_EXCLUDE_DOMAIN, candidate.domain)
    }
}

@Composable
private fun SettingsDialogHost(controller: SettingsController) {
    when (val dialog = controller.dialog) {
        is SettingsDialog.Entry ->
            PinEntryDialog(
                onVerify = controller::verifyPin,
                onSuccess = controller::onPinVerified,
                onDismiss = controller::dismissDialog,
            )
        SettingsDialog.Setup -> PinSetupDialog(onSave = controller::savePin, onDismiss = controller::dismissDialog)
        is SettingsDialog.AddExclusion ->
            AddExclusionDialog(
                dialog = dialog,
                onConfirm = controller::confirmAddExclusion,
                onDismiss = controller::dismissDialog,
            )
        is SettingsDialog.DomainList ->
            ExcludedDomainsDialog(
                dialog = dialog,
                onRemove = controller::removeExcludedDomain,
                onDismiss = controller::dismissDialog,
            )
        null -> Unit
    }
}
