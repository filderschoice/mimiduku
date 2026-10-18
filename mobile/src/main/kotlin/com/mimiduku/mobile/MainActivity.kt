package com.mimiduku.mobile

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mimiduku.mobile.pin.PinManager
import com.mimiduku.mobile.service.MimidukuAccessibilityService
import com.mimiduku.mobile.settings.LogScreen
import com.mimiduku.mobile.settings.PinEntryDialog
import com.mimiduku.mobile.settings.PinSetupDialog
import com.mimiduku.mobile.settings.SettingsController
import com.mimiduku.mobile.settings.SettingsDialog
import com.mimiduku.mobile.settings.SettingsPlatform
import com.mimiduku.mobile.settings.SettingsScreen
import com.mimiduku.mobile.settings.SettingsStores
import com.mimiduku.mobile.settings.SetupStatus
import com.mimiduku.mobile.storage.SharedPreferencesStringStore

/** 保護者・利用者向けの設定画面。状態と操作は [SettingsController] が持つ。 */
class MainActivity :
    ComponentActivity(),
    SettingsPlatform {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stores =
            SettingsStores(
                modeStore = MimidukuAccessibilityService.usageModeStore(this),
                detectionLog = MimidukuAccessibilityService.detectionLog(this),
                pinManager = PinManager(SharedPreferencesStringStore.of(this)),
                pauseStore = MimidukuAccessibilityService.pauseStore(this),
                badgeSettings = MimidukuAccessibilityService.badgeSettings(this),
                consentStore = MimidukuAccessibilityService.consentStore(this),
            )
        setContent {
            val controller =
                remember {
                    SettingsController(stores, this, features = MimidukuAccessibilityService.FEATURES)
                }
            // 設定画面から戻ったときに状態を読み直す。
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { controller.refresh() }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val logRows = controller.logRows
                    if (logRows == null) {
                        SettingsScreen(state = controller.state, actions = controller, consentActions = controller)
                    } else {
                        LogScreen(rows = logRows, actions = controller)
                    }
                    SettingsDialogHost(controller)
                }
            }
        }
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

    override fun openAppDetails() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

@Composable
private fun SettingsDialogHost(controller: SettingsController) {
    when (controller.dialog) {
        is SettingsDialog.Entry ->
            PinEntryDialog(
                onVerify = controller::verifyPin,
                onSuccess = controller::onPinVerified,
                onDismiss = controller::dismissDialog,
            )
        SettingsDialog.Setup -> PinSetupDialog(onSave = controller::savePin, onDismiss = controller::dismissDialog)
        null -> Unit
    }
}
