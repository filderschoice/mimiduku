package com.mimiduku.mobile.settings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R
import com.mimiduku.mobile.mode.UsageMode

/** 設定画面に出す状態。 */
data class SettingsState(
    val serviceEnabled: Boolean,
    val showRestrictedGuide: Boolean,
    val mode: UsageMode,
    val logCount: Int,
    val pinSet: Boolean,
)

/** 設定画面の操作。PIN が要るかの判定は呼び出し側（MainActivity）で行う。 */
class SettingsActions(
    val openAccessibilitySettings: () -> Unit,
    val openAppDetails: () -> Unit,
    val selectMode: (UsageMode) -> Unit,
    val setPin: () -> Unit,
    val changePin: () -> Unit,
    val removePin: () -> Unit,
)

@Composable
fun SettingsScreen(
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
        ServiceCard(state, actions)
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
        PinCard(state, actions)
        SettingsCard(title = stringResource(R.string.settings_log_title)) {
            Text(text = stringResource(R.string.settings_log_count, state.logCount))
            Text(text = stringResource(R.string.settings_log_privacy), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ServiceCard(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsCard(title = stringResource(R.string.settings_service_title)) {
        val status =
            if (state.serviceEnabled) R.string.settings_service_enabled else R.string.settings_service_disabled
        Text(text = stringResource(status), style = MaterialTheme.typography.titleMedium)
        Button(onClick = actions.openAccessibilitySettings) {
            Text(text = stringResource(R.string.settings_open_accessibility))
        }
    }
}

@Composable
private fun PinCard(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsCard(title = stringResource(R.string.settings_pin_title)) {
        val status = if (state.pinSet) R.string.settings_pin_set else R.string.settings_pin_not_set
        Text(text = stringResource(status))
        if (state.pinSet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions.changePin) { Text(stringResource(R.string.settings_pin_change)) }
                OutlinedButton(onClick = actions.removePin) { Text(stringResource(R.string.settings_pin_remove)) }
            }
        } else {
            Button(onClick = actions.setPin) { Text(stringResource(R.string.settings_pin_setup)) }
        }
        Text(text = stringResource(R.string.settings_pin_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SettingsCard(
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
