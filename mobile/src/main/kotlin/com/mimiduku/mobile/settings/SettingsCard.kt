package com.mimiduku.mobile.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R

/**
 * 設定画面共通のカード。必要な情報だけを本文に置き、補足の説明は [help] に回す。
 * [help] があるとタイトル右に「i」アイコンが付き、押すとカード内に説明が開閉する。
 */
@Composable
fun SettingsCard(
    title: String,
    help: String? = null,
    content: @Composable () -> Unit,
) {
    var helpOpen by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (help != null) HelpToggle(open = helpOpen, onToggle = { helpOpen = !helpOpen })
            }
            if (help != null) HelpPanel(visible = helpOpen, text = help)
            content()
        }
    }
}

/** 補足説明の開閉ボタン。開いている間は塗りのアイコンにして状態を示す。 */
@Composable
internal fun HelpToggle(
    open: Boolean,
    onToggle: () -> Unit,
) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (open) Icons.Filled.Info else Icons.Outlined.Info,
            contentDescription = stringResource(if (open) R.string.help_close else R.string.help_open),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 補足説明。色付きの面に小さめの文字で出し、本文と見分けられるようにする。 */
@Composable
internal fun HelpPanel(
    visible: Boolean,
    text: String,
) {
    AnimatedVisibility(visible = visible) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
        }
    }
}
