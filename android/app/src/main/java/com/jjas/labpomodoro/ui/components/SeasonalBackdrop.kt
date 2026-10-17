package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.jjas.labpomodoro.domain.model.Season
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lo que cae detrás del recipiente en temporada: pétalos de cempasúchil en Día de Muertos, nieve
 * en invierno. Pocos, lentos y tenues, para no distraer.
 */
@Composable
fun SeasonalBackdrop(season: Season, modifier: Modifier = Modifier) {
    val time by rememberInfiniteTransition(label = "temporada").animateFloat(
        0f, 1f, infiniteRepeatable(tween(LOOP_MILLIS, easing = LinearEasing), RepeatMode.Restart), label = "caída",
    )
    // Posición, velocidad y tamaño fijos por partícula. Cada una cae 2, 3 o 4 veces por vuelta:
    // así el reinicio de la animación no se nota
    val pieces = remember(season) {
        val random = Random(season.ordinal)
        List(COUNT) { i -> FallingPiece(random.nextFloat(), random.nextFloat(), 2 + i % 3, 0.6f + random.nextFloat() * 0.8f) }
    }
    Canvas(modifier) {
        val unit = size.minDimension / 100f
        pieces.forEachIndexed { i, p ->
            val y = ((time * p.laps + p.offset) % 1f) * (size.height + 40f) - 20f
            val x = p.x * size.width + sin((time * 6.283f * 2f) + i) * unit * 4f
            when (season) {
                Season.MUERTOS -> rotate(time * 360f * p.laps + i * 40f, Offset(x, y)) {
                    val petal = if (i % 3 == 0) Color(0xFFE91E63) else if (i % 3 == 1) Color(0xFFFF8F00) else Color(0xFFFFB300)
                    drawOval(petal.copy(alpha = 0.55f), topLeft = Offset(x - unit * 1.4f * p.scale, y - unit * 0.8f * p.scale), size = Size(unit * 2.8f * p.scale, unit * 1.6f * p.scale))
                }
                Season.WINTER -> drawCircle(Color.White.copy(alpha = 0.5f * p.scale), radius = unit * 0.9f * p.scale, center = Offset(x, y))
            }
        }
    }
}

private class FallingPiece(val x: Float, val offset: Float, val laps: Int, val scale: Float)

private const val COUNT = 26
/** Una vuelta de la animación: en ella cada partícula cae 2, 3 o 4 veces (de 15 a 30 s por caída). */
private const val LOOP_MILLIS = 60_000
