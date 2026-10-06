package com.jjas.labpomodoro.ui.stats

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import com.jjas.labpomodoro.domain.usecase.FocusStats
import com.jjas.labpomodoro.domain.usecase.StatsRange
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val SPANISH: Locale = Locale.forLanguageTag("es")

/** Tiempo legible: "25 min", "1 h 15 min", "3 h". */
fun formatFocus(seconds: Long): String {
    val minutes = seconds / 60
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

@Composable
fun StatsScreen(onBack: () -> Unit, onOpenPro: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Column(
                Modifier
                    .weight(1f)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Progreso",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onBack) { Text("Listo") }
                }
                Spacer(Modifier.height(12.dp))
                RangeSelector(range, viewModel::setRange)
                Spacer(Modifier.height(16.dp))

                val current = state
                if (current == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Summary(current.stats, current.streak.current)
                    if (current.stats.isEmpty) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "Aún no hay sesiones en este periodo. Completa un pomodoro y aquí verás tus patrones.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Charts(current.stats)
                    }
                    Section("Dónde te concentras")
                    Places(
                        ui = current.places,
                        onOpenPro = onOpenPro,
                        onEnable = { viewModel.setPlacesEnabled(true) },
                        onDisable = { viewModel.setPlacesEnabled(false) },
                        onRename = viewModel::renamePlace,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            AdBanner(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun RangeSelector(range: StatsRange, onChange: (StatsRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        StatsRange.entries.forEachIndexed { i, r ->
            SegmentedButton(
                selected = r == range,
                onClick = { onChange(r) },
                shape = SegmentedButtonDefaults.itemShape(i, StatsRange.entries.size),
            ) { Text(r.label) }
        }
    }
}

@Composable
private fun Summary(stats: FocusStats, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(formatFocus(stats.workSeconds), "de enfoque", Modifier.weight(1f))
        Tile(stats.completed.toString(), "pomodoros", Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(stats.completionRate?.let { "$it %" } ?: "—", "completados sin saltar", Modifier.weight(1f))
        Tile("$streak ${if (streak == 1) "día" else "días"}", "de racha", Modifier.weight(1f))
    }
    if (stats.activeDays > 0) {
        Spacer(Modifier.height(8.dp))
        Text(
            "En promedio, ${formatFocus(stats.averagePerActiveDaySeconds)} los días que te concentras.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Tile(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Section(title: String, subtitle: String? = null) {
    Spacer(Modifier.height(28.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
    subtitle?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(8.dp))
}

private val DAY = DateTimeFormatter.ofPattern("EEE d 'de' MMM", SPANISH)
private val MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", SPANISH)

@Composable
private fun Charts(stats: FocusStats) {
    val timeline = stats.timeline
    val monthly = stats.range == StatsRange.ALL
    Section(if (monthly) "Por mes" else "Por día", "Toca una barra para ver su valor")
    BarChart(
        values = timeline.map { it.seconds },
        labels = timeline.mapIndexed { i, b ->
            when {
                monthly -> b.start.month.getDisplayName(TextStyle.NARROW, SPANISH).uppercase()
                timeline.size <= 7 -> b.start.dayOfWeek.getDisplayName(TextStyle.NARROW, SPANISH).uppercase()
                // En 30 días, una etiqueta por semana
                (timeline.size - 1 - i) % 7 == 0 -> b.start.dayOfMonth.toString()
                else -> ""
            }
        },
        describe = { i ->
            val b = timeline[i]
            val date = if (monthly) MONTH.format(b.start) else DAY.format(b.start)
            "${date.replaceFirstChar { it.uppercase() }}: ${formatFocus(b.seconds)}"
        },
    )

    Section(
        "Qué días te concentras más",
        stats.bestWeekday?.let { "Tu mejor día: ${it.getDisplayName(TextStyle.FULL, SPANISH)}" },
    )
    BarChart(
        values = stats.byWeekday,
        labels = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, SPANISH).uppercase() },
        describe = { i -> "${DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, SPANISH).replaceFirstChar { it.uppercase() }}: ${formatFocus(stats.byWeekday[i])}" },
    )

    Section(
        "A qué hora rindes más",
        stats.bestHour?.let { "Tu mejor hora: de $it:00 a ${(it + 1) % 24}:00" },
    )
    BarChart(
        values = stats.byHour,
        labels = (0..23).map { if (it % 6 == 0) "$it h" else "" },
        describe = { i -> "De $i:00 a ${(i + 1) % 24}:00: ${formatFocus(stats.byHour[i])}" },
    )
}

@Composable
private fun Places(
    ui: PlacesUi,
    onOpenPro: () -> Unit,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onRename: (Long, String) -> Unit,
) {
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onEnable()
    }
    val explain = "Al iniciar un plan se guarda tu ubicación aproximada, solo en el teléfono, para ver en qué " +
        "lugares rindes más. Nunca se sube a internet."
    when (ui) {
        PlacesUi.NotPro -> {
            Text(explain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onOpenPro) { Text("Disponible en Pro") }
        }
        PlacesUi.Disabled -> {
            Text(explain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                Text("Activar lugares")
            }
        }
        is PlacesUi.Enabled -> {
            if (ui.places.isEmpty()) {
                Text(
                    "Inicia un plan y aquí aparecerá tu primer lugar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Toca un lugar para ponerle nombre.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val max = ui.places.maxOf { it.workSeconds }.coerceAtLeast(1)
                ui.places.forEach { PlaceRow(it, max, onRename) }
            }
            TextButton(onClick = onDisable) { Text("Dejar de guardar lugares") }
        }
    }
}

@Composable
private fun PlaceRow(place: PlaceTotal, maxSeconds: Long, onRename: (Long, String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val total = place.completed + place.skipped
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { editing = true }
            .padding(vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(place.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(formatFocus(place.workSeconds), style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { place.workSeconds.toFloat() / maxSeconds },
            modifier = Modifier.fillMaxWidth(),
        )
        if (total > 0) {
            Text(
                "${place.completed * 100 / total} % de tus pomodoros completados aquí · $total en total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (editing) {
        var name by rememberSaveable { mutableStateOf(place.name) }
        AlertDialog(
            onDismissRequest = { editing = false },
            confirmButton = {
                TextButton(onClick = {
                    onRename(place.placeId, name)
                    editing = false
                }) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } },
            title = { Text("Nombre del lugar") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    placeholder = { Text("Casa, Oficina, Biblioteca…", textAlign = TextAlign.Start) },
                )
            },
        )
    }
}
