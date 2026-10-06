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
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.ui.components.GoldColor
import com.jjas.labpomodoro.ui.components.KoalaAvatar
import com.jjas.labpomodoro.ui.components.metalColor

/** Una vez por nivel: cuando los 118 elementos llegan a bronce, a plata o a oro. */
@Composable
fun MasteryTableDialog(level: Mastery, onDismiss: () -> Unit) {
    val (title, body) = when (level) {
        Mastery.BRONZE -> "¡Tabla de bronce!" to listOf(
            "Los 118 elementos, cinco veces cada uno. Ya no es suerte: es método.",
            "La plata te espera. Cada pomodoro sigue sumando.",
        )
        Mastery.SILVER -> "¡Tabla de plata!" to listOf(
            "Diez veces cada elemento. A estas alturas tu laboratorio tiene más experiencia que muchos.",
            "Solo queda el oro: el nivel de quienes convierten la constancia en costumbre.",
        )
        else -> "¡Tabla de oro!" to listOf(
            "Quince veces cada uno de los 118 elementos. Lo que los alquimistas buscaron durante siglos, " +
                "convertir las cosas en oro, tú lo lograste con tiempo y enfoque.",
            "Llegaste a la cima de este laboratorio. Lo que construiste aquí, la costumbre de concentrarte, " +
                "ya es tuyo y va contigo a todo lo que hagas.",
            "Gracias por cada pomodoro.",
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text("¡Seguimos!") } },
        title = {
            Text(
                title,
                textAlign = TextAlign.Center,
                color = level.metalColor() ?: GoldColor,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KoalaAvatar(size = 88.dp)
                body.forEach {
                    Text(it, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
                Text("— JJAS y el koala", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}
