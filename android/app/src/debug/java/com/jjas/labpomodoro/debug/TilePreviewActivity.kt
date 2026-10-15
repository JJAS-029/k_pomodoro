package com.jjas.labpomodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.color
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

/**
 * Propuestas de color para la tabla y las tarjetas. Extra "variant": 0 actual, 1 Joya, 2 Vial,
 * 3 Neón; "light" = true: tema claro.
 */
class TilePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val light = intent.getBooleanExtra("light", false)
        val variant = intent.getIntExtra("variant", 0)
        setContent {
            LabPomodoroTheme(darkTheme = !light) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier.safeDrawingPadding().padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(NAMES[variant], style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        MiniTable { z, size -> Tile(variant, PeriodicTable[z], size, light) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(11, 26, 53, 79).forEach { Tile(variant, PeriodicTable[it], 76.dp, light) }
                        }
                        Legend(variant, light)
                    }
                }
            }
        }
    }
}

private val NAMES = listOf("Actual", "A · Joya", "B · Vial", "C · Neón")

/** Periodos 1–4 y los primeros lantánidos: lo justo para ver cómo conviven los colores. */
@Composable
private fun MiniTable(tile: @Composable (Int, Dp) -> Unit) {
    val cell = 19.dp
    val gap = 2.dp
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        for (period in 1..4) {
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (group in 1..18) {
                    val z = (1..36).firstOrNull { PeriodicTable[it].group == group && PeriodicTable[it].period == period }
                    if (z != null) tile(z, cell) else Spacer(Modifier.size(cell))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.width((cell + gap) * 2))
            (57..71).forEach { tile(it, cell) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Spacer(Modifier.width((cell + gap) * 2))
            (89..103).forEach { tile(it, cell) }
        }
    }
}

/** Matiz de cada familia, repartidos para que ninguna se parezca a su vecina. */
private fun ElementCategory.hue(): Float = when (this) {
    ElementCategory.ALKALI_METAL -> 2f
    ElementCategory.ALKALINE_EARTH_METAL -> 28f
    ElementCategory.TRANSITION_METAL -> 212f
    ElementCategory.POST_TRANSITION_METAL -> 192f
    ElementCategory.METALLOID -> 96f
    ElementCategory.NONMETAL -> 162f
    ElementCategory.HALOGEN -> 46f
    ElementCategory.NOBLE_GAS -> 274f
    ElementCategory.LANTHANIDE -> 326f
    ElementCategory.ACTINIDE -> 246f
    ElementCategory.UNKNOWN -> 220f
}

private fun ElementCategory.sat(): Float = when (this) {
    ElementCategory.POST_TRANSITION_METAL -> 0.28f
    ElementCategory.UNKNOWN -> 0.06f
    else -> 0.72f
}

/** Avance de maestría de mentira, para ver el nivel del líquido en la variante Vial. */
private fun fakeProgress(z: Int) = ((z * 37) % 100) / 100f * 0.8f + 0.15f

