package com.mimiduku.mobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mimiduku.mobile.mode.UsageMode
import com.mimiduku.mobile.service.MimidukuAccessibilityService
import com.mimiduku.mobile.settings.SetupStatus

/** 保護者・利用者向けの設定画面。サービスの状態、有効化の手順、利用モード、検知ログの件数を出す。 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val modeStore = MimidukuAccessibilityService.usageModeStore(this)
        val detectionLog = MimidukuAccessibilityService.detectionLog(this)
        setContent {
            var serviceEnabled by remember { mutableStateOf(isServiceEnabled()) }
            var logCount by remember { mutableIntStateOf(0) }
            var mode by remember { mutableStateOf(modeStore.load()) }
            // 設定画面から戻ったときに状態を読み直す。
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                serviceEnabled = isServiceEnabled()
                logCount = detectionLog.entries().size
            }
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        state =
                            SettingsState(
                                serviceEnabled = serviceEnabled,
                                showRestrictedGuide = SetupStatus.needsRestrictedSettingsGuide(Build.VERSION.SDK_INT),
                                mode = mode,
                                logCount = logCount,
                            ),
                        actions =
                            SettingsActions(
                                openAccessibilitySettings = ::openAccessibilitySettings,
                                openAppDetails = ::openAppDetails,
                                selectMode = {
                                    modeStore.save(it)
                                    mode = it
                                },
                            ),
                    )
                }
            }
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

private data class SettingsState(
    val serviceEnabled: Boolean,
    val showRestrictedGuide: Boolean,
    val mode: UsageMode,
    val logCount: Int,
)

private class SettingsActions(
    val openAccessibilitySettings: () -> Unit,
    val openAppDetails: () -> Unit,
    val selectMode: (UsageMode) -> Unit,
)

@Composable
private fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(text = stringResource(R.string.settings_intro))
        SettingsCard(title = stringResource(R.string.settings_service_title)) {
            val status =
                if (state.serviceEnabled) R.string.settings_service_enabled else R.string.settings_service_disabled
            Text(text = stringResource(status), style = MaterialTheme.typography.titleMedium)
            Button(onClick = actions.openAccessibilitySettings) {
                Text(text = stringResource(R.string.settings_open_accessibility))
            }
        }
        if (state.showRestrictedGuide && !state.serviceEnabled) {
            SettingsCard(title = stringResource(R.string.settings_restricted_title)) {
                Text(text = stringResource(R.string.settings_restricted_steps))
                OutlinedButton(onClick = actions.openAppDetails) {
                    Text(text = stringResource(R.string.settings_open_app_details))
                }
            }
        }
        SettingsCard(title = stringResource(R.string.settings_mode_title)) {
            Column(modifier = Modifier.selectableGroup()) {
                UsageMode.entries.forEach { ModeOption(it, state, actions) }
            }
        }
        SettingsCard(title = stringResource(R.string.settings_log_title)) {
            Text(text = stringResource(R.string.settings_log_count, state.logCount))
            Text(text = stringResource(R.string.settings_log_privacy), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun ModeOption(
    option: UsageMode,
    state: SettingsState,
    actions: SettingsActions,
) {
    val selected = option == state.mode
    val (labelRes, descriptionRes) =
        when (option) {
            UsageMode.CHILD -> R.string.settings_mode_child to R.string.settings_mode_child_desc
            UsageMode.ADULT -> R.string.settings_mode_adult to R.string.settings_mode_adult_desc
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = { actions.selectMode(option) }, role = Role.RadioButton)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = stringResource(labelRes), style = MaterialTheme.typography.titleMedium)
            Text(text = stringResource(descriptionRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
