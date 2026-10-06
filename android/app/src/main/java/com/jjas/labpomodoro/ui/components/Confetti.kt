package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Lo que dispara una ráfaga: cada valor nuevo de [id] lanza una, con esos colores. */
data class Celebration(
    val id: Long,
    val colors: List<Color>,
    val big: Boolean = false,
    /** De dónde salen, en fracciones de la pantalla. */
    val originX: Float = 0.5f,
    val originY: Float = 0.4f,
)

private class Piece(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var angle: Float,
    val spin: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val round: Boolean,
    /** Fase del aleteo: el papelito gira sobre sí mismo y se ve más angosto a ratos. */
    val flutter: Float,
) {
    var age = 0f
}

/**
 * Confeti propio: papelitos que salen en abanico desde el origen de la [Celebration], giran, caen por gravedad con resistencia del aire y se desvanecen. Solo anima mientras
 * hay papelitos, así que no gasta batería el resto del tiempo.
 */
@Composable
fun ConfettiBurst(celebration: Celebration?, modifier: Modifier = Modifier) {
    val pieces = remember { ArrayList<Piece>() }
    var frame by remember { mutableLongStateOf(0L) }
    val density = LocalDensity.current.density
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var bursts by remember { mutableIntStateOf(0) }

    LaunchedEffect(celebration?.id) {
        val c = celebration ?: return@LaunchedEffect
        // Espera a conocer el tamaño del lienzo
        while (canvasSize == Size.Zero) withFrameNanos { }
        val random = Random(c.id)
        val count = if (c.big) 140 else 70
        val colors = c.colors.ifEmpty { listOf(Color.White) } + Color.White
        repeat(count) {
            // Abanico hacia arriba, de unos 140°
            val angle = Math.toRadians(-90.0 + (random.nextDouble() - 0.5) * 140.0)
            val speed = (500f + random.nextFloat() * 900f) * density / 2.75f
            pieces += Piece(
                x = canvasSize.width * c.originX,
                y = canvasSize.height * c.originY,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                angle = random.nextFloat() * 360f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                width = (5f + random.nextFloat() * 5f) * density,
                height = (3f + random.nextFloat() * 3f) * density,
                color = colors[random.nextInt(colors.size)],
                round = random.nextFloat() < 0.3f,
                flutter = random.nextFloat() * 6.28f,
            )
        }
        bursts++
    }

    // La animación va aparte: si la celebración se quita antes, los papelitos terminan de caer
    LaunchedEffect(bursts) {
        var last = 0L
        while (pieces.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                last = now
                val gravity = 900f * density / 2.75f
                val iterator = pieces.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.age += dt
                    // Resistencia del aire: frena rápido y luego cae flotando
                    val drag = 1f - 1.8f * dt
                    p.vx *= drag
                    p.vy = p.vy * drag + gravity * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.angle += p.spin * dt
                    if (p.age > LIFE || p.y > canvasSize.height + 40f) iterator.remove()
                }
                frame = now
            }
        }
    }

    Canvas(modifier.onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }) {
        frame // Leerlo aquí hace que se redibuje en cada cuadro
        for (p in pieces) {
            val fade = ((LIFE - p.age) / FADE).coerceIn(0f, 1f)
            val color = p.color.copy(alpha = p.color.alpha * fade)
            if (p.round) {
                drawCircle(color, p.height * 0.7f, Offset(p.x, p.y))
            } else {
                val squeeze = 0.35f + 0.65f * abs(sin(p.flutter + p.age * 9f))
                rotate(p.angle, Offset(p.x, p.y)) {
                    drawRect(
                        color,
                        topLeft = Offset(p.x - p.width / 2, p.y - p.height * squeeze / 2),
                        size = Size(p.width, p.height * squeeze),
                    )
                }
            }
        }
    }
}

private const val LIFE = 2.8f
private const val FADE = 0.7f
