package com.mimiduku.mobile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mimiduku.mobile.R
import com.mimiduku.mobile.pin.PinCheck
import com.mimiduku.mobile.pin.PinManager

/** PIN の入力を求める。照合に成功したら [onSuccess] を呼ぶ。 */
@Composable
fun PinEntryDialog(
    onVerify: (String) -> PinCheck,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_entry_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PinField(value = pin, label = R.string.pin_field_label, onValueChange = { pin = it })
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val check = onVerify(pin)
                pin = ""
                error = PinPolicy.messageFor(check)
                if (check == PinCheck.Success) onSuccess()
            }) { Text(stringResource(R.string.pin_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pin_cancel)) } },
    )
}

/** 新しい PIN を2回入力させて設定する。[onSave] は形式が合えば true を返す。 */
@Composable
fun PinSetupDialog(
    onSave: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pin_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pin_setup_hint))
                PinField(value = first, label = R.string.pin_field_new, onValueChange = { first = it })
                PinField(value = second, label = R.string.pin_field_confirm, onValueChange = { second = it })
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error =
                    when {
                        !PinManager.isValidFormat(first) -> R.string.pin_error_format
                        first != second -> R.string.pin_error_mismatch
                        !onSave(first) -> R.string.pin_error_format
                        else -> null
                    }
            }) { Text(stringResource(R.string.pin_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pin_cancel)) } },
    )
}

@Composable
private fun PinField(
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it in '0'..'9' }.take(PinManager.MAX_LENGTH)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}

/** 確認を省く対象の追加。遷移元のアプリと、読めたサイトのうち登録するものを選ばせる。 */
@Composable
fun AddExclusionDialog(
    dialog: SettingsDialog.AddExclusion,
    onConfirm: (addPackage: Boolean, addDomain: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var addPackage by remember { mutableStateOf(false) }
    var addDomain by remember { mutableStateOf(false) }
    val domain = dialog.candidate.domain
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exclusion_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.exclusion_add_hint))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = addPackage, onCheckedChange = { addPackage = it })
                    Text(
                        stringResource(
                            R.string.exclusion_add_app,
                            dialog.appLabel ?: dialog.candidate.sourcePackage,
                        ),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = addDomain && domain != null,
                        onCheckedChange = { addDomain = it },
                        enabled = domain != null,
                    )
                    Text(
                        if (domain != null) {
                            stringResource(R.string.exclusion_add_site, domain)
                        } else {
                            stringResource(R.string.exclusion_add_site_unknown)
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(addPackage, addDomain && domain != null) },
                enabled = addPackage || (addDomain && domain != null),
            ) { Text(stringResource(R.string.exclusion_add_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.pin_cancel)) } },
    )
}

/** 確認を省くサイトの一覧。PIN で復号した内容で、閉じると破棄する。個別に外せる（PIN は不要）。 */
@Composable
fun ExcludedDomainsDialog(
    dialog: SettingsDialog.DomainList,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exclusion_domains_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialog.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = item.domain, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onRemove(item.id) }) {
                            Text(stringResource(R.string.settings_exclusion_remove))
                        }
                    }
                }
                if (dialog.unlistedCount > 0) {
                    Text(stringResource(R.string.exclusion_domains_unlisted, dialog.unlistedCount))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.exclusion_domains_close)) } },
    )
}
