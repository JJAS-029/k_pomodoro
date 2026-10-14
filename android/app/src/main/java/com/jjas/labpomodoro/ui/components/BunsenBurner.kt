package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.sin

/**
 * Mechero Bunsen bajo el recipiente: con la llama azul encendida durante el trabajo (explica por
 * qué el líquido se evapora) y apagado en los descansos. La llama se enciende y apaga con suavidad.
 */
@Composable
fun BunsenBurner(lit: Boolean, modifier: Modifier = Modifier) {
    val glass = glassOutlineColor()
    val flame by animateFloatAsState(if (lit) 1f else 0f, tween(600), label = "flame")
    // Parpadeo continuo solo mientras está encendido
    val flicker = if (lit) {
        val transition = rememberInfiniteTransition(label = "flicker")
        transition.animateFloat(0f, 6.28f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "phase").value
    } else {
        0f
    }

    Canvas(modifier) {
        val cx = size.width / 2f
        val h = size.height
        // Tubo y base
        val tubeW = h * 0.28f
        val tubeTop = h * 0.45f
        drawRoundRect(glass, Offset(cx - tubeW / 2, tubeTop), Size(tubeW, h * 0.48f), CornerRadius(tubeW * 0.2f))
        val baseW = h * 1.3f
        drawRoundRect(glass, Offset(cx - baseW / 2, h * 0.88f), Size(baseW, h * 0.12f), CornerRadius(h * 0.06f))
        // Llama: cono exterior azul y núcleo claro, que tiembla un poco
        if (flame > 0.01f) {
            // Llega casi al fondo del recipiente, que queda justo encima
            val flameH = tubeTop * 0.95f * flame * (1f - 0.06f + 0.06f * sin(flicker * 2f))
            val sway = tubeW * 0.12f * sin(flicker)
            fun cone(width: Float, height: Float) = Path().apply {
                moveTo(cx - width / 2, tubeTop)
                quadraticTo(cx - width / 2, tubeTop - height * 0.55f, cx + sway, tubeTop - height)
                quadraticTo(cx + width / 2, tubeTop - height * 0.55f, cx + width / 2, tubeTop)
                close()
            }
            drawPath(
                cone(tubeW * 1.1f, flameH),
                Brush.verticalGradient(
                    listOf(Color(0x004FC3F7), Color(0xCC2979FF), Color(0xFF1E88E5)),
                    startY = tubeTop - flameH,
                    endY = tubeTop,
                ),
            )
            drawPath(
                cone(tubeW * 0.5f, flameH * 0.55f),
                Brush.verticalGradient(
                    listOf(Color(0x80B3E5FC), Color(0xFFE1F5FE)),
                    startY = tubeTop - flameH * 0.55f,
                    endY = tubeTop,
                ),
            )
        }
    }
}
