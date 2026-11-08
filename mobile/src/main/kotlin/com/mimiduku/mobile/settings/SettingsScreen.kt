package com.mimiduku.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.painterResource
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
    /** 通知にバッジの操作を出すか。 */
    val badgeNotification: Boolean = true,
    /** この版で有効な機能。画面に出す説明の切り替えに使う。 */
    val features: FeatureSet = FeatureSet.GITHUB,
    /** アクセシビリティ API の利用について同意済みか。 */
    val consented: Boolean = true,
    /** 確認を省くアプリ。 */
    val excludedPackages: List<ExcludedApp> = emptyList(),
    /** 確認を省くサイトの件数（ドメインはハッシュで保存するため、一覧には出せない）。 */
    val excludedDomainCount: Int = 0,
    /** 一覧に出せる（暗号化した控えのある）サイトの数。残りは旧い登録で、件数と全削除だけできる。 */
    val excludedDomainListable: Int = 0,
)

/** 確認を省くアプリ。[label] はアプリ名（分からなければパッケージ名）。 */
data class ExcludedApp(
    val packageName: String,
    val label: String,
)

/** 確認を省く対象の操作。追加はバナーの「今後は確認しない」から行い、ここでは外すだけ（PIN は不要）。 */
interface ExclusionActions {
    fun removeExcludedPackage(packageName: String)

    fun clearExcludedDomains()

    /** 保護者 PIN を入力して、省くサイトの一覧を表示する。 */
    fun showExcludedDomains()

    /** 一覧に表示中のサイトを外す。 */
    fun removeExcludedDomain(id: String)
}

/** みみづくバッジの操作。 */
interface BadgeActions {
    fun setBadgeVisible(visible: Boolean)

    fun setBadgeNotification(enabled: Boolean)
}

/** 設定画面の操作。PIN が要るかの判定は実装側（[SettingsController]）で行う。 */
interface SettingsActions : BadgeActions {
    fun openAccessibilitySettings()

    fun openAppDetails()

    fun selectMode(mode: UsageMode)

    fun setPin()

    fun changePin()

    fun removePin()

    fun pause(minutes: Int)

    fun resume()

    fun openLog()
}

/** 画面間の移動の操作。 */
interface NavigationActions {
    fun openSettings()

    fun openAbout()

    fun openSupportPage()

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
    exclusions: ExclusionActions,
) {
    BackHandler(onBack = navigation::navigateUp)
    AppScaffold(title = stringResource(R.string.menu_settings), navigateUp = navigation::navigateUp) {
        SettingsCard(title = stringResource(R.string.settings_mode_title), icon = "🧒") {
            Column(modifier = Modifier.selectableGroup()) {
                UsageMode.entries.forEach { ModeOption(it, state, actions) }
            }
        }
        PinCard(state, actions)
        ExclusionCard(state, exclusions)
        val badgeHelp =
            if (state.features.badgeReactsToAds) {
                R.string.settings_badge_description
            } else {
                R.string.settings_badge_description_limited
            }
        SettingsCard(
            title = stringResource(R.string.settings_badge_title),
            help = stringResource(badgeHelp),
            icon = "🦉",
        ) {
            SwitchRow(R.string.settings_badge_show, state.badgeVisible, actions::setBadgeVisible)
            SwitchRow(R.string.settings_badge_notification, state.badgeNotification, actions::setBadgeNotification)
        }
    }
}

@Composable
private fun SwitchRow(
    labelRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(labelRes), modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * アクセシビリティ API の利用についての目立つ開示と同意（Google Play の要件。両方の版で出す）。
 * 同意のチェックとボタンの操作を求め、同意するまでアクセシビリティ設定へ進めない。
 */
@Composable
internal fun DisclosureCard(consentActions: ConsentActions) {
    var checked by remember { mutableStateOf(false) }
    SettingsCard(title = stringResource(R.string.disclosure_title), icon = "👀") {
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
    SettingsCard(
        title = stringResource(R.string.settings_service_title),
        icon = "🦉",
        help = stringResource(R.string.settings_intro),
    ) {
        val status =
            if (state.serviceEnabled) R.string.settings_service_enabled else R.string.settings_service_disabled
        val owl = if (state.serviceEnabled) R.drawable.badge_owl_wide else R.drawable.badge_owl_sleepy
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painter = painterResource(owl),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
            Text(text = stringResource(status), style = MaterialTheme.typography.titleMedium)
        }
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
    SettingsCard(
        title = stringResource(R.string.settings_pause_title),
        icon = "⏱️",
        help = stringResource(R.string.settings_pause_description),
    ) {
        if (state.pauseRemainingMinutes > 0) {
            Text(
                text = stringResource(R.string.settings_pause_active, state.pauseRemainingMinutes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = actions::resume) { Text(stringResource(R.string.settings_pause_resume)) }
        } else {
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
private fun ExclusionCard(
    state: SettingsState,
    actions: ExclusionActions,
) {
    SettingsCard(
        title = stringResource(R.string.settings_exclusion_title),
        icon = "✅",
        help = stringResource(R.string.settings_exclusion_help),
    ) {
        if (state.excludedPackages.isEmpty() && state.excludedDomainCount == 0) {
            Text(text = stringResource(R.string.settings_exclusion_empty))
        }
        state.excludedPackages.forEach { app ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = app.label, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = { actions.removeExcludedPackage(app.packageName) }) {
                    Text(stringResource(R.string.settings_exclusion_remove))
                }
            }
        }
        if (state.excludedDomainCount > 0) {
            Text(text = stringResource(R.string.settings_exclusion_domains, state.excludedDomainCount))
            if (state.excludedDomainListable > 0) {
                OutlinedButton(onClick = actions::showExcludedDomains) {
                    Text(stringResource(R.string.settings_exclusion_show_domains))
                }
            }
            OutlinedButton(onClick = actions::clearExcludedDomains) {
                Text(stringResource(R.string.settings_exclusion_clear_domains))
            }
        }
    }
}

@Composable
private fun PinCard(
    state: SettingsState,
    actions: SettingsActions,
) {
    SettingsCard(
        title = stringResource(R.string.settings_pin_title),
        icon = "🔑",
        help = stringResource(R.string.settings_pin_help),
    ) {
        val status = if (state.pinSet) R.string.settings_pin_status_set else R.string.settings_pin_status_not_set
        Text(text = stringResource(status), style = MaterialTheme.typography.titleMedium)
        if (state.pinSet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = actions::changePin) { Text(stringResource(R.string.settings_pin_change)) }
                OutlinedButton(onClick = actions::removePin) { Text(stringResource(R.string.settings_pin_remove)) }
            }
        } else {
            Button(onClick = actions::setPin) { Text(stringResource(R.string.settings_pin_setup)) }
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
            if (selected) Text(text = stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall)
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
