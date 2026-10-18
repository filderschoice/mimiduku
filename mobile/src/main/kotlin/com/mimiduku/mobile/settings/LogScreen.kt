package com.mimiduku.mobile.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R

/** 保護者向けの検知ログ閲覧画面。 */
@Composable
fun LogScreen(
    rows: List<LogRow>,
    actions: LogActions,
) {
    BackHandler(onBack = actions::closeLog)
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.log_title), style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = actions::closeLog) { Text(stringResource(R.string.log_close)) }
        }
        Text(text = stringResource(R.string.settings_log_privacy), style = MaterialTheme.typography.bodySmall)
        if (rows.isEmpty()) {
            Text(text = stringResource(R.string.log_empty))
        } else {
            OutlinedButton(onClick = actions::clearLog) { Text(stringResource(R.string.log_clear)) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows) { LogRowCard(it) }
            }
        }
    }
}

@Composable
private fun LogRowCard(row: LogRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = row.time, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "${stringResource(row.kindRes)}・${stringResource(row.actionRes)}",
                style = MaterialTheme.typography.titleSmall,
            )
            val apps =
                row.targetApp?.let { stringResource(R.string.log_apps_transition, row.sourceApp, it) }
                    ?: stringResource(R.string.log_apps_single, row.sourceApp)
            Text(text = apps, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
