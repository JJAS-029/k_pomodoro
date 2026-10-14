package com.jjas.labpomodoro.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.ui.stats.formatFocus
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Ajustar el plan en curso: alargar esta sesión, sumar o quitar un pomodoro, o terminar al acabar
 * esta sesión. Los cambios se ven al momento en el resumen de arriba.
 */
@Composable
fun AdjustPlanSheet(
    active: TimerUi.Active,
    onExtend: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onFinishAfter: () -> Unit,
) {
    Column(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.main_plan_title), style = MaterialTheme.typography.titleLarge)
        // Resumen: lo que falta y a qué hora acabaría (en pausa, si se continúa ahora)
        val endsAt = LocalTime.now().plusSeconds(active.planRemainingMillis / 1000)
            .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        Text(
            stringResource(
                R.string.main_plan_summary,
                pluralStringResource(R.plurals.main_plan_pomodoros_left, active.pomodorosLeft, active.pomodorosLeft),
                formatFocus(active.planRemainingMillis / 1000),
                endsAt,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        FilledTonalButton(onClick = onExtend, enabled = active.canExtend, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.main_plan_extend))
        }
        FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.main_plan_add))
        }
        OutlinedButton(onClick = onRemove, enabled = active.pomodorosLeft > 0, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.main_plan_remove))
        }
        OutlinedButton(onClick = onFinishAfter, enabled = active.next != null, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.main_plan_finish_after))
        }
    }
}
