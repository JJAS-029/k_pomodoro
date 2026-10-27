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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jjas.labpomodoro.ui.theme.isLight
import com.jjas.labpomodoro.domain.model.BurnerStyle
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
fun BunsenBurner(
    lit: Boolean,
    modifier: Modifier = Modifier,
    tones: FlameTones = LocalLabTheme.current.flame,
    /** Bunsen de serie, o el que se compró en la tienda de cristalería. */
    style: BurnerStyle = LocalLabTheme.current.burner,
) {
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
        when (style) {
            BurnerStyle.BUNSEN -> {
                val tubeW = h * 0.26f
                val tubeTop = h * 0.45f
                drawBody(cx, h, tubeW, tubeTop, light)
                if (flame > 0.01f) drawFlame(cx, tubeW, tubeTop, flame, time, tones, light)
            }
            BurnerStyle.ALCOHOL_LAMP -> {
                val wickTop = h * 0.32f
                drawAlcoholLamp(cx, h, wickTop, light)
                if (flame > 0.01f) drawFlame(cx, h * 0.17f, wickTop, flame, time, tones, light, heightScale = 0.85f)
            }
            BurnerStyle.MEKER -> {
                val headTop = h * 0.5f
                drawMeker(cx, h, headTop, light)
                // Llama corta y ancha, hecha de muchos conitos sobre la rejilla
                if (flame > 0.01f) {
                    listOf(-0.16f, 0f, 0.16f).forEachIndexed { i, dx ->
                        drawFlame(cx + h * dx, h * 0.16f, headTop, flame, (time + i * 0.31f) % 1f, tones, light, heightScale = 0.55f, halo = i == 1)
                    }
                }
            }
            BurnerStyle.HOT_PLATE -> drawHotPlate(cx, h, flame, time, tones, light)
        }
    }
}

/** Sombra suave sobre la mesa, debajo de cualquier mechero. */
private fun DrawScope.drawTableShadow(cx: Float, h: Float, width: Float, light: Boolean) {
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = if (light) 0.18f else 0.45f), Color.Transparent), Offset(cx, h), width * 0.6f),
        topLeft = Offset(cx - width * 0.6f, h - h * 0.07f),
        size = Size(width * 1.2f, h * 0.14f),
    )
}

/** Lámpara de alcohol: bulbo de vidrio con alcohol, cuello metálico y mecha. */
private fun DrawScope.drawAlcoholLamp(cx: Float, h: Float, wickTop: Float, light: Boolean) {
    val colors = metal(light)
    val bodyW = h * 0.78f
    val bodyTop = h * 0.46f
    drawTableShadow(cx, h, bodyW, light)
    val glass = if (light) Color(0xFF6B7A90) else Color.White
    val body = Rect(cx - bodyW / 2, bodyTop, cx + bodyW / 2, h * 0.98f)
    // Alcohol en la mitad de abajo, con un brillo azulado
    drawOval(
        Brush.verticalGradient(listOf(Color(0xFF9FD4FF).copy(alpha = 0.35f), Color(0xFF4F9DFF).copy(alpha = 0.7f)), startY = bodyTop + body.height * 0.3f, endY = body.bottom),
        topLeft = Offset(body.left + bodyW * 0.05f, bodyTop + body.height * 0.3f),
        size = Size(bodyW * 0.9f, body.height * 0.66f),
    )
    drawOval(glass.copy(alpha = if (light) 0.14f else 0.1f), body.topLeft, body.size)
    drawOval(glass.copy(alpha = if (light) 0.6f else 0.45f), body.topLeft, body.size, style = Stroke(width = h * 0.025f))
    // Reflejo del vidrio
    drawArc(
        Color.White.copy(alpha = if (light) 0.7f else 0.4f),
        startAngle = 200f, sweepAngle = 40f, useCenter = false,
        topLeft = Offset(body.left + bodyW * 0.1f, bodyTop + body.height * 0.12f),
        size = Size(bodyW * 0.8f, body.height * 0.76f),
        style = Stroke(width = h * 0.03f, cap = StrokeCap.Round),
    )
    // Cuello metálico y tapa
    val neckW = h * 0.3f
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - neckW / 2, endX = cx + neckW / 2),
        Offset(cx - neckW / 2, h * 0.38f),
        Size(neckW, h * 0.12f),
        CornerRadius(h * 0.03f),
    )
    // Mecha
    drawRoundRect(
        if (light) Color(0xFFD9CDB8) else Color(0xFFEDE3D0),
        Offset(cx - h * 0.04f, wickTop),
        Size(h * 0.08f, h * 0.07f),
        CornerRadius(h * 0.02f),
    )
}

/** Mechero Meker: como el Bunsen, pero con la boca ancha y una rejilla arriba. */
private fun DrawScope.drawMeker(cx: Float, h: Float, headTop: Float, light: Boolean) {
    val colors = metal(light)
    val baseTop = h * 0.86f
    val baseW = h * 1.3f
    drawTableShadow(cx, h, baseW, light)
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - baseW / 2, endX = cx + baseW / 2),
        Offset(cx - baseW / 2, baseTop),
        Size(baseW, h * 0.12f),
        CornerRadius(h * 0.06f),
    )
    // Tubo que se abre hacia la cabeza
    val tubeW = h * 0.22f
    val headW = h * 0.56f
    val tube = Path().apply {
        moveTo(cx - tubeW / 2, baseTop + h * 0.02f)
        lineTo(cx - tubeW / 2, h * 0.68f)
        lineTo(cx - headW / 2, headTop + h * 0.06f)
        lineTo(cx + headW / 2, headTop + h * 0.06f)
        lineTo(cx + tubeW / 2, h * 0.68f)
        lineTo(cx + tubeW / 2, baseTop + h * 0.02f)
        close()
    }
    drawPath(tube, Brush.horizontalGradient(colors, startX = cx - headW / 2, endX = cx + headW / 2))
    // Cabeza con la rejilla
    drawRoundRect(
        Brush.horizontalGradient(colors, startX = cx - headW / 2, endX = cx + headW / 2),
        Offset(cx - headW / 2, headTop),
        Size(headW, h * 0.07f),
        CornerRadius(h * 0.02f),
    )
    val grid = Color.Black.copy(alpha = if (light) 0.35f else 0.55f)
    for (i in 1..6) {
        val x = cx - headW / 2 + headW * i / 7f
        drawLine(grid, Offset(x, headTop + h * 0.012f), Offset(x, headTop + h * 0.058f), strokeWidth = h * 0.012f)
    }
}

