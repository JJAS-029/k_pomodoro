package com.jjas.labpomodoro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val current = settings
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            SettingsContent(
                settings = current,
                onBack = onBack,
                onSessionChange = viewModel::updateSession,
                onSoundChange = viewModel::setSoundEnabled,
                onVibrationChange = viewModel::setVibrationEnabled,
                onKeepScreenOnChange = viewModel::setKeepScreenOn,
            )
        }
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onSessionChange: ((SessionConfig) -> SessionConfig) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
) {
    val session = settings.session
    Column(
        modifier = Modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text("Listo") }
        }

        SectionTitle("Plan de sesiones")
        StepperRow("Horas totales de trabajo", session.totalHours, SessionConfig.TOTAL_HOURS, "h") { v ->
            onSessionChange { it.copy(totalHours = v) }
        }
        StepperRow("Pomodoro", session.workMinutes, SessionConfig.WORK_MINUTES, "min", step = 5) { v ->
            onSessionChange { it.copy(workMinutes = v) }
        }
        StepperRow("Descanso corto", session.shortBreakMinutes, SessionConfig.SHORT_BREAK_MINUTES, "min") { v ->
            onSessionChange { it.copy(shortBreakMinutes = v) }
        }
        StepperRow("Descanso largo", session.longBreakMinutes, SessionConfig.LONG_BREAK_MINUTES, "min", step = 5) { v ->
            onSessionChange { it.copy(longBreakMinutes = v) }
        }
        StepperRow("Pomodoros antes del descanso largo", session.pomodorosUntilLong, SessionConfig.POMODOROS_UNTIL_LONG, "") { v ->
            onSessionChange { it.copy(pomodorosUntilLong = v) }
        }

        Spacer(Modifier.height(12.dp))
        Text("Si las horas no cuadran exacto", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        val options = listOf(
            PlanRounding.TRIM_LAST to "Recortar el último",
            PlanRounding.WHOLE_POMODOROS to "Pomodoros enteros",
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = session.rounding == mode,
                    onClick = { onSessionChange { it.copy(rounding = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }
        Text(
            text = when (session.rounding) {
                PlanRounding.TRIM_LAST -> "Nunca te pasas de las horas pedidas."
                PlanRounding.WHOLE_POMODOROS -> "Todos los pomodoros duran lo mismo; puede pasarse un poco."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionTitle("Avisos")
        SwitchRow("Sonidos", settings.soundEnabled, onSoundChange)
        SwitchRow("Vibración", settings.vibrationEnabled, onVibrationChange)
        SwitchRow("Mantener la pantalla encendida", settings.keepScreenOn, onKeepScreenOnChange)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
    HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
        ) { Text("−") }
        Text(
            text = if (unit.isEmpty()) "$value" else "$value $unit",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.displaySmall.fontFamily),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        OutlinedIconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
        ) { Text("+") }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsContentPreview() {
    LabPomodoroTheme {
        SettingsContent(AppSettings(), {}, {}, {}, {}, {})
    }
}
