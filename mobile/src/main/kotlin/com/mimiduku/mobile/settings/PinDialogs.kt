package com.mimiduku.mobile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