@Composable
private fun Tile(variant: Int, element: Element, size: Dp, light: Boolean) {
    if (variant == 0) {
        ElementTile(element, discovered = true, size = size, quantity = if (size > 40.dp) 5 else 0, progress = if (size > 40.dp) 0.6f else null)
        return
    }
    val c = element.category
    val h = c.hue()
    val s = c.sat()
    val shape = RoundedCornerShape(size * 0.18f)
    val big = size > 40.dp
    val symbolSize = (size.value * if (big) 0.36f else 0.46f).sp
    val modifier = Modifier.size(size).clip(shape)
    when (variant) {
        // A · Joya: degradado del mismo matiz con igual luminosidad entre familias; la sombra gira
        // hacia el rojo en los cálidos (el amarillo se vuelve ámbar, nunca olivo)
        1 -> {
            val warm = h < 70f
            val top = Color.hsl(h, s, if (light) 0.58f else 0.62f)
            val deep = Color.hsl(if (warm) (h - 18f + 360f) % 360f else h + 8f, (s + 0.1f).coerceAtMost(1f), if (light) 0.36f else 0.3f)
            Box(
                modifier
                    .background(Brush.linearGradient(listOf(top, deep)))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
                    .drawBehind {
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), endY = this.size.height * 0.45f),
                            cornerRadius = CornerRadius(this.size.minDimension * 0.18f),
                        )
                    },
                contentAlignment = Alignment.Center,
            ) { TileText(element, big, symbolSize, Color.White, Color.hsl(h, s, 0.15f, 0.7f)) }
        }
        // B · Vial: cristal neutro con el líquido de la familia; el nivel es la maestría
        2 -> {
            val glass = if (light) Color(0xFFE9ECF1) else Color(0xFF16181D)
            val liquidTop = Color.hsl(h, s, if (light) 0.6f else 0.6f)
            val liquidDeep = Color.hsl(h, (s + 0.1f).coerceAtMost(1f), if (light) 0.4f else 0.34f)
            val level = fakeProgress(element.atomicNumber)
            Box(
                modifier
                    .background(glass)
                    .drawBehind {
                        val y = this.size.height * (1f - level)
                        drawRect(Brush.verticalGradient(listOf(liquidTop, liquidDeep), startY = y, endY = this.size.height), topLeft = Offset(0f, y))
                        // Menisco: una línea de luz en la superficie
                        drawRect(Color.White.copy(alpha = 0.55f), topLeft = Offset(0f, y), size = Size(this.size.width, this.size.height * 0.03f))
                        // Reflejo vertical del vidrio
                        drawRect(Color.White.copy(alpha = 0.12f), topLeft = Offset(this.size.width * 0.12f, 0f), size = Size(this.size.width * 0.08f, this.size.height))
                    }
                    .border(1.dp, Color.hsl(h, s, if (light) 0.55f else 0.65f, 0.7f), shape),
                contentAlignment = Alignment.Center,
            ) { TileText(element, big, symbolSize, if (light) Color(0xFF1B1C1E) else Color.White, if (light) Color.White else Color.Black.copy(alpha = 0.6f)) }
        }
        // C · Neón: casi negro, borde luminoso y el símbolo del color de la familia (AMOLED)
        else -> {
            val neon = Color.hsl(h, (s + 0.2f).coerceAtMost(1f), if (light) 0.42f else 0.66f)
            Box(
                modifier
                    .background(if (light) Color.White else Color(0xFF07080B))
                    .drawBehind {
                        drawRoundRect(
                            Brush.radialGradient(listOf(neon.copy(alpha = 0.28f), Color.Transparent), center = Offset(this.size.width / 2, this.size.height), radius = this.size.width),
                        )
                        drawRoundRect(neon, style = Stroke(1.5.dp.toPx()), cornerRadius = CornerRadius(this.size.minDimension * 0.18f))
                    },
                contentAlignment = Alignment.Center,
            ) { TileText(element, big, symbolSize, neon, neon.copy(alpha = 0.6f)) }
        }
    }
}

@Composable
private fun TileText(element: Element, big: Boolean, symbolSize: androidx.compose.ui.unit.TextUnit, color: Color, glow: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (big) Text(element.atomicNumber.toString(), fontSize = 10.sp, color = color.copy(alpha = 0.8f))
        Text(
            element.symbol,
            style = TextStyle(fontSize = symbolSize, fontWeight = FontWeight.Bold, color = color, shadow = Shadow(glow, blurRadius = 8f)),
        )
    }
}

@Composable
private fun Legend(variant: Int, light: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        ElementCategory.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { c ->
                    val color = if (variant == 0) c.color() else Color.hsl(c.hue(), c.sat(), if (light) 0.5f else 0.62f)
                    Row(
                        Modifier.border(1.dp, color.copy(alpha = 0.4f), CircleShape).padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(14.dp).drawBehind {
                                val r = this.size.minDimension / 2
                                drawCircle(Brush.radialGradient(listOf(Color.White, color, color.copy(red = color.red * 0.4f, green = color.green * 0.4f, blue = color.blue * 0.4f)), center = Offset(r * 0.7f, r * 0.6f), radius = r * 1.5f))
                                drawCircle(Color.White.copy(alpha = 0.8f), radius = r - 1f, style = Stroke(1f))
                                drawOval(Color.White.copy(alpha = 0.9f), topLeft = Offset(r * 0.45f, r * 0.38f), size = Size(r * 0.55f, r * 0.34f))
                            },
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(c.name.lowercase().replace('_', ' '), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
