package com.jjas.labpomodoro.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.ui.components.KoalaAvatar

/** Se muestra una sola vez, cuando el usuario descubre los 118 elementos. */
@Composable
fun TableCompleteDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("Gracias, koala") } },
        title = {
            Text(
                "¡Completaste la tabla periódica!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KoalaAvatar(size = 96.dp)
                Text(
                    "Los ${PeriodicTable.SIZE} elementos. Cada uno salió de tu tiempo de enfoque: de horas en las " +
                        "que elegiste concentrarte cuando era más fácil distraerse.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Mendeléyev tardó años en ordenar esta tabla, y generaciones de científicos dedicaron su vida " +
                        "a encontrar cada casilla. Tú la llenaste pomodoro a pomodoro, y hasta fabricaste en tu " +
                        "sintetizador los que no existen en la naturaleza.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Esto no es solo una colección: es la prueba de que la constancia transforma. Así como estos " +
                        "elementos forman todo lo que existe, tus horas de enfoque están formando a la persona que " +
                        "quieres ser.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Gracias por dejar que este pequeño laboratorio te acompañara. Aquí seguimos para lo que sigue.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "— JJAS y el koala",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
