package com.jjas.labpomodoro.ui.stats

import android.Manifest
import android.content.res.Resources
import android.text.format.DateFormat
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.ui.medals.MedalDialog
import com.jjas.labpomodoro.ui.medals.MedalGrid
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.usecase.FocusStats
import com.jjas.labpomodoro.domain.usecase.StatsRange
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Tiempo legible: "25 min", "1 h 15 min", "3 h". */
fun formatFocus(seconds: Long, resources: Resources): String {
    val minutes = seconds / 60
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> resources.getString(R.string.prog_focus_minutes, m)
        m == 0L -> resources.getString(R.string.prog_focus_hours, h)
        else -> resources.getString(R.string.prog_focus_hours_minutes, h, m)
    }
}

/** [formatFocus] con los recursos de la pantalla actual. */
@Composable
fun formatFocus(seconds: Long): String {
    // Se lee la configuración para que cambie con el idioma
    LocalConfiguration.current
    return formatFocus(seconds, LocalResources.current)
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
                        stringResource(R.string.prog_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onBack) { Text(stringResource(R.string.prog_done)) }
                }
                Spacer(Modifier.height(12.dp))
                RangeSelector(range, viewModel::setRange)
                Spacer(Modifier.height(16.dp))

                val current = state
                if (current == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Summary(current.stats, current.streak.current)
                    LaunchedEffect(current.medals) { viewModel.markMedalsSeen(current.medals) }
                    Medals(current.medals)
                    if (current.stats.isEmpty) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.prog_empty_period),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Charts(current.stats)
                    }
                    Section(stringResource(R.string.prog_places_section))
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
            ) { Text(stringResource(r.labelRes)) }
        }
    }
}

@Composable
private fun Summary(stats: FocusStats, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(formatFocus(stats.workSeconds), stringResource(R.string.prog_tile_focus), Modifier.weight(1f))
        Tile(stats.completed.toString(), stringResource(R.string.prog_tile_pomodoros), Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(
            stats.completionRate?.let { stringResource(R.string.prog_percent, it) } ?: "—",
            stringResource(R.string.prog_tile_completion),
            Modifier.weight(1f),
        )
        Tile(pluralStringResource(R.plurals.prog_streak_days, streak, streak), stringResource(R.string.prog_tile_streak), Modifier.weight(1f))
    }
    if (stats.activeDays > 0) {
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.prog_average, formatFocus(stats.averagePerActiveDaySeconds)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Medals(medals: List<MedalProgress>) {
    var selected by remember { mutableStateOf<MedalProgress?>(null) }
    Section(
        stringResource(R.string.prog_medals),
        stringResource(R.string.prog_medals_subtitle, medals.count { it.earned }, medals.size),
    )
    MedalGrid(medals, onSelect = { selected = it })
    selected?.let { chosen ->
        // Se busca de nuevo para que el progreso se actualice si la ventana sigue abierta
        MedalDialog(medals.firstOrNull { it.medal == chosen.medal } ?: chosen, onDismiss = { selected = null })
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

@Composable
private fun Charts(stats: FocusStats) {
    val resources = LocalResources.current
    val locale: Locale = LocalConfiguration.current.locales[0]
    // Patrones según el idioma: "mar, 5 oct" en español, "Tue, Oct 5" en inglés
    val day = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale) }
    val month = remember(locale) { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale) }
    fun value(label: String, seconds: Long) =
        resources.getString(R.string.prog_bar_value, label.replaceFirstChar { it.uppercase(locale) }, formatFocus(seconds, resources))
    fun hourRange(hour: Int) = resources.getString(R.string.prog_hour_range, hour, (hour + 1) % 24)

    val timeline = stats.timeline
    val monthly = stats.range == StatsRange.ALL
    Section(
        stringResource(if (monthly) R.string.prog_by_month else R.string.prog_by_day),
        stringResource(R.string.prog_tap_bar),
    )
    BarChart(
        values = timeline.map { it.seconds },
        labels = timeline.mapIndexed { i, b ->
            when {
                monthly -> b.start.month.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
                timeline.size <= 7 -> b.start.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
                // En 30 días, una etiqueta por semana
                (timeline.size - 1 - i) % 7 == 0 -> b.start.dayOfMonth.toString()
                else -> ""
            }
        },
        describe = { i ->
            val b = timeline[i]
            value(if (monthly) month.format(b.start) else day.format(b.start), b.seconds)
        },
    )

    Section(
        stringResource(R.string.prog_weekdays_title),
        stats.bestWeekday?.let { stringResource(R.string.prog_best_day, it.getDisplayName(TextStyle.FULL, locale)) },
    )
    BarChart(
        values = stats.byWeekday,
        labels = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, locale).uppercase(locale) },
        describe = { i -> value(DayOfWeek.of(i + 1).getDisplayName(TextStyle.FULL, locale), stats.byWeekday[i]) },
    )

    Section(
        stringResource(R.string.prog_hours_title),
        stats.bestHour?.let { stringResource(R.string.prog_best_hour, it, (it + 1) % 24) },
    )
    BarChart(
        values = stats.byHour,
        labels = (0..23).map { if (it % 6 == 0) resources.getString(R.string.prog_hour_axis, it) else "" },
        describe = { i -> value(hourRange(i), stats.byHour[i]) },
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
    val explain = stringResource(R.string.prog_places_explain)
    when (ui) {
        PlacesUi.NotPro -> {
            Text(explain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onOpenPro) { Text(stringResource(R.string.prog_places_pro)) }
        }
        PlacesUi.Disabled -> {
            Text(explain, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) {
                Text(stringResource(R.string.prog_places_enable))
            }
        }
        is PlacesUi.Enabled -> {
            if (ui.places.isEmpty()) {
                Text(
                    stringResource(R.string.prog_places_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    stringResource(R.string.prog_places_tap),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val max = ui.places.maxOf { it.workSeconds }.coerceAtLeast(1)
                ui.places.forEach { PlaceRow(it, max, onRename) }
            }
            TextButton(onClick = onDisable) { Text(stringResource(R.string.prog_places_disable)) }
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
                stringResource(R.string.prog_place_stats, place.completed * 100 / total, total),
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
                }) { Text(stringResource(R.string.prog_save)) }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text(stringResource(R.string.prog_cancel)) } },
            title = { Text(stringResource(R.string.prog_place_name)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.prog_place_hint), textAlign = TextAlign.Start) },
                )
            },
        )
    }
}
