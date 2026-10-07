package com.jjas.labpomodoro.ui.guide

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.League
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.KoalaAvatar
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.MiniVessel
import com.jjas.labpomodoro.ui.components.PlanDot
import com.jjas.labpomodoro.ui.components.PlanDotView
import com.jjas.labpomodoro.ui.components.SessionIndicator
import com.jjas.labpomodoro.ui.components.UpNext
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.VesselView
import com.jjas.labpomodoro.ui.components.icon
import com.jjas.labpomodoro.ui.medals.MedalBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {
    fun markSeen() {
        viewModelScope.launch { settings.setGuideSeen() }
    }
}

private const val SEED = 7L
private val WorkColor = LiquidPalette.liquid(SessionType.WORK, SEED, 0)
private val ShortColor = LiquidPalette.liquid(SessionType.SHORT_BREAK, SEED, 1)
private val LongColor = LiquidPalette.liquid(SessionType.LONG_BREAK, SEED, 7)

/**
 * Guía de uso: explica la simbología con las mismas piezas que se ven en la app.
 * Se abre sola la primera vez y después desde el dock (Guía).
 */
@Composable
fun GuideScreen(onDone: () -> Unit, viewModel: GuideViewModel = hiltViewModel()) {
    val pages: List<@Composable () -> Unit> = listOf(
        { WelcomePage() },
        { LabPage() },
        { SymbolsPage() },
        { ControlsPage() },
        { ShelfAndAmbientPage() },
        { ElementsPage() },
        { ProgressPage() },
    )
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val finish = {
        viewModel.markSeen()
        onDone()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.prog_guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = finish) { Text(stringResource(R.string.prog_guide_skip)) }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { pages[page]() }
            }

            // Indicador de página y botón
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages.size) { i ->
                        Box(
                            Modifier
                                .size(if (i == pager.currentPage) 10.dp else 7.dp)
                                .background(
                                    if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                )
                        )
                    }
                }
                val last = pager.currentPage == pages.lastIndex
                Button(onClick = { if (last) finish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(if (last) R.string.prog_guide_start else R.string.prog_guide_next))
                }
            }
        }
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Fila de leyenda: símbolo a la izquierda, explicación a la derecha. */
@Composable
private fun LegendRow(symbol: @Composable () -> Unit, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { symbol() }
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun LegendIcon(icon: Int, tint: Color) {
    Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
}

@Composable
private fun LabPage() {
    PageTitle(stringResource(R.string.prog_guide_lab_title))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VesselView(VesselShape.ERLENMEYER, 0.6f, WorkColor, Color.Transparent, LiquidEffect.VAPOR, animate = true, modifier = Modifier.weight(1f).width(130.dp))
            Text(stringResource(R.string.prog_guide_work), color = WorkColor, style = MaterialTheme.typography.titleMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VesselView(
                VesselShape.ROUND_FLASK, 0.4f, ShortColor,
                LiquidPalette.bubble(SessionType.SHORT_BREAK, SEED, 1), LiquidEffect.BUBBLES, animate = true,
                modifier = Modifier.weight(1f).width(130.dp),
            )
            Text(stringResource(R.string.prog_guide_break), color = ShortColor, style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(20.dp))
    Body(stringResource(R.string.prog_guide_lab_body))
}

@Composable
private fun SymbolsPage() {
    PageTitle(stringResource(R.string.prog_guide_symbols_title))
    SessionIndicator(
        current = SessionType.WORK,
        currentColor = WorkColor,
        next = UpNext(SessionType.SHORT_BREAK, ShortColor, 5),
        dots = listOf(
            PlanDot(DotState.DONE, WorkColor, cycleEnd = false),
            PlanDot(DotState.CURRENT, WorkColor, cycleEnd = false),
            PlanDot(DotState.PENDING, WorkColor, cycleEnd = false),
            PlanDot(DotState.PENDING, WorkColor, cycleEnd = true),
            PlanDot(DotState.PENDING, WorkColor, cycleEnd = false),
        ),
        description = stringResource(R.string.prog_guide_indicator_desc),
    )
    Spacer(Modifier.height(20.dp))
    LegendRow({ LegendIcon(SessionType.WORK.icon(), WorkColor) }, stringResource(R.string.prog_guide_legend_work))
    LegendRow({ LegendIcon(SessionType.SHORT_BREAK.icon(), ShortColor) }, stringResource(R.string.prog_guide_legend_short_break))
    LegendRow({ LegendIcon(SessionType.LONG_BREAK.icon(), LongColor) }, stringResource(R.string.prog_guide_legend_long_break))
    LegendRow({ LegendIcon(R.drawable.ic_check, MaterialTheme.colorScheme.primary) }, stringResource(R.string.prog_guide_legend_plan_end))
    LegendRow({ Text("5′", style = MaterialTheme.typography.titleMedium) }, stringResource(R.string.prog_guide_legend_minutes))
    Spacer(Modifier.height(8.dp))
    LegendRow({ PlanDotView(PlanDot(DotState.DONE, WorkColor, false), 12.dp) }, stringResource(R.string.prog_guide_legend_done))
    LegendRow({ PlanDotView(PlanDot(DotState.CURRENT, WorkColor, false), 12.dp) }, stringResource(R.string.prog_guide_legend_current))
    LegendRow({ PlanDotView(PlanDot(DotState.PENDING, WorkColor, false), 12.dp) }, stringResource(R.string.prog_guide_legend_pending))
    LegendRow({ PlanDotView(PlanDot(DotState.SKIPPED, WorkColor, false), 12.dp) }, stringResource(R.string.prog_guide_legend_skipped))
    Body(stringResource(R.string.prog_guide_symbols_gap))
}

@Composable
private fun ControlButton(icon: Int, highlighted: Boolean = false) {
    Box(
        Modifier
            .size(44.dp)
            .background(
                if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = if (highlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ControlsPage() {
    PageTitle(stringResource(R.string.prog_guide_controls_title))
    LegendRow({ ControlButton(R.drawable.ic_pause, highlighted = true) }, stringResource(R.string.prog_guide_pause))
    LegendRow({ ControlButton(R.drawable.ic_skip) }, stringResource(R.string.prog_guide_skip_session))
    LegendRow({ ControlButton(R.drawable.ic_stop) }, stringResource(R.string.prog_guide_stop))
    LegendRow({ ControlButton(R.drawable.ic_chevron_up) }, stringResource(R.string.prog_guide_open_menu))
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.prog_guide_in_menu), style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    LegendRow({ LegendIcon(R.drawable.ic_tune, MaterialTheme.colorScheme.onSurface) }, stringResource(R.string.prog_guide_menu_settings))
    LegendRow({ LegendIcon(R.drawable.ic_star, MaterialTheme.colorScheme.onSurface) }, stringResource(R.string.prog_guide_menu_achievements))
    LegendRow({ LegendIcon(R.drawable.ic_chart, MaterialTheme.colorScheme.onSurface) }, stringResource(R.string.prog_guide_menu_progress))
    LegendRow({ LegendIcon(R.drawable.ic_moon, MaterialTheme.colorScheme.onSurface) }, stringResource(R.string.prog_guide_menu_ambient))
    LegendRow({ LegendIcon(R.drawable.ic_help, MaterialTheme.colorScheme.onSurface) }, stringResource(R.string.prog_guide_menu_help))
    Body(stringResource(R.string.prog_guide_controls_notification))
}

@Composable
private fun ShelfAndAmbientPage() {
    PageTitle(stringResource(R.string.prog_guide_shelf_title))
    Row(
        modifier = Modifier.height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val items = listOf(
            Triple(VesselShape.BEAKER, WorkColor, 1f),
            Triple(VesselShape.TEST_TUBE, ShortColor, 1f),
            Triple(VesselShape.ERLENMEYER, LiquidPalette.liquid(SessionType.WORK, SEED, 2), 0.5f),
            Triple(VesselShape.ROUND_FLASK, ShortColor, 0f),
        )
        items.forEachIndexed { i, (shape, color, fill) ->
            MiniVessel(
                shape, fill, color,
                outlineColor = when (i) {
                    2 -> color
                    3 -> Color.White.copy(alpha = 0.25f)
                    else -> Color(0xFFCCCCCC).copy(alpha = 0.7f)
                },
                // El último ejemplo es una sesión saltada
                dashed = i == 3,
                modifier = Modifier.size(width = 56.dp * shape.aspect + 4.dp, height = 56.dp),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_shelf_body))
    Spacer(Modifier.height(24.dp))
    LegendRow({ LegendIcon(R.drawable.ic_moon, MaterialTheme.colorScheme.secondary) }, stringResource(R.string.prog_guide_ambient))
    LegendRow({ LegendIcon(R.drawable.ic_stat_timer, MaterialTheme.colorScheme.tertiary) }, stringResource(R.string.prog_guide_floating))
}

@Composable
private fun ElementsPage() {
    PageTitle(stringResource(R.string.prog_guide_elements_title))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ElementTile(PeriodicTable[1], discovered = true, size = 48.dp)
        Text("+", style = MaterialTheme.typography.titleLarge)
        ElementTile(PeriodicTable[92], discovered = true, size = 48.dp)
        Text("→", style = MaterialTheme.typography.titleLarge)
        ElementTile(PeriodicTable[93], discovered = true, size = 48.dp)
    }
    Spacer(Modifier.height(16.dp))
    Body(stringResource(R.string.prog_guide_elements_body1))
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_elements_body2))
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_elements_body3))
    Spacer(Modifier.height(12.dp))
    LegendRow({ LegendIcon(R.drawable.ic_headphones, MaterialTheme.colorScheme.secondary) }, stringResource(R.string.prog_guide_sound))
}

@Composable
private fun ProgressPage() {
    PageTitle(stringResource(R.string.prog_guide_progress_title))
    // La misma pieza con cada nivel de maestría
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Mastery.DISCOVERED, Mastery.BRONZE, Mastery.SILVER, Mastery.GOLD).forEach {
            ElementTile(PeriodicTable[79], discovered = true, size = 48.dp, mastery = it)
        }
    }
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_mastery_body))
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(Medal.STREAK_3, Medal.MARATHON, Medal.HOURS_100).forEach { MedalBadge(MedalProgress(it, it.target), 48.dp) }
        MedalBadge(MedalProgress(Medal.STREAK_30, 12), 48.dp)
    }
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_medals_body))
    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(League.HYDROGEN, League.NEON, League.GOLD, League.PLATINUM).forEach { LeagueChip(it) }
    }
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_league_body))
}

@Composable
private fun LeagueChip(league: League) {
    Box(
        Modifier
            .size(48.dp)
            .background(Color(league.color), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(league.symbol, style = MaterialTheme.typography.titleMedium, color = Color.Black.copy(alpha = 0.8f))
    }
}

/** Saludo para quien abre la app por primera vez. */
@Composable
private fun WelcomePage() {
    KoalaAvatar()
    Spacer(Modifier.height(20.dp))
    PageTitle(stringResource(R.string.prog_guide_welcome_title))
    Body(stringResource(R.string.prog_guide_welcome_body1))
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_welcome_body2))
    Spacer(Modifier.height(12.dp))
    Body(stringResource(R.string.prog_guide_welcome_body3))
}

