package com.jjas.labpomodoro.ui.lab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.data.repository.InventoryItem
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.domain.model.MasteryRules
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.Rarity
import com.jjas.labpomodoro.domain.model.labelRes
import com.jjas.labpomodoro.domain.model.rarity
import com.jjas.labpomodoro.domain.usecase.Fusion
import com.jjas.labpomodoro.domain.usecase.RewardSchedule
import com.jjas.labpomodoro.ui.components.BronzeColor
import com.jjas.labpomodoro.ui.components.Celebration
import com.jjas.labpomodoro.ui.components.ConfettiBurst
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.GoldColor
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.SilverColor
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.VesselView
import com.jjas.labpomodoro.ui.components.color
import com.jjas.labpomodoro.ui.components.elementDiscovery
import com.jjas.labpomodoro.ui.components.elementFact
import com.jjas.labpomodoro.ui.components.elementLookOrigin
import com.jjas.labpomodoro.ui.components.localizedName
import com.jjas.labpomodoro.ui.components.look
import com.jjas.labpomodoro.ui.components.metalColor
import com.jjas.labpomodoro.ui.league.LeaguePanel
import com.jjas.labpomodoro.ui.promo.ShareText
import com.jjas.labpomodoro.ui.promo.shareText
import com.jjas.labpomodoro.ui.stats.formatFocus
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Logros: tabla periódica con lo descubierto, avance de recompensas y sintetizador. */
@Composable
fun LabScreen(onBack: () -> Unit, viewModel: LabViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val newOnes by viewModel.newAtomicNumbers.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var fused by remember { mutableStateOf<Element?>(null) }
    val fusionFailed = stringResource(R.string.el_lab_fusion_failed)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LabEvent.Fused -> fused = event.result
                LabEvent.FusionFailed -> snackbar.showSnackbar(fusionFailed)
            }
        }
    }
    // El resultado de la fusión se muestra un momento al centro
    LaunchedEffect(fused) {
        if (fused != null) {
            delay(1_800)
            fused = null
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Box(
                Modifier
                    .weight(1f)
                    .safeDrawingPadding()
            ) {
                val current = state
                if (current == null) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    LabContent(current, newOnes, onBack, viewModel::fuse, viewModel::setVesselElement)
                }
                // Ráfaga con los colores del elemento recién fabricado, una por fusión
                val burst = remember(fused) {
                    fused?.let { Celebration(System.nanoTime(), listOf(it.look().color, it.category.color())) }
                }
                ConfettiBurst(burst, Modifier.fillMaxSize())
                FusionFlash(fused, Modifier.align(Alignment.Center))
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
            }
            // Solo en la versión gratis
            AdBanner(Modifier.navigationBarsPadding())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabContent(
    state: LabUi,
    newOnes: Set<Int>,
    onBack: () -> Unit,
    onFuse: (Fusion.Recipe) -> Unit,
    onVesselElement: (Int) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    var target by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
    ) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.el_lab_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            val context = LocalContext.current
            val resources = LocalResources.current
            IconButton(onClick = {
                val counts = state.masteryCounts
                context.shareText(
                    ShareText.progress(
                        context = context,
                        discovered = state.discoveredCount,
                        total = PeriodicTable.SIZE,
                        bronze = counts[Mastery.BRONZE] ?: 0,
                        silver = counts[Mastery.SILVER] ?: 0,
                        gold = counts[Mastery.GOLD] ?: 0,
                        streak = state.streak.current,
                        focus = formatFocus(state.totalWorkSeconds, resources),
                    )
                )
            }) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.el_lab_share_progress))
            }
            TextButton(onClick = onBack) { Text(stringResource(R.string.el_lab_done)) }
        }
        Stats(state, Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        MasterySummary(state, Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp))
        RewardProgress(state.totalWorkSeconds, Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(16.dp))

        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.el_lab_tab_table)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.el_lab_tab_synthesizer)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.el_lab_tab_league)) })
        }
        Spacer(Modifier.height(16.dp))
        when (tab) {
            0 -> {
                PeriodicTableGrid(
                    state.items,
                    newOnes,
                    onSelect = { selected = it },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    mastery = state::mastery,
                )
                Spacer(Modifier.height(12.dp))
                Legend(Modifier.padding(horizontal = 24.dp))
            }
            1 -> Synthesizer(
                state = state,
                onTarget = { target = it },
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            else -> LeaguePanel(Modifier.padding(horizontal = 24.dp))
        }
    }

    selected?.let { z ->
        // La ficha es larga: se abre completa para no tener que arrastrarla
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            val item = state.items[z - 1]
            ElementDetail(
                item = item,
                canCraft = Fusion.recipesFor(z, state.quantities).isNotEmpty(),
                inVessels = state.vesselElement == z,
                obtained = state.obtained(z),
                onToggleVessel = { onVesselElement(if (state.vesselElement == z) 0 else z) },
                onSynthesize = {
                    target = z
                    tab = 1
                    selected = null
                },
            )
        }
    }

    target?.let { z ->
        ModalBottomSheet(onDismissRequest = { target = null }) {
            RecipesSheetContent(
                target = PeriodicTable[z],
                quantities = state.quantities,
                onFuse = {
                    onFuse(it)
                    target = null
                },
            )
        }
    }
}

