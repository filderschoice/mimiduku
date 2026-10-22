package com.mimiduku.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R

/** 上部バーのアプリアイコンの拡大率（アダプティブアイコンの前景は余白が広いため、顔が見える大きさへ拡大する）。 */
private const val APP_ICON_ZOOM = 1.6f

/** ホーム画面。見張りの状態と一時停止、直近の検知を出す。歯車で設定、「⋮」でログ・アプリ情報・このアプリについてへ進む。 */
@Composable
fun HomeScreen(
    state: SettingsState,
    actions: SettingsActions,
    navigation: NavigationActions,
    consentActions: ConsentActions,
) {
    AppScaffold(
        title = stringResource(R.string.app_name),
        navigateUp = null,
        actions = {
            IconButton(onClick = navigation::openSettings) {
                Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.menu_settings))
            }
            OverflowMenu(actions, navigation)
        },
    ) {
        if (state.consented) ServiceCard(state, actions) else DisclosureCard(consentActions)
        if (state.consented) {
            if (state.showRestrictedGuide && !state.serviceEnabled) {
                SettingsCard(
                    title = stringResource(R.string.settings_restricted_title),
                    icon = "🚧",
                    help = stringResource(R.string.settings_restricted_steps),
                ) {
                    OutlinedButton(onClick = actions::openAppDetails) {
                        Text(text = stringResource(R.string.settings_open_app_details))
                    }
                }
            }
            PauseCard(state, actions)
            SettingsCard(
                title = stringResource(R.string.settings_log_title),
                icon = "📒",
                help = stringResource(R.string.settings_log_privacy),
            ) {
                Text(text = stringResource(R.string.settings_log_count, state.logCount))
                OutlinedButton(onClick = actions::openLog) { Text(stringResource(R.string.settings_log_open)) }
            }
        }
    }
}

/** このアプリについて。画面の読み取りの開示とログの扱いを、いつでも読み返せる。 */
@Composable
fun AboutScreen(
    state: SettingsState,
    navigation: NavigationActions,
) {
    BackHandler(onBack = navigation::navigateUp)
    AppScaffold(title = stringResource(R.string.menu_about), navigateUp = navigation::navigateUp) {
        SettingsCard(title = stringResource(R.string.settings_distribution_title), icon = "🏷️") {
            Text(
                text = stringResource(distributionLabelRes(state.features)),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        SettingsCard(title = stringResource(R.string.disclosure_title), icon = "👀") {
            Text(text = stringResource(R.string.disclosure_body))
        }
        SettingsCard(title = stringResource(R.string.settings_log_title), icon = "📒") {
            Text(text = stringResource(R.string.settings_log_privacy))
        }
        SettingsCard(
            title = stringResource(R.string.support_title),
            help = stringResource(R.string.support_help),
            icon = "🙋",
        ) {
            Text(text = stringResource(R.string.support_body))
            Button(onClick = navigation::openSupportPage) { Text(text = stringResource(R.string.support_open)) }
        }
    }
}

/** 上部のバーと縦スクロールの本文を持つ、各画面共通の枠。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppScaffold(
    title: String,
    navigateUp: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (navigateUp == null) {
                            Image(
                                painter = painterResource(R.drawable.ic_launcher_foreground),
                                contentDescription = null,
                                modifier =
                                    Modifier.size(
                                        40.dp,
                                    ).graphicsLayer(scaleX = APP_ICON_ZOOM, scaleY = APP_ICON_ZOOM),
                            )
                        }
                        Text(text = title)
                    }
                },
                navigationIcon = {
                    if (navigateUp != null) {
                        IconButton(onClick = navigateUp) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.menu_back),
                            )
                        }
                    }
                },
                actions = actions,
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

/** 「⋮」メニュー。検知ログ・アプリ情報（OS の画面）・このアプリについて。 */
@Composable
private fun OverflowMenu(
    actions: SettingsActions,
    navigation: NavigationActions,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.menu_more))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MenuItem(R.string.menu_log, actions::openLog) { expanded = false }
        MenuItem(R.string.menu_app_info, actions::openAppDetails) { expanded = false }
        MenuItem(R.string.menu_about, navigation::openAbout) { expanded = false }
    }
}

@Composable
private fun MenuItem(
    labelRes: Int,
    onClick: () -> Unit,
    onDone: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = stringResource(labelRes)) },
        onClick = {
            onDone()
            onClick()
        },
    )
}
