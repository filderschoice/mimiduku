package com.mimiduku.mobile

import android.content.Intent
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mimiduku.mobile.pin.PinManager
import com.mimiduku.mobile.service.MimidukuAccessibilityService
import com.mimiduku.mobile.settings.GuardedAction
import com.mimiduku.mobile.settings.PinEntryDialog
import com.mimiduku.mobile.settings.PinPolicy
import com.mimiduku.mobile.settings.PinSetupDialog
import com.mimiduku.mobile.settings.SettingsActions
import com.mimiduku.mobile.settings.SettingsScreen
import com.mimiduku.mobile.settings.SettingsState
import com.mimiduku.mobile.settings.SetupStatus
import com.mimiduku.mobile.storage.SharedPreferencesStringStore

/** 表示中のダイアログ。 */
private sealed interface SettingsDialog {
    /** PIN を求め、成功したら [onSuccess] を行う。 */
    class Entry(
        val onSuccess: () -> Unit,
    ) : SettingsDialog

    data object Setup : SettingsDialog
}

/**
 * 保護者・利用者向けの設定画面。保護を弱める操作は、PIN を設定している場合に PIN の入力を求める（[PinPolicy]）。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modeStore = MimidukuAccessibilityService.usageModeStore(this)
        val detectionLog = MimidukuAccessibilityService.detectionLog(this)
        val pinManager = PinManager(SharedPreferencesStringStore.of(this))
        setContent {
            var serviceEnabled by remember { mutableStateOf(isServiceEnabled()) }
            var logCount by remember { mutableIntStateOf(0) }
            var mode by remember { mutableStateOf(modeStore.load()) }
            var pinSet by remember { mutableStateOf(pinManager.isSet()) }
            var dialog by remember { mutableStateOf<SettingsDialog?>(null) }
            // 設定画面から戻ったときに状態を読み直す。
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isServiceEnabled()
                logCount = detectionLog.entries().size
            }

            fun guarded(
                action: GuardedAction,
                block: () -> Unit,
            ) {
                if (PinPolicy.requiresPin(action, pinManager.isSet())) dialog = SettingsDialog.Entry(block) else block()
            }

            val actions =
                SettingsActions(
                    openAccessibilitySettings = ::openAccessibilitySettings,
                    openAppDetails = ::openAppDetails,
                    selectMode = { selected ->
                        PinPolicy.forModeChange(mode, selected)?.let { action ->
                            guarded(action) {
                                modeStore.save(selected)
                                mode = selected
                            }
                        }
                    },
                    setPin = { dialog = SettingsDialog.Setup },
                    changePin = { guarded(GuardedAction.CHANGE_PIN) { dialog = SettingsDialog.Setup } },
                    removePin = {
                        guarded(GuardedAction.REMOVE_PIN) {
                            pinManager.clear()
                            pinSet = false
                        }
                    },
                )
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        state =
                            SettingsState(
                                serviceEnabled = serviceEnabled,
                                showRestrictedGuide = SetupStatus.needsRestrictedSettingsGuide(Build.VERSION.SDK_INT),
                                mode = mode,
                                logCount = logCount,
                                pinSet = pinSet,
                            ),
                        actions = actions,
                    )
                    SettingsDialogHost(
                        dialog = dialog,
                        pinManager = pinManager,
                        onDialogChange = { dialog = it },
                        onPinSaved = { pinSet = true },
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsDialogHost(
        dialog: SettingsDialog?,
        pinManager: PinManager,
        onDialogChange: (SettingsDialog?) -> Unit,
        onPinSaved: () -> Unit,
    ) {
        when (dialog) {
            is SettingsDialog.Entry ->
                PinEntryDialog(
                    onVerify = pinManager::verify,
                    onSuccess = {
                        // onSuccess の中で次のダイアログ（PIN の変更）を開く場合があるため、先に閉じる。
                        onDialogChange(null)
                        dialog.onSuccess()
                    },
                    onDismiss = { onDialogChange(null) },
                )
            SettingsDialog.Setup ->
                PinSetupDialog(
                    onSave = { pin ->
                        pinManager.set(pin).also { saved ->
                            if (saved) {
                                onPinSaved()
                                onDialogChange(null)
                            }
                        }
                    },
                    onDismiss = { onDialogChange(null) },
                )
            null -> Unit
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openAppDetails() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun isServiceEnabled(): Boolean =
        SetupStatus.isServiceEnabled(
            enabledServices =
                Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            packageName = packageName,
            className = MimidukuAccessibilityService::class.java.name,
        )
}
