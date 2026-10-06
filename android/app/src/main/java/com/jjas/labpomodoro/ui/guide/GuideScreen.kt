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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.ElementTile
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
                    "Cómo funciona",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = finish) { Text("Saltar") }
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
                    Text(if (last) "Empezar" else "Siguiente")
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
    PageTitle("Tu laboratorio de enfoque")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VesselView(VesselShape.ERLENMEYER, 0.6f, WorkColor, Color.Transparent, LiquidEffect.VAPOR, animate = true, modifier = Modifier.weight(1f).width(130.dp))
            Text("Trabajo", color = WorkColor, style = MaterialTheme.typography.titleMedium)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            VesselView(
                VesselShape.ROUND_FLASK, 0.4f, ShortColor,
                LiquidPalette.bubble(SessionType.SHORT_BREAK, SEED, 1), LiquidEffect.BUBBLES, animate = true,
                modifier = Modifier.weight(1f).width(130.dp),
            )
            Text("Descanso", color = ShortColor, style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(20.dp))
    Body(
        "Mientras trabajas, el líquido se evapora poco a poco. En el descanso, el recipiente se vuelve a llenar.\n\n" +
            "Cada sesión del plan usa un recipiente y un color distintos."
    )
}

@Composable
private fun SymbolsPage() {
    PageTitle("Qué estás haciendo y qué sigue")
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
        description = "Ejemplo del indicador de sesión",
    )
    Spacer(Modifier.height(20.dp))
    LegendRow({ LegendIcon(SessionType.WORK.icon(), WorkColor) }, "Trabajo (pomodoro)")
    LegendRow({ LegendIcon(SessionType.SHORT_BREAK.icon(), ShortColor) }, "Descanso corto")
    LegendRow({ LegendIcon(SessionType.LONG_BREAK.icon(), LongColor) }, "Descanso largo")
    LegendRow({ LegendIcon(R.drawable.ic_check, MaterialTheme.colorScheme.primary) }, "Fin del plan")
    LegendRow({ Text("5′", style = MaterialTheme.typography.titleMedium) }, "Minutos de lo que sigue")
    Spacer(Modifier.height(8.dp))
    LegendRow({ PlanDotView(PlanDot(DotState.DONE, WorkColor, false), 12.dp) }, "Pomodoro terminado")
    LegendRow({ PlanDotView(PlanDot(DotState.CURRENT, WorkColor, false), 12.dp) }, "Pomodoro en curso")
    LegendRow({ PlanDotView(PlanDot(DotState.PENDING, WorkColor, false), 12.dp) }, "Pomodoro pendiente")
    LegendRow({ PlanDotView(PlanDot(DotState.SKIPPED, WorkColor, false), 12.dp) }, "Pomodoro saltado")
    Body("Un espacio entre puntos marca un descanso largo.")
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
    PageTitle("Controles")
    LegendRow({ ControlButton(R.drawable.ic_pause, highlighted = true) }, "Pausar o continuar")
    LegendRow({ ControlButton(R.drawable.ic_skip) }, "Saltar a la siguiente sesión (no cuenta como completada)")
    LegendRow({ ControlButton(R.drawable.ic_stop) }, "Detener el plan")
    LegendRow({ ControlButton(R.drawable.ic_chevron_up) }, "Abrir el menú")
    Spacer(Modifier.height(12.dp))
    Text("En el menú", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    LegendRow({ LegendIcon(R.drawable.ic_tune, MaterialTheme.colorScheme.onSurface) }, "Config: horas, duraciones y avisos")
    LegendRow({ LegendIcon(R.drawable.ic_star, MaterialTheme.colorScheme.onSurface) }, "Logros y tabla periódica")
    LegendRow({ LegendIcon(R.drawable.ic_moon, MaterialTheme.colorScheme.onSurface) }, "Modo ambiente al instante (Pro)")
    LegendRow({ LegendIcon(R.drawable.ic_help, MaterialTheme.colorScheme.onSurface) }, "Volver a ver esta guía")
    Body("Los mismos controles aparecen en la notificación.")
}

@Composable
private fun ShelfAndAmbientPage() {
    PageTitle("Repisa, modo ambiente y ventana flotante")
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
    Body("La repisa guarda la cristalería de todo tu plan: cada recipiente se llena al completar su sesión. Los punteados son sesiones saltadas.")
    Spacer(Modifier.height(24.dp))
    LegendRow({ LegendIcon(R.drawable.ic_moon, MaterialTheme.colorScheme.secondary) }, "Modo ambiente (Pro): si no tocas la pantalla (o con 🌙 en el menú), quedan solo el recipiente, el tiempo y lo que sigue, atenuados. Tócala para volver.")
    LegendRow({ LegendIcon(R.drawable.ic_stat_timer, MaterialTheme.colorScheme.tertiary) }, "Si sales de la app, el timer sigue en una ventana flotante y en la notificación.")
}

@Composable
private fun ElementsPage() {
    PageTitle("Colecciona la tabla periódica")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        ElementTile(PeriodicTable[1], discovered = true, size = 48.dp)
        Text("+", style = MaterialTheme.typography.titleLarge)
        ElementTile(PeriodicTable[92], discovered = true, size = 48.dp)
        Text("→", style = MaterialTheme.typography.titleLarge)
        ElementTile(PeriodicTable[93], discovered = true, size = 48.dp)
    }
    Spacer(Modifier.height(16.dp))
    Body("Tu tiempo de enfoque se convierte en elementos: cada 25 min completados ganas uno básico y cada 60 min uno raro. Saltar una sesión no cuenta.")
    Spacer(Modifier.height(12.dp))
    Body("En ★ Logros › Sintetizador fusiona dos elementos: sus números atómicos se suman (H 1 + U 92 → Np 93). Es la única forma de conseguir los sintéticos.")
    Spacer(Modifier.height(12.dp))
    Body("Tus elementos llenan los recipientes de trabajo y se comportan como en la realidad: el sodio burbujea, el neón brilla, el mercurio refleja como metal. Toca uno en la tabla para saber qué es y para qué sirve.")
    Spacer(Modifier.height(12.dp))
    LegendRow({ LegendIcon(R.drawable.ic_headphones, MaterialTheme.colorScheme.secondary) }, "Sonido (Pro): ruido blanco, rosa o café, lluvia u olas mientras trabajas.")
}

/** Saludo para quien abre la app por primera vez. */
@Composable
private fun WelcomePage() {
    Image(
        painterResource(R.drawable.koala_mascot),
        contentDescription = "Koala, la mascota de Lab Pomodoro",
        modifier = Modifier
            .size(120.dp)
            .clip(RoundedCornerShape(28.dp)),
    )
    Spacer(Modifier.height(20.dp))
    PageTitle("¡Hola! Te damos la bienvenida a tu laboratorio")
    Body(
        "Aquí cada sesión de enfoque es un experimento. Mientras trabajas, el líquido se evapora; " +
            "al descansar, el recipiente se vuelve a llenar."
    )
    Spacer(Modifier.height(12.dp))
    Body(
        "Con tu tiempo de enfoque ganas elementos de la tabla periódica, y con el tiempo descubrirás " +
            "a qué hora y qué días rindes más."
    )
    Spacer(Modifier.height(12.dp))
    Body("Te explico cómo funciona en unos pasos. Si ya lo conoces, toca Saltar.")
}

