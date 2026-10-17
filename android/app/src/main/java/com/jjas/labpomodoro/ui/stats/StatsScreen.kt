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
import com.jjas.labpomodoro.domain.usecase.ChallengeKind
import com.jjas.labpomodoro.domain.usecase.ChallengeProgress
import java.time.ZoneId
import java.time.Instant
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import java.time.format.FormatStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Canvas
import com.jjas.labpomodoro.domain.usecase.HeatDay
import com.jjas.labpomodoro.ui.league.LeaguePanel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import com.jjas.labpomodoro.domain.model.TagColors
import com.jjas.labpomodoro.data.local.entity.TagTotal
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.ui.medals.MedalDialog
import com.jjas.labpomodoro.ui.medals.MedalGrid
import com.jjas.labpomodoro.ui.medals.MedalFilter
import com.jjas.labpomodoro.ui.medals.MedalHeader
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
fun StatsScreen(onBack: () -> Unit, onOpenPro: () -> Unit, initialTab: Int = 0, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }

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
                Spacer(Modifier.height(8.dp))
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.prog_tab_stats)) })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.prog_tab_medals)) })
                    Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.el_lab_tab_league)) })
                }
                Spacer(Modifier.height(16.dp))

                val current = state
                if (tab == 2) {
                    LeaguePanel()
                } else if (current == null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else if (tab == 1) {
                    LaunchedEffect(current.medals) { viewModel.markMedalsSeen(current.medals) }
                    Challenges(current.challenges, current.weekDaysLeft)
                    Spacer(Modifier.height(8.dp))
                    Medals(current.medals)
                } else {
                    RangeSelector(range, viewModel::setRange)
                    Spacer(Modifier.height(16.dp))
                    Summary(current.stats, current.streak.current)
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
                    Section(
                        stringResource(R.string.heat_section),
                        pluralStringResource(R.plurals.heat_subtitle, current.activeDays, current.activeDays),
                    )
                    YearHeatmapView(current.year)
                    Section(stringResource(R.string.tag_stats_section), stringResource(R.string.tag_stats_subtitle))
                    Tags(current.tags)
                    if (current.notes.isNotEmpty()) {
                        Section(stringResource(R.string.note_section), stringResource(R.string.note_section_subtitle))
                        Notes(current.notes)
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
    var filter by rememberSaveable { mutableStateOf(MedalFilter.ALL) }
    Section(
        stringResource(R.string.prog_medals),
        stringResource(R.string.prog_medals_subtitle, medals.count { it.earned }, medals.size),
    )
    MedalHeader(medals, filter, onFilter = { filter = it })
    Spacer(Modifier.height(16.dp))
    MedalGrid(medals, onSelect = { selected = it }, filter = filter)
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

/** Enfoque por etiqueta: tiempo, pomodoros y qué parte del total fue cada una. */
@Composable
private fun Tags(tags: List<TagTotal>) {
    if (tags.isEmpty()) {
        Text(
            stringResource(R.string.tag_stats_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val total = tags.sumOf { it.workSeconds }.coerceAtLeast(1)
    tags.forEach { tag ->
        val color = Color(TagColors.of(tag.color))
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(tag.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(formatFocus(tag.workSeconds), style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { tag.workSeconds.toFloat() / total },
                color = color,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                pluralStringResource(R.plurals.tag_stats_row, tag.completed, tag.completed, tag.workSeconds * 100 / total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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

/**
 * Mapa de calor del último año, al estilo de GitHub: una columna por semana (lunes arriba) y un
 * cuadro por día, más intenso entre más enfoque. Arranca mostrando lo más reciente; al tocar un
 * día se ve su fecha y sus minutos.
 */
@Composable
private fun YearHeatmapView(weeks: List<List<HeatDay?>>) {
    if (weeks.isEmpty()) return
    val locale = LocalConfiguration.current.locales[0]
    val accent = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val levels = listOf(empty, accent.copy(alpha = 0.3f), accent.copy(alpha = 0.5f), accent.copy(alpha = 0.75f), accent)
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    var selected by remember { mutableStateOf<HeatDay?>(null) }
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val cell = 13.dp
    val gap = 3.dp
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("MMM", locale) }
    Row {
        // Lunes, miércoles y viernes, como referencia
        Column(Modifier.padding(top = 18.dp, end = 4.dp)) {
            DayOfWeek.entries.forEachIndexed { i, day ->
                Box(Modifier.height(cell + gap), contentAlignment = Alignment.CenterStart) {
                    if (i % 2 == 0 && i < 6) Text(day.getDisplayName(TextStyle.NARROW, locale), style = labelStyle)
                }
            }
        }
        Box(Modifier.horizontalScroll(scroll)) {
            Canvas(
                Modifier
                    .width((cell + gap) * weeks.size)
                    .height(18.dp + (cell + gap) * 7)
                    .pointerInput(weeks) {
                        detectTapGestures { pos ->
                            val step = (cell + gap).toPx()
                            val w = (pos.x / step).toInt()
                            val d = ((pos.y - 18.dp.toPx()) / step).toInt()
                            selected = weeks.getOrNull(w)?.getOrNull(d)
                        }
                    },
            ) {
                val step = (cell + gap).toPx()
                val size = cell.toPx()
                val top = 18.dp.toPx()
                weeks.forEachIndexed { w, week ->
                    // Nombre del mes en la semana donde empieza
                    val first = week.firstOrNull()?.date
                    if (first != null && first.dayOfMonth <= 7) {
                        drawText(measurer, monthFormat.format(first).trimEnd('.'), Offset(w * step, 0f), labelStyle)
                    }
                    week.forEachIndexed { d, day ->
                        if (day != null) {
                            drawRoundRect(
                                levels[day.level],
                                topLeft = Offset(w * step, top + d * step),
                                size = Size(size, size),
                                cornerRadius = CornerRadius(size * 0.25f),
                            )
                            if (day == selected) {
                                drawRoundRect(
                                    labelColor,
                                    topLeft = Offset(w * step, top + d * step),
                                    size = Size(size, size),
                                    cornerRadius = CornerRadius(size * 0.25f),
                                    style = Stroke(1.5.dp.toPx()),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    val day = selected
    Text(
        if (day == null) {
            stringResource(R.string.heat_tap)
        } else {
            stringResource(
                R.string.heat_day,
                day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                formatFocus(day.minutes * 60L),
            )
        },
        style = MaterialTheme.typography.bodySmall,
        color = labelColor,
    )
}

/** Diario: las notas de logro más recientes, con su fecha y hora. */
@Composable
private fun Notes(notes: List<SessionEntity>) {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale) }
    notes.forEach { session ->
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(session.note.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            Text(
                Instant.ofEpochMilli(session.startedAtMillis).atZone(ZoneId.systemDefault()).format(format),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Retos de la semana: su avance y el premio (un elemento raro). */
@Composable
private fun Challenges(challenges: List<ChallengeProgress>, daysLeft: Int) {
    if (challenges.isEmpty()) return
    Section(stringResource(R.string.challenge_section), pluralStringResource(R.plurals.challenge_days_left, daysLeft, daysLeft))
    challenges.forEach { challenge ->
        val target = challenge.kind.target
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    pluralStringResource(challenge.kind.titleRes(), target, target),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (challenge.done) "✓" else "${challenge.current.coerceAtMost(target)}/$target",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (challenge.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { challenge.fraction }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(if (challenge.done) R.string.challenge_done else R.string.challenge_reward),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun ChallengeKind.titleRes(): Int = when (this) {
    ChallengeKind.EARLY_BIRD -> R.plurals.challenge_early
    ChallengeKind.STEADY_DAYS -> R.plurals.challenge_days
    ChallengeKind.FOCUS_MINUTES -> R.plurals.challenge_minutes
    ChallengeKind.BIG_DAY -> R.plurals.challenge_big_day
    ChallengeKind.TAGGED -> R.plurals.challenge_tagged
    ChallengeKind.WEEKEND -> R.plurals.challenge_weekend
}
