package com.jjas.labpomodoro.wear

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.sin

/** Las formas que dibuja el reloj; las demás del teléfono se parecen a alguna de estas. */
private enum class WatchShape { BEAKER, FLASK, TUBE, ROUND }

private fun shapeOf(name: String): WatchShape = when (name) {
    "ERLENMEYER", "KITASATO" -> WatchShape.FLASK
    "TEST_TUBE", "GRADUATED_CYLINDER" -> WatchShape.TUBE
    "ROUND_FLASK", "VOLUMETRIC_FLASK", "DISTILLATION_FLASK", "SEPARATORY_FUNNEL" -> WatchShape.ROUND
    else -> WatchShape.BEAKER
}

/**
 * Recipiente del reloj con el líquido a [level] (0..1). Con Pro toma la forma de la sesión y el
 * líquido ondea con burbujas; sin Pro es un vaso fijo.
 */
@Composable
fun WatchVessel(shape: String, color: Color, level: Float, animated: Boolean, modifier: Modifier = Modifier) {
    val phase = if (animated) {
        rememberInfiniteTransition(label = "vessel")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(2_400, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else {
        0f
    }
    val form = if (animated) shapeOf(shape) else WatchShape.BEAKER
    Canvas(modifier) {
        val outline = outline(form)
        val top = size.height * (1f - level.coerceIn(0f, 1f) * 0.86f)
        clipPath(outline) {
            val liquid = Path().apply {
                moveTo(0f, size.height)
                lineTo(0f, top)
                // Oleaje suave en la superficie (quieto sin Pro)
                val steps = 16
                for (i in 0..steps) {
                    val x = size.width * i / steps
                    val wave = if (animated) sin((i.toFloat() / steps + phase) * 2 * PI).toFloat() * size.height * 0.025f else 0f
                    lineTo(x, top + wave)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(liquid, Brush.verticalGradient(listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.6f)), startY = top, endY = size.height))
            if (animated) bubbles(top, phase)
        }
        drawPath(outline, Color.White.copy(alpha = 0.75f), style = Stroke(width = size.minDimension * 0.05f))
    }
}

/** Burbujas que suben del fondo a la superficie. */
private fun DrawScope.bubbles(surface: Float, phase: Float) {
    repeat(5) { i ->
        val k = (phase + i / 5f) % 1f
        val y = size.height - (size.height - surface) * k
        val x = size.width * (0.3f + 0.4f * ((i * 37) % 10) / 10f) + sin(k * 9f + i) * size.width * 0.04f
        drawCircle(Color.White.copy(alpha = 0.55f * (1f - k)), radius = size.minDimension * 0.035f, center = Offset(x, y))
    }
}

private fun DrawScope.outline(form: WatchShape): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        when (form) {
            WatchShape.BEAKER -> {
                val r = w * 0.2f
                moveTo(w * 0.08f, 0f)
                lineTo(w * 0.08f, h - r)
                quadraticTo(w * 0.08f, h, w * 0.08f + r, h)
                lineTo(w * 0.92f - r, h)
                quadraticTo(w * 0.92f, h, w * 0.92f, h - r)
                lineTo(w * 0.92f, 0f)
            }
            WatchShape.FLASK -> {
                moveTo(w * 0.38f, 0f)
                lineTo(w * 0.38f, h * 0.32f)
                lineTo(w * 0.05f, h * 0.9f)
                quadraticTo(w * 0.02f, h, w * 0.14f, h)
                lineTo(w * 0.86f, h)
                quadraticTo(w * 0.98f, h, w * 0.95f, h * 0.9f)
                lineTo(w * 0.62f, h * 0.32f)
                lineTo(w * 0.62f, 0f)
            }
            WatchShape.TUBE -> {
                val r = w * 0.2f
                moveTo(w * 0.3f, 0f)
                lineTo(w * 0.3f, h - r)
                arcTo(Rect(w * 0.3f, h - 2 * r, w * 0.7f, h), 180f, -180f, false)
                lineTo(w * 0.7f, 0f)
            }
            WatchShape.ROUND -> {
                val r = w * 0.46f
                moveTo(w * 0.38f, 0f)
                lineTo(w * 0.38f, h - 2 * r + r * 0.15f)
                arcTo(Rect(w / 2 - r, h - 2 * r, w / 2 + r, h), 250f, -320f, false)
                lineTo(w * 0.62f, 0f)
            }
        }
    }
}
