package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jjas.labpomodoro.ui.theme.isLight
import com.jjas.labpomodoro.domain.model.FlameColor
import kotlin.math.sin

/**
 * Colores de la llama: halo, cono exterior, núcleo y chispas. Azul por defecto (combustión
 * completa); los temas de laboratorio podrán cambiarla.
 */
class FlameTones(val halo: Color, val outer: Color, val outerBase: Color, val core: Color, val spark: Color) {
    companion object {
        val Blue = FlameTones(
            halo = Color(0xFF2979FF),
            outer = Color(0xFF448AFF),
            outerBase = Color(0xFF1E5BD8),
            core = Color(0xFFB3E5FC),
            spark = Color(0xFFE1F5FE),
        )

        fun of(color: FlameColor): FlameTones = when (color) {
            FlameColor.BLUE -> Blue
            FlameColor.GREEN -> FlameTones(Color(0xFF00E676), Color(0xFF4CFF8A), Color(0xFF12A150), Color(0xFFC8FFD9), Color(0xFFE6FFEE))
            FlameColor.VIOLET -> FlameTones(Color(0xFFB04DFF), Color(0xFFC77DFF), Color(0xFF7B2FD1), Color(0xFFEBD4FF), Color(0xFFF5EAFF))
            FlameColor.ORANGE -> FlameTones(Color(0xFFFF9100), Color(0xFFFFB347), Color(0xFFE06A00), Color(0xFFFFF0C2), Color(0xFFFFF8E1))
            FlameColor.RED -> FlameTones(Color(0xFFFF1744), Color(0xFFFF5A6E), Color(0xFFC2102E), Color(0xFFFFD3DA), Color(0xFFFFEBEE))
        }
    }
}

/**
 * Mechero Bunsen bajo el recipiente, al estilo del vidrio Liquid Glass: tubo metálico con reflejo,
 * anillo regulador de aire, manguera del gas y base con brillo. Durante el trabajo la llama se
 * enciende (explica por qué el líquido se evapora): halo, cono exterior, núcleo brillante que
 * parpadea, chispitas que suben y un resplandor que tiñe la base del recipiente. En los descansos
 * se apaga con suavidad.
 */
@Composable
fun BunsenBurner(lit: Boolean, modifier: Modifier = Modifier, tones: FlameTones = LocalLabTheme.current.flame) {
    val light = MaterialTheme.colorScheme.isLight
    val flame by animateFloatAsState(if (lit) 1f else 0f, tween(600), label = "flame")
    // Reloj continuo solo mientras está encendido: sin llama no pide cuadros
    val time = if (lit || flame > 0.01f) {
        val transition = rememberInfiniteTransition(label = "flicker")
        transition.animateFloat(0f, 1f, infiniteRepeatable(tween(FLICKER_MS, easing = LinearEasing), RepeatMode.Restart), label = "time").value
    } else {
        0f
    }

    Canvas(modifier) {
        val cx = size.width / 2f
        val h = size.height
        val tubeW = h * 0.26f
        val tubeTop = h * 0.45f
        drawBody(cx, h, tubeW, tubeTop, light)
        if (flame > 0.01f) drawFlame(cx, tubeW, tubeTop, flame, time, tones, light)
    }
}

/** Metal: oscuro en los bordes y con una franja de luz, como un cilindro pulido. */
private fun metal(light: Boolean): List<Color> = if (light) {
    listOf(Color(0xFF6E7681), Color(0xFFB8C0CA), Color(0xFFF2F5F8), Color(0xFF9AA3AE), Color(0xFF5E6670))
} else {
    listOf(Color(0xFF3A3F46), Color(0xFF8A929C), Color(0xFFE6EBF0), Color(0xFF6D757F), Color(0xFF2E3238))
}

