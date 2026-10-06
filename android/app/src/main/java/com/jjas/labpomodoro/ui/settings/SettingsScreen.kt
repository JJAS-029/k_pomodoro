package com.jjas.labpomodoro.ui.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.ui.promo.GITHUB_URL
import com.jjas.labpomodoro.ui.promo.Podcast
import com.jjas.labpomodoro.ui.promo.SUGGESTIONS_EMAIL
import com.jjas.labpomodoro.ui.promo.openUrl
import com.jjas.labpomodoro.ui.promo.sendSuggestion
import com.jjas.labpomodoro.ui.sound.FocusSoundPanel
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPro: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val exportMessage by viewModel.message.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    // El usuario elige dónde guardar el archivo (Descargas, Drive…)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsv)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val current = settings
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column {
                SettingsContent(
                    modifier = Modifier.weight(1f),
                    settings = current,
                    onBack = onBack,
                    onSessionChange = viewModel::updateSession,
                    onSoundChange = viewModel::setSoundEnabled,
                    onVibrationChange = viewModel::setVibrationEnabled,
                    onKeepScreenOnChange = viewModel::setKeepScreenOn,
                    onAmbientChange = viewModel::setAmbientMode,
                    onAmbientDelayChange = viewModel::setAmbientDelayMinutes,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onResetVesselElement = viewModel::resetVesselElement,
                    soundSection = { FocusSoundPanel(onOpenPro = onOpenPro) },
                    backupSection = { CloudBackupSection() },
                    dataSection = {
                        DataSection(
                            isPro = current.isPro,
                            message = exportMessage,
                            onExport = { exportLauncher.launch("lab-pomodoro-historial.csv") },
                            onOpenPro = onOpenPro,
                            showPrivacy = !current.isPro && viewModel.privacyOptionsRequired,
                            onPrivacy = { activity?.let(viewModel::showPrivacyOptions) },
                        )
                    },
                )
                // Solo en la versión gratis; no tapa nada porque va debajo del contenido
                AdBanner(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun DataSection(
    isPro: Boolean,
    message: String?,
    onExport: () -> Unit,
    onOpenPro: () -> Unit,
    showPrivacy: Boolean,
    onPrivacy: () -> Unit,
) {
    Text(
        "Descarga todas tus sesiones (fecha, tipo, minutos y si se completó) para verlas en una hoja de cálculo.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    if (isPro) {
        OutlinedButton(onClick = onExport) { Text("Exportar historial (CSV)") }
    } else {
        FilledTonalButton(onClick = onOpenPro) { Text("Exportar historial · Pro") }
    }
    message?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (showPrivacy) {
        TextButton(onClick = onPrivacy) { Text("Privacidad de anuncios") }
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
    onAmbientChange: (Boolean) -> Unit,
    onAmbientDelayChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onResetVesselElement: () -> Unit = {},
    soundSection: @Composable () -> Unit = {},
    dataSection: @Composable () -> Unit = {},
    backupSection: @Composable () -> Unit = {},
) {
    val session = settings.session
    Column(
        modifier = modifier
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

        SectionTitle(if (settings.isPro) "Modo ambiente" else "Modo ambiente · Pro")
        SwitchRow(
            label = "Solo el reloj si no tocas la pantalla",
            checked = settings.ambientActive,
            onCheckedChange = onAmbientChange,
            enabled = settings.isPro,
        )
        Text(
            text = if (settings.isPro) {
                "Mientras corre el timer, la pantalla se atenúa y queda solo el tiempo. Tócala para volver."
            } else {
                "Disponible en la versión Pro."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.ambientActive) {
            Spacer(Modifier.height(12.dp))
            Text("Activar después de", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val delays = AppSettings.AMBIENT_DELAYS
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                delays.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = settings.ambientDelayMinutes == minutes,
                        onClick = { onAmbientDelayChange(minutes) },
                        shape = SegmentedButtonDefaults.itemShape(index, delays.size),
                    ) { Text("$minutes min") }
                }
            }
        }

        SectionTitle("Recipientes")
        val fixed = settings.vesselElement.takeIf { it in 1..PeriodicTable.SIZE }?.let { PeriodicTable[it] }
        Text(
            text = if (fixed == null) {
                "Variados: cada sesión de trabajo usa un elemento distinto de tu colección."
            } else {
                "Fijo: todos los recipientes usan ${fixed.name.lowercase()}."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (fixed != null) {
            TextButton(onClick = onResetVesselElement) { Text("Volver a variados") }
        } else {
            Text(
                "Para fijar uno, ábrelo en Logros › Tabla periódica y toca \"Usar en todos mis recipientes\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Material You existe desde Android 12
        val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        SectionTitle(if (settings.isPro) "Apariencia" else "Apariencia · Pro")
        SwitchRow(
            label = "Colores del sistema (Material You)",
            checked = settings.dynamicColorActive,
            onCheckedChange = onDynamicColorChange,
            enabled = settings.isPro && dynamicAvailable,
        )
        Text(
            text = when {
                !dynamicAvailable -> "Requiere Android 12 o superior."
                !settings.isPro -> "Disponible en la versión Pro."
                else -> "Los botones y acentos toman los colores de tu fondo de pantalla. El fondo sigue negro."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(if (settings.isPro) "Sonido de concentración" else "Sonido de concentración · Pro")
        soundSection()

        SectionTitle("Respaldo en la nube")
        backupSection()

        SectionTitle(if (settings.isPro) "Tus datos" else "Tus datos · Pro")
        dataSection()

        SectionTitle("Acerca de")
        AboutSection()
        Spacer(Modifier.height(24.dp))
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
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsContentPreview() {
    LabPomodoroTheme {
        SettingsContent(AppSettings(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/** Créditos: el creador, sus podcasts y su GitHub. Se ve también en Pro. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    Text(
        "Lab Pomodoro ${BuildConfig.VERSION_NAME} · hecho por JJAS. Si te gusta la ciencia, escucha mis podcasts:",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Podcast.entries.forEach { podcast ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { context.openUrl(podcast.spotifyUrl) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(podcast.cover),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(podcast.title, style = MaterialTheme.typography.titleSmall)
                Text(podcast.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Escuchar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "¿Una idea, algo que no funciona o un elemento que te encantó? Escríbeme.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = { context.sendSuggestion(BuildConfig.VERSION_NAME) }) { Text("Enviar sugerencias") }
    Text(SUGGESTIONS_EMAIL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { context.openUrl(GITHUB_URL) }) { Text("Mi GitHub: JJAS-029") }
}
