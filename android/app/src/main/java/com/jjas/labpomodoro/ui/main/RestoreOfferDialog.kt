package com.jjas.labpomodoro.ui.main

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.jjas.labpomodoro.data.backup.BackupInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BACKUP_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es"))

/** Fecha del respaldo en hora local, para mostrarla. */
fun BackupInfo.formattedDate(): String = Instant.ofEpochMilli(createdAtMillis).atZone(ZoneId.systemDefault()).format(BACKUP_DATE)

/** Al iniciar sesión en un teléfono sin progreso, si hay un respaldo en la nube. */
@Composable
fun RestoreOfferDialog(info: BackupInfo, onRestore: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onRestore) { Text("Recuperar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Empezar de cero") } },
        title = { Text("Encontramos tu laboratorio") },
        text = {
            Text(
                "Tienes un respaldo del ${info.formattedDate()}" +
                    (if (info.device.isNotBlank()) " (${info.device})" else "") +
                    " con ${info.elements} elementos y ${info.sessions} sesiones. ¿Quieres recuperarlo en este teléfono?"
            )
        },
    )
}