private fun DrawScope.drawBody(cx: Float, h: Float, tubeW: Float, tubeTop: Float, light: Boolean) {
    val colors = metal(light)
    val baseTop = h * 0.86f
    val baseW = h * 1.3f
    // Sombra suave sobre la mesa
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (light) 0.18f else 0.45f), Color.Transparent), Offset(cx, h), baseW * 0.6f),
        topLeft = Offset(cx - baseW * 0.6f, h - h * 0.07f),
        size = Size(baseW * 1.2f, h * 0.14f),
    )
    // Manguera del gas, saliendo por la izquierda de la base
    val hose = Path().apply {
        moveTo(cx - tubeW * 0.5f, h * 0.8f)
        cubicTo(cx - tubeW * 1.6f, h * 0.8f, cx - baseW * 0.45f, h * 0.9f, cx - baseW * 0.72f, h * 0.97f)
    }
    drawPath(hose, if (light) Color(0xFFB07A55) else Color(0xFF8D5A3A), style = Stroke(width = h * 0.07f))
    drawPath(hose, Color.White.copy(alpha = 0.25f), style = Stroke(width = h * 0.02f))
    // Base: un disco achatado con brillo arriba
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - baseW / 2, endX = cx + baseW / 2),
        Offset(cx - baseW / 2, baseTop),
        Size(baseW, h * 0.12f),
        CornerRadius(h * 0.06f),
    )
    drawRoundRect(
        Color.White.copy(alpha = if (light) 0.55f else 0.35f),
        Offset(cx - baseW * 0.42f, baseTop + h * 0.012f),
        Size(baseW * 0.84f, h * 0.022f),
        CornerRadius(h * 0.011f),
    )
    // Tubo
    val tubeH = baseTop - tubeTop
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - tubeW / 2, endX = cx + tubeW / 2),
        Offset(cx - tubeW / 2, tubeTop),
        Size(tubeW, tubeH + h * 0.02f),
        CornerRadius(tubeW * 0.12f),
    )
    // Boca del tubo: un borde un poco más ancho
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - tubeW * 0.6f, endX = cx + tubeW * 0.6f),
        Offset(cx - tubeW * 0.6f, tubeTop - h * 0.015f),
        Size(tubeW * 1.2f, h * 0.05f),
        CornerRadius(h * 0.025f),
    )
    // Anillo regulador de aire, con sus orificios
    val collarTop = tubeTop + tubeH * 0.62f
    val collarH = tubeH * 0.2f
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - tubeW * 0.65f, endX = cx + tubeW * 0.65f),
        Offset(cx - tubeW * 0.65f, collarTop),
        Size(tubeW * 1.3f, collarH),
        CornerRadius(collarH * 0.3f),
    )
    val hole = Color.Black.copy(alpha = if (light) 0.45f else 0.7f)
    listOf(-0.28f, 0.28f).forEach { dx ->
        drawOval(hole, Offset(cx + tubeW * dx - tubeW * 0.1f, collarTop + collarH * 0.28f), Size(tubeW * 0.2f, collarH * 0.44f))
    }
    // Filo especular a la izquierda, como el borde del vidrio
    drawLine(
        Color.White.copy(alpha = if (light) 0.7f else 0.5f),
        Offset(cx - tubeW * 0.3f, tubeTop + h * 0.04f),
        Offset(cx - tubeW * 0.3f, collarTop - h * 0.02f),
        strokeWidth = tubeW * 0.08f,
    )
}

private fun DrawScope.drawFlame(cx: Float, tubeW: Float, tubeTop: Float, flame: Float, time: Float, tones: FlameTones, light: Boolean) {
    val phase = time * TWO_PI
    // Dos ritmos de parpadeo: la llama nunca se ve en bucle
    val pulse = 1f - 0.05f + 0.05f * sin(phase * 3f) + 0.025f * sin(phase * 7f + 1.3f)
    val flameH = tubeTop * 0.95f * flame * pulse
    val sway = tubeW * 0.12f * sin(phase * 2f)
    fun cone(width: Float, height: Float, tipSway: Float) = Path().apply {
        moveTo(cx - width / 2, tubeTop)
        quadraticTo(cx - width / 2, tubeTop - height * 0.55f, cx + tipSway, tubeTop - height)
        quadraticTo(cx + width / 2, tubeTop - height * 0.55f, cx + width / 2, tubeTop)
        close()
    }
    // Resplandor que tiñe la base del recipiente (justo arriba) y el aire alrededor
    val haloR = tubeW * 2.4f
    drawCircle(
        Brush.radialGradient(
            listOf(tones.halo.copy(alpha = (if (light) 0.22f else 0.38f) * flame), Color.Transparent),
            center = Offset(cx, tubeTop - flameH * 0.7f),
            radius = haloR,
        ),
        radius = haloR,
        center = Offset(cx, tubeTop - flameH * 0.7f),
    )
    // Cono exterior: transparente en la punta, intenso en la base
    drawPath(
        cone(tubeW * 1.1f, flameH, sway),
        Brush.verticalGradient(
            listOf(tones.outer.copy(alpha = 0f), tones.outer.copy(alpha = 0.8f), tones.outerBase),
            startY = tubeTop - flameH,
            endY = tubeTop,
        ),
    )
    // Núcleo: el cono interior brillante, más quieto que el exterior
    val coreH = flameH * 0.5f
    drawPath(
        cone(tubeW * 0.5f, coreH, sway * 0.4f),
        Brush.verticalGradient(
            listOf(tones.core.copy(alpha = 0.6f), tones.core, Color.White),
            startY = tubeTop - coreH,
            endY = tubeTop,
        ),
    )
    // Chispitas que suben desde la punta y se apagan
    repeat(SPARKS) { i ->
        val life = (time * 1.6f + i / SPARKS.toFloat()) % 1f
        val y = tubeTop - flameH * (0.55f + 0.75f * life)
        val x = cx + sway + tubeW * 0.45f * sin(i * 12.9f + life * 5f)
        val alpha = (1f - life) * flame * 0.9f
        drawCircle(tones.spark.copy(alpha = alpha), radius = tubeW * 0.045f * (1f - life * 0.5f), center = Offset(x, y))
    }
}

private const val FLICKER_MS = 2_400
private const val SPARKS = 4
private const val TWO_PI = 6.2831855f