/** Placa calefactora: caja con perilla y una placa que se pone al rojo (del color de la llama). */
private fun DrawScope.drawHotPlate(cx: Float, h: Float, heat: Float, time: Float, tones: FlameTones, light: Boolean) {
    val colors = metal(light)
    val boxW = h * 1.6f
    val boxTop = h * 0.6f
    drawTableShadow(cx, h, boxW, light)
    drawRoundRect(
        Brush.verticalGradient(
            if (light) listOf(Color(0xFFF2F4F7), Color(0xFFC9CED6)) else listOf(Color(0xFF4A5059), Color(0xFF23272D)),
            startY = boxTop, endY = h,
        ),
        Offset(cx - boxW / 2, boxTop),
        Size(boxW, h * 0.38f),
        CornerRadius(h * 0.06f),
    )
    // Perilla y piloto
    val knob = Offset(cx + boxW * 0.3f, h * 0.8f)
    drawCircle(Brush.horizontalGradient(colors, startX = knob.x - h * 0.09f, endX = knob.x + h * 0.09f), radius = h * 0.09f, center = knob)
    drawLine(Color.Black.copy(alpha = 0.5f), knob, Offset(knob.x + h * 0.04f, knob.y - h * 0.06f), strokeWidth = h * 0.025f, cap = StrokeCap.Round)
    val pilot = Offset(cx - boxW * 0.3f, h * 0.8f)
    drawCircle(Color.Black.copy(alpha = 0.4f), radius = h * 0.035f, center = pilot)
    if (heat > 0.01f) drawCircle(tones.halo.copy(alpha = heat), radius = h * 0.03f, center = pilot)
    // Placa: cerámica oscura que se calienta
    val plateW = boxW * 0.86f
    val plateTop = h * 0.5f
    val plate = Rect(cx - plateW / 2, plateTop, cx + plateW / 2, boxTop + h * 0.02f)
    drawRoundRect(Color(0xFF1C1D21), plate.topLeft, plate.size, CornerRadius(h * 0.03f))
    if (heat > 0.01f) {
        val haloR = plateW * 0.7f
        drawCircle(
            Brush.radialGradient(listOf(tones.halo.copy(alpha = (if (light) 0.25f else 0.4f) * heat), Color.Transparent), Offset(cx, plateTop), haloR),
            radius = haloR,
            center = Offset(cx, plateTop),
        )
        drawRoundRect(
            Brush.horizontalGradient(
                listOf(tones.outerBase.copy(alpha = 0.2f * heat), tones.outer.copy(alpha = 0.85f * heat), tones.outerBase.copy(alpha = 0.2f * heat)),
                startX = plate.left, endX = plate.right,
            ),
            plate.topLeft, plate.size, CornerRadius(h * 0.03f),
        )
        // Resistencia en espiral que brilla
        listOf(0.82f, 0.55f, 0.28f).forEach { k ->
            drawOval(
                tones.core.copy(alpha = 0.7f * heat),
                topLeft = Offset(cx - plateW * k / 2, plate.top + plate.height * 0.2f),
                size = Size(plateW * k, plate.height * 0.6f),
                style = Stroke(width = h * 0.012f),
            )
        }
        // Aire caliente que ondula sobre la placa
        val phase = time * TWO_PI
        repeat(3) { i ->
            val x0 = cx + plateW * (i - 1) * 0.28f
            val wave = Path().apply {
                for (step in 0..10) {
                    val k = step / 10f
                    val x = x0 + h * 0.05f * sin(phase * 2f + k * 6f + i)
                    val y = plateTop - h * 0.06f - k * plateTop * 0.7f
                    if (step == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(wave, tones.outer.copy(alpha = 0.22f * heat), style = Stroke(width = h * 0.02f, cap = StrokeCap.Round))
        }
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

private fun DrawScope.drawFlame(
    cx: Float,
    tubeW: Float,
    tubeTop: Float,
    flame: Float,
    time: Float,
    tones: FlameTones,
    light: Boolean,
    heightScale: Float = 1f,
    halo: Boolean = true,
) {
    val phase = time * TWO_PI
    // Dos ritmos de parpadeo: la llama nunca se ve en bucle
    val pulse = 1f - 0.05f + 0.05f * sin(phase * 3f) + 0.025f * sin(phase * 7f + 1.3f)
    val flameH = tubeTop * 0.95f * flame * pulse * heightScale
    val sway = tubeW * 0.12f * sin(phase * 2f)
    fun cone(width: Float, height: Float, tipSway: Float) = Path().apply {
        moveTo(cx - width / 2, tubeTop)
        quadraticTo(cx - width / 2, tubeTop - height * 0.55f, cx + tipSway, tubeTop - height)
        quadraticTo(cx + width / 2, tubeTop - height * 0.55f, cx + width / 2, tubeTop)
        close()
    }
    // Resplandor que tiñe la base del recipiente (justo arriba) y el aire alrededor
    val haloR = tubeW * 2.4f
    if (halo) drawCircle(
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
