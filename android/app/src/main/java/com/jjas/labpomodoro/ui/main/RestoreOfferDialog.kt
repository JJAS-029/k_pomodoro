package com.jjas.labpomodoro.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.backup.BackupInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Fecha del respaldo en hora local y en el idioma del teléfono, para mostrarla. */
fun BackupInfo.formattedDate(): String = Instant.ofEpochMilli(createdAtMillis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).withLocale(Locale.getDefault()))

/** Al iniciar sesión en un teléfono sin progreso, si hay un respaldo en la nube. */
@Composable
fun RestoreOfferDialog(info: BackupInfo, onRestore: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onRestore) { Text(stringResource(R.string.main_restore_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.main_restore_dismiss)) } },
        title = { Text(stringResource(R.string.main_restore_title)) },
        text = {
            Text(
                if (info.device.isNotBlank()) {
                    stringResource(R.string.main_restore_text_device, info.formattedDate(), info.device, info.elements, info.sessions)
                } else {
                    stringResource(R.string.main_restore_text, info.formattedDate(), info.elements, info.sessions)
                }
            )
        },
    )
}
