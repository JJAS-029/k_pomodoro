package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.random.Random

/** Una gota: su posición inicial y su plano de profundidad (0 = lejos, 2 = cerca). */
private class Drop(val x: Float, val y: Float, val layer: Int, val jitter: Float)

/**
 * Lluvia detrás del recipiente, para acompañar el sonido de lluvia en el modo ambiente. Tres
 * planos de profundidad: las gotas cercanas caen más rápido, son más largas y más visibles
 * (paralaje). Todo cae con la misma inclinación, como si hubiera un poco de viento.
 */
@Composable
fun RainBackground(modifier: Modifier = Modifier, color: Color = Color(0xFF9FB4C7)) {
    val drops = remember {
        val random = Random(42)
        List(DROPS) { Drop(random.nextFloat(), random.nextFloat(), random.nextInt(3), random.nextFloat()) }
    }
    var frameNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos = it }
    }

    Canvas(modifier) {
        val t = frameNanos / 1_000_000_000f
        val w = size.width
        val h = size.height
        drops.forEach { d ->
            val speed = SPEEDS[d.layer] * (0.85f + 0.3f * d.jitter)
            val length = h * LENGTHS[d.layer]
            // Posición cíclica: al salir por abajo vuelve a entrar por arriba
            val progress = (d.y + t * speed) % 1f
            val y = progress * (h + length) - length
            val x = ((d.x + progress * WIND) % 1f) * w
            drawLine(
                color.copy(alpha = ALPHAS[d.layer]),
                start = Offset(x, y),
                end = Offset(x + length * WIND * 0.6f, y + length),
                strokeWidth = WIDTHS[d.layer] * density,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val DROPS = 90
private const val WIND = 0.12f
private val SPEEDS = floatArrayOf(0.35f, 0.55f, 0.85f)
private val LENGTHS = floatArrayOf(0.02f, 0.035f, 0.055f)
private val ALPHAS = floatArrayOf(0.12f, 0.2f, 0.32f)
private val WIDTHS = floatArrayOf(0.8f, 1.1f, 1.5f)
