package com.mimiduku.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R
import com.mimiduku.mobile.feature.FeatureSet
import com.mimiduku.mobile.mode.PauseStore
import com.mimiduku.mobile.mode.UsageMode

/** 設定画面に出す状態。 */
data class SettingsState(
    val serviceEnabled: Boolean,
    val showRestrictedGuide: Boolean,
    val mode: UsageMode,
    val logCount: Int,
    val pinSet: Boolean,
    /** 一時停止の残り時間（分、切り上げ）。停止していなければ 0。 */
    val pauseRemainingMinutes: Int,
    val badgeVisible: Boolean,
    /** この版で有効な機能。画面に出す説明の切り替えに使う。 */
    val features: FeatureSet = FeatureSet.GITHUB,
    /** アクセシビリティ API の利用について同意済みか。 */
    val consented: Boolean = true,
)

/** 設定画面の操作。PIN が要るかの判定は実装側（[SettingsController]）で行う。 */
interface SettingsActions {
    fun openAccessibilitySettings()

    fun openAppDetails()

    fun selectMode(mode: UsageMode)

    fun setPin()

    fun changePin()

    fun removePin()

    fun pause(minutes: Int)

    fun resume()

    fun openLog()

    fun setBadgeVisible(visible: Boolean)
}

/** 画面間の移動の操作。 */
interface NavigationActions {
    fun openSettings()

    fun openAbout()

    fun navigateUp()
}

/** アクセシビリティ API の利用についての開示と同意の操作。 */
interface ConsentActions {
    fun acceptDisclosure()
}

/** ログ閲覧画面の操作。 */
interface LogActions {
    fun closeLog()

    fun clearLog()
}

/** 設定画面。利用モード・保護者 PIN・みみづくバッジ。 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    actions: SettingsActions,
    navigation: NavigationActions,
) {
    BackHandler(onBack = navigation::navigateUp)
    AppScaffold(title = stringResource(R.string.menu_settings), navigateUp = navigation::navigateUp) {
        SettingsCard(title = stringResource(R.string.settings_mode_title)) {
            Column(modifier = Modifier.selectableGroup()) {
                UsageMode.entries.forEach { ModeOption(it, state, actions) }
            }
        }
        PinCard(state, actions)
        SettingsCard(title = stringResource(R.string.settings_badge_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val description =
                    if (state.features.badgeReactsToAds) {
                        R.string.settings_badge_description
                    } else {
                        R.string.settings_badge_description_limited
                    }
                Text(text = stringResource(description), modifier = Modifier.weight(1f))
                Switch(checked = state.badgeVisible, onCheckedChange = actions::setBadgeVisible)
            }
        }
    }
}

/**
 * アクセシビリティ API の利用についての目立つ開示と同意（Google Play の要件。両方の版で出す）。
 * 同意のチェックとボタンの操作を求め、同意するまでアクセシビリティ設定へ進めない。
 */
@Composable
internal fun DisclosureCard(consentActions: ConsentActions) {
    var checked by remember { mutableStateOf(false) }
    SettingsCard(title = stringResource(R.string.disclosure_title)) {
        Text(text = stringResource(R.string.disclosure_body))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = checked, onCheckedChange = { checked = it })
            Text(text = stringResource(R.string.disclosure_check))
        }
        Button(onClick = consentActions::acceptDisclosure, enabled = checked) {
            Text(text = stringResource(R.string.disclosure_accept))
        }
    }
}

@Composable
internal fun ServiceCard(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsCard(title = stringResource(R.string.settings_service_title)) {
        val status =
            if (state.serviceEnabled) R.string.settings_service_enabled else R.string.settings_service_disabled
        Text(text = stringResource(status), style = MaterialTheme.typography.titleMedium)
        Button(onClick = actions::openAccessibilitySettings) {
            Text(text = stringResource(R.string.settings_open_accessibility))
        }
    }
}

@Composable
internal fun PauseCard(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsCard(title = stringResource(R.string.settings_pause_title)) {
        if (state.pauseRemainingMinutes > 0) {
            Text(
                text = stringResource(R.string.settings_pause_active, state.pauseRemainingMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = actions::resume) { Text(stringResource(R.string.settings_pause_resume)) }
        } else {
            Text(text = stringResource(R.string.settings_pause_description))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PauseStore.DURATIONS_MINUTES.forEach { minutes ->
                    OutlinedButton(onClick = { actions.pause(minutes) }) {
                        Text(stringResource(R.string.settings_pause_minutes, minutes))
                    }
                }
            }
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
                OutlinedButton(onClick = actions::changePin) { Text(stringResource(R.string.settings_pin_change)) }
                OutlinedButton(onClick = actions::removePin) { Text(stringResource(R.string.settings_pin_remove)) }
            }
        } else {
            Button(onClick = actions::setPin) { Text(stringResource(R.string.settings_pin_setup)) }
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
            UsageMode.CHILD -> R.string.settings_mode_child to childModeDescriptionRes(state.features)
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

/** 版の表示。 */
fun distributionLabelRes(features: FeatureSet): Int =
    when (features.distribution) {
        FeatureSet.Distribution.GITHUB -> R.string.settings_distribution_github
        FeatureSet.Distribution.PLAY -> R.string.settings_distribution_play
    }

/** こどもモードの説明。自動で戻さない版では「選ばせる」説明にする。 */
fun childModeDescriptionRes(features: FeatureSet): Int =
    if (features.autoBackOnTransition) R.string.settings_mode_child_desc else R.string.settings_mode_child_desc_ask