@Composable
private fun Stats(state: LabUi, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("${state.discoveredCount}/${PeriodicTable.SIZE}", stringResource(R.string.el_stat_elements), Modifier.weight(1f))
        StatCard(
            pluralStringResource(R.plurals.el_stat_streak_days, state.streak.current, state.streak.current),
            stringResource(R.string.el_stat_streak_label, state.streak.longest),
            Modifier.weight(1f),
        )
        StatCard(
            formatFocus(state.totalWorkSeconds),
            stringResource(R.string.el_stat_focus_label),
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Cuánto falta para el siguiente elemento de cada tipo. */
@Composable
private fun RewardProgress(totalWorkSeconds: Long, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RewardBar(stringResource(R.string.el_reward_next_basic), totalWorkSeconds, RewardSchedule.BASIC_EVERY_SECONDS, Color(0xFF80CBC4))
        RewardBar(stringResource(R.string.el_reward_next_rare), totalWorkSeconds, RewardSchedule.RARE_EVERY_SECONDS, Color(0xFFCE93D8))
    }
}

@Composable
private fun RewardBar(label: String, totalWorkSeconds: Long, every: Long, color: Color) {
    val doneMinutes = totalWorkSeconds % every / 60
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                stringResource(R.string.el_reward_minutes, doneMinutes.toInt(), (every / 60).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { RewardSchedule.progress(totalWorkSeconds, every) },
            color = color,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Fila y columna de cada elemento: periodos 1–7 arriba y el bloque f (lantánidos y actínidos)
// en dos filas aparte, como en la tabla impresa
private val TABLE_POSITIONS: Map<Pair<Int, Int>, Element> = PeriodicTable.elements.associateBy { e ->
    when {
        e.group != null -> e.period to e.group
        e.category == ElementCategory.LANTHANIDE -> 9 to (e.atomicNumber - 57 + 3)
        else -> 10 to (e.atomicNumber - 89 + 3)
    }
}

private const val TABLE_COLUMNS = 18

@Composable
private fun PeriodicTableGrid(
    items: List<InventoryItem>,
    newOnes: Set<Int>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    mastery: (Element) -> Mastery = { Mastery.NONE },
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val gap = 2.dp
        // Siempre cabe completa: en vertical las casillas quedan compactas (solo el símbolo)
        // y el detalle se ve al tocar
        val cell: Dp = (maxWidth - gap * (TABLE_COLUMNS - 1)) / TABLE_COLUMNS
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (row in 1..10) {
                if (row == 8) {
                    Spacer(Modifier.height(cell / 3))
                    continue
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 1..TABLE_COLUMNS) {
                        val element = TABLE_POSITIONS[row to col]
                        if (element == null) {
                            FBlockMarker(row, col, cell)
                        } else {
                            val item = items[element.atomicNumber - 1]
                            ElementTile(
                                element = element,
                                discovered = item.discovered,
                                quantity = item.quantity,
                                size = cell,
                                highlighted = element.atomicNumber in newOnes,
                                mastery = mastery(element),
                                modifier = Modifier.clickable { onSelect(element.atomicNumber) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Hueco del grupo 3 en los periodos 6 y 7: ahí van los lantánidos y actínidos de abajo. */
@Composable
private fun FBlockMarker(row: Int, col: Int, cell: Dp) {
    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
        if (col == 3 && (row == 6 || row == 7)) {
            val color = if (row == 6) ElementCategory.LANTHANIDE.color() else ElementCategory.ACTINIDE.color()
            Box(
                Modifier
                    .size(cell / 3)
                    .background(color.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ElementCategory.entries.forEach { category ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(category.color(), RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(category.labelRes()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

@Composable
private fun ElementDetail(
    item: InventoryItem,
    canCraft: Boolean,
    inVessels: Boolean,
    onToggleVessel: () -> Unit,
    onSynthesize: () -> Unit,
    obtained: Int = 0,
) {
    val element = item.element
    val fact = elementFact(element.atomicNumber)
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ElementTile(element, discovered = item.discovered, quantity = item.quantity, size = 72.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(element.localizedName(), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.el_detail_subtitle, element.atomicNumber, stringResource(element.category.labelRes())),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(element.rarity.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = element.category.color(),
                )
            }
        }
        if (item.discovered) {
            Spacer(Modifier.height(16.dp))
            MasteryProgress(element, obtained)
        }
        Spacer(Modifier.height(16.dp))
        DiscoveryCard(element)
        Spacer(Modifier.height(16.dp))
        Text(fact.description, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.el_detail_uses_title), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(fact.uses, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        // Cómo se ve en el timer: es la recompensa visual de tenerlo
        val look = element.look()
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                VesselView(
                    shape = VesselShape.BEAKER,
                    fill = 0.7f,
                    liquidColor = look.color,
                    bubbleColor = Color.Transparent,
                    effect = LiquidEffect.VAPOR,
                    animate = item.discovered,
                    behavior = look.behavior,
                    modifier = Modifier.size(width = 56.dp, height = 72.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.el_detail_in_vessels), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(look.behavior.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val origin = elementLookOrigin(element.atomicNumber)
                    if (origin.isNotEmpty()) {
                        Text(
                            stringResource(R.string.el_detail_color_origin, origin.replaceFirstChar { it.lowercase(locale) }),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        if (item.discovered) {
            Spacer(Modifier.height(8.dp))
            if (inVessels) {
                OutlinedButton(onClick = onToggleVessel) { Text(stringResource(R.string.el_detail_remove_vessels)) }
            } else {
                FilledTonalButton(onClick = onToggleVessel) { Text(stringResource(R.string.el_detail_use_vessels)) }
            }
        }
        Spacer(Modifier.height(16.dp))
        val owned = when {
            !item.discovered -> stringResource(R.string.el_detail_not_discovered)
            item.quantity == 0 -> stringResource(R.string.el_detail_used_up)
            else -> pluralStringResource(R.plurals.el_detail_owned, item.quantity, item.quantity)
        }
        Text(owned, style = MaterialTheme.typography.bodyLarge)
        item.firstObtainedAtMillis?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE_FORMAT.withLocale(locale))
            Text(stringResource(R.string.el_detail_first_found, date), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when (element.rarity) {
                Rarity.BASIC -> stringResource(R.string.el_detail_source_basic)
                Rarity.RARE -> stringResource(R.string.el_detail_source_rare)
                Rarity.SYNTHETIC -> stringResource(R.string.el_detail_source_synthetic)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (canCraft) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onSynthesize) { Text(stringResource(R.string.el_detail_synthesize)) }
        }
    }
}

/** Cuántos elementos hay en bronce, plata y oro. */
@Composable
private fun MasterySummary(state: LabUi, modifier: Modifier = Modifier) {
    val counts = state.masteryCounts
    val bronze = counts[Mastery.BRONZE] ?: 0
    val silver = counts[Mastery.SILVER] ?: 0
    val gold = counts[Mastery.GOLD] ?: 0
    // Hasta descubrir algo en bronce no hay nada que contar
    if (bronze + silver + gold == 0 && state.discoveredCount < PeriodicTable.SIZE) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.el_mastery_title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MasteryCount(BronzeColor, stringResource(Mastery.BRONZE.labelRes), bronze)
        MasteryCount(SilverColor, stringResource(Mastery.SILVER.labelRes), silver)
        MasteryCount(GoldColor, stringResource(Mastery.GOLD.labelRes), gold)
    }
}

@Composable
private fun MasteryCount(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Box(
            Modifier
                .size(12.dp)
                .border(2.dp, color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Nivel de maestría del elemento y cuánto falta para el siguiente. */
@Composable
private fun MasteryProgress(element: Element, obtained: Int) {
    val level = MasteryRules.level(element, obtained)
    val next = MasteryRules.next(element, obtained)
    val thresholds = MasteryRules.thresholds(element)
    val levelColor = level.metalColor() ?: MaterialTheme.colorScheme.onSurfaceVariant
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.el_mastery_level_prefix), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(level.labelRes), style = MaterialTheme.typography.titleSmall, color = levelColor)
            Spacer(Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.el_mastery_obtained, obtained, obtained),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (obtained.toFloat() / thresholds[2]).coerceIn(0f, 1f) },
            // La barra toma el color de la meta que viene
            color = (next?.first ?: Mastery.GOLD).metalColor() ?: GoldColor,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = next?.let { (goal, left) ->
                val goalLabel = stringResource(goal.labelRes).lowercase(LocalConfiguration.current.locales[0])
                pluralStringResource(R.plurals.el_mastery_left, left, left, goalLabel)
            } ?: stringResource(R.string.el_mastery_maxed),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Ficha técnica: cuándo, quién y dónde se descubrió, y su lugar en la tabla. */
@Composable
private fun DiscoveryCard(element: Element) {
    val discovery = elementDiscovery(element.atomicNumber)
    val locale = LocalConfiguration.current.locales[0]
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (discovery.year != null) {
                FactRow(stringResource(R.string.el_fact_discovered_in), discovery.year.toString())
                FactRow(stringResource(R.string.el_fact_by), discovery.discoverers)
                discovery.country?.let { FactRow(stringResource(R.string.el_fact_where), it) }
            } else {
                val era = discovery.era ?: stringResource(R.string.el_era_antiquity)
                FactRow(stringResource(R.string.el_fact_discovered), stringResource(R.string.el_fact_known_since, era.lowercase(locale)))
            }
            val place = element.group?.let { stringResource(R.string.el_fact_group, it) }
                ?: if (element.category == ElementCategory.LANTHANIDE) {
                    stringResource(R.string.el_fact_lanthanides)
                } else {
                    stringResource(R.string.el_fact_actinides)
                }
            FactRow(stringResource(R.string.el_fact_in_table), stringResource(R.string.el_fact_position, place, element.period))
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Synthesizer(
    state: LabUi,
    onTarget: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (missing, owned) = state.craftable.partition { !state.items[it.atomicNumber - 1].discovered }
    Column(modifier) {
        Text(
            stringResource(R.string.el_synth_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        if (missing.isEmpty() && owned.isEmpty()) {
            Text(
                stringResource(R.string.el_synth_empty),
                style = MaterialTheme.typography.bodyLarge,
            )
            return@Column
        }
        // Primero lo que aún no se tiene: es lo que hace avanzar la tabla
        CraftableGroup(stringResource(R.string.el_synth_new), missing, state, onTarget)
        CraftableGroup(stringResource(R.string.el_synth_more), owned, state, onTarget)
    }
}

@Composable
private fun CraftableGroup(title: String, elements: List<Element>, state: LabUi, onTarget: (Int) -> Unit) {
    if (elements.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        elements.forEach { element ->
            val item = state.items[element.atomicNumber - 1]
            ElementTile(
                element = element,
                discovered = item.discovered,
                quantity = item.quantity,
                size = 44.dp,
                modifier = Modifier.clickable { onTarget(element.atomicNumber) },
            )
        }
    }
    Spacer(Modifier.height(20.dp))
}

/** Hoja con las formas de fabricar [target]. */
@Composable
private fun RecipesSheetContent(target: Element, quantities: Map<Int, Int>, onFuse: (Fusion.Recipe) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
    ) {
        val locale = LocalConfiguration.current.locales[0]
        Text(stringResource(R.string.el_recipes_title, target.localizedName().lowercase(locale)), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        val recipes = Fusion.recipesFor(target.atomicNumber, quantities)
        if (recipes.isEmpty()) {
            Text(stringResource(R.string.el_recipes_none), style = MaterialTheme.typography.bodyLarge)
        }
        recipes.take(MAX_RECIPES).forEach { recipe ->
            RecipeRow(recipe, quantities, onFuse = { onFuse(recipe) })
        }
    }
}

private const val MAX_RECIPES = 6

@Composable
private fun RecipeRow(recipe: Fusion.Recipe, quantities: Map<Int, Int>, onFuse: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ElementTile(recipe.a, discovered = true, quantity = quantities[recipe.a.atomicNumber] ?: 0, size = 40.dp)
        Text("+", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
        ElementTile(recipe.b, discovered = true, quantity = quantities[recipe.b.atomicNumber] ?: 0, size = 40.dp)
        Text("→", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
        ElementTile(recipe.result, discovered = true, size = 40.dp)
        Spacer(Modifier.weight(1f))
        Button(onClick = onFuse) { Text(stringResource(R.string.el_fuse)) }
    }
}

/** Destello al terminar una fusión: el elemento nuevo aparece grande al centro. */
@Composable
private fun FusionFlash(element: Element?, modifier: Modifier = Modifier) {
    // Conserva el último para que la animación de salida tenga qué mostrar
    var shown by remember { mutableStateOf(element) }
    if (element != null) shown = element
    AnimatedVisibility(
        visible = element != null,
        enter = scaleIn(initialScale = 0.4f) + fadeIn(),
        exit = scaleOut(targetScale = 1.2f) + fadeOut(),
        modifier = modifier,
    ) {
        val e = shown ?: return@AnimatedVisibility
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, e.category.color()),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ElementTile(e, discovered = true, size = 96.dp)
                Spacer(Modifier.height(12.dp))
                val locale = LocalConfiguration.current.locales[0]
                Text(stringResource(R.string.el_fused, e.localizedName().lowercase(locale)), fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
