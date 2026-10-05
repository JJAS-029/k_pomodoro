package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

enum class LiquidEffect {
    NONE,

    /** Trabajo: el líquido se evapora y sale vapor por la boca del recipiente. */
    VAPOR,

    /** Descanso: suben burbujas grandes de color. */
    BUBBLES,
}

private val GlassColor = Color(0xFFCCCCCC)

/**
 * Recipiente de laboratorio con líquido animado. Además del efecto principal, mientras [animate]
 * es true el líquido tiene efervescencia (burbujitas de gaseosa desde el fondo y las paredes).
 *
 * @param fill nivel del líquido, 0..1 (relativo a la capacidad del recipiente).
 * @param animate si es false no nacen partículas nuevas (pausa) y las que hay terminan su vida.
 */
@Composable
fun VesselView(
    shape: VesselShape,
    fill: Float,
    liquidColor: Color,
    bubbleColor: Color,
    effect: LiquidEffect,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedFill by animateFloatAsState(fill.coerceIn(0f, 1f), tween(300, easing = LinearEasing), label = "fill")
    val animatedColor by animateColorAsState(liquidColor, tween(600), label = "liquid")
    val currentFill by rememberUpdatedState(animatedFill)
    val currentEffect by rememberUpdatedState(effect)
    val currentAnimate by rememberUpdatedState(animate)

    // Al cambiar de recipiente las partículas viejas ya no tienen sentido
    val particles = remember(shape) { ParticleSystem() }
    var frameNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(animate, effect, shape) {
        var last = 0L
        while (currentAnimate || particles.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                particles.update(dt, currentFill, currentEffect, spawn = currentAnimate)
                frameNanos = now
            }
        }
    }

    Canvas(modifier) {
        val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
        val t = frameNanos / 1_000_000_000f
        clipRect {
            drawLiquid(g, animatedFill, animatedColor, t, waving = animate)
            particles.drawInLiquid(this, g, animatedFill, bubbleColor)
            drawGlass(g)
            particles.drawVapor(this, g)
        }
    }
}

/** Versión estática y pequeña para la repisa. */
@Composable
fun MiniVessel(
    shape: VesselShape,
    fill: Float,
    liquidColor: Color,
    outlineColor: Color,
    dashed: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val g = VesselGeometry.fit(shape, size.width, size.height, strokeWidth = 1.5f * density)
        drawLiquid(g, fill, liquidColor, t = 0f, waving = false)
        drawPath(
            g.outline,
            outlineColor,
            style = Stroke(
                width = g.stroke,
                cap = StrokeCap.Round,
                pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null,
            ),
        )
    }
}

private fun DrawScope.drawLiquid(g: VesselGeometry, fill: Float, liquid: Color, t: Float, waving: Boolean) {
    if (fill <= 0.001f) return
    clipPath(g.interior) {
        val surface = g.surfaceY(fill)
        val amplitude = if (waving) g.unit * 0.015f else 0f
        val path = Path().apply {
            moveTo(g.left, surface)
            val steps = 24
            for (i in 0..steps) {
                val x = g.left + g.width * i / steps
                val y = surface + amplitude * sin((i.toFloat() / steps) * 2f * PI.toFloat() * 1.5f + t * 2.2f)
                lineTo(x, y)
            }
            lineTo(g.right, g.bottom)
            lineTo(g.left, g.bottom)
            close()
        }
        // Profundidad: más claro en la superficie y más oscuro al fondo
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(lerp(liquid, Color.White, 0.18f).copy(alpha = 0.9f), lerp(liquid, Color.Black, 0.35f).copy(alpha = 0.95f)),
                startY = surface,
                endY = g.bottom,
            ),
        )
        // Menisco: línea brillante en la superficie
        drawLine(Color.White.copy(alpha = 0.3f), Offset(g.left, surface), Offset(g.right, surface), g.stroke * 0.6f)
    }
}

private fun DrawScope.drawGlass(g: VesselGeometry) {
    val highlight = Color.White.copy(alpha = 0.14f)
    when (g.shape) {
        VesselShape.BEAKER, VesselShape.TEST_TUBE -> {
            // Marcas de medición y reflejo vertical
            val hw = g.halfWidth(0.5f)
            for (i in 1..4) {
                val v = i / 5f * g.maxFill
                val length = if (i % 2 == 0) hw * 0.35f else hw * 0.2f
                drawLine(
                    Color.White.copy(alpha = 0.35f),
                    Offset(g.px(0.5f + hw - length), g.py(v)),
                    Offset(g.px(0.5f + hw - 0.02f), g.py(v)),
                    g.stroke * 0.5f,
                    StrokeCap.Round,
                )
            }
            drawLine(highlight, Offset(g.px(0.5f - hw * 0.75f), g.py(0.85f)), Offset(g.px(0.5f - hw * 0.75f), g.py(0.25f)), g.stroke * 1.4f, StrokeCap.Round)
        }
        VesselShape.ERLENMEYER -> {
            drawLine(highlight, Offset(g.px(0.13f), g.py(0.2f)), Offset(g.px(0.34f), g.py(0.52f)), g.stroke * 1.4f, StrokeCap.Round)
        }
        VesselShape.ROUND_FLASK -> {
            val r = g.width / 2f * 0.78f
            val cy = g.bottom - g.width / 2f
            drawArc(
                highlight,
                startAngle = 200f,
                sweepAngle = 50f,
                useCenter = false,
                topLeft = Offset(g.px(0.5f) - r, cy - r),
                size = Size(2 * r, 2 * r),
                style = Stroke(g.stroke * 1.4f, cap = StrokeCap.Round),
            )
        }
    }
    drawPath(g.outline, GlassColor, style = Stroke(width = g.stroke, cap = StrokeCap.Round))
}

private enum class Kind { FIZZ, BUBBLE, VAPOR }

private class Particle(
    /** Carril horizontal −1..1: se escala con el ancho del recipiente a esa altura. */
    val lane: Float,
    /** Altura en "fracción de llenado": 1 = superficie con el recipiente lleno; el vapor pasa de 1. */
    var v: Float,
    val speed: Float,
    val life: Float,
    val size: Float,
    val phase: Float,
    val kind: Kind,
) {
    var age = 0f
}

private class ParticleSystem {
    private val particles = ArrayList<Particle>()
    private val random = Random(System.nanoTime())
    private var fizzBudget = 0f
    private var mainBudget = 0f

    fun isNotEmpty() = particles.isNotEmpty()

    fun update(dt: Float, fill: Float, effect: LiquidEffect, spawn: Boolean) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += dt
            p.v += p.speed * dt
            val popped = p.kind != Kind.VAPOR && p.v >= fill
            if (p.age >= p.life || popped) iterator.remove()
        }

        if (!spawn || effect == LiquidEffect.NONE || dt == 0f || fill <= 0.02f) return

        // Efervescencia: la mayoría nace en el fondo y algunas en las paredes
        fizzBudget += 9f * dt
        while (fizzBudget >= 1f && particles.size < MAX_PARTICLES) {
            fizzBudget -= 1f
            val fromWall = random.nextFloat() < 0.3f
            particles += Particle(
                lane = if (fromWall) (if (random.nextBoolean()) 0.85f else -0.85f) else random.nextFloat() * 1.6f - 0.8f,
                v = if (fromWall) random.nextFloat() * fill * 0.8f else 0.02f,
                speed = 0.22f + random.nextFloat() * 0.2f,
                life = 10f,
                size = 0.008f + random.nextFloat() * 0.01f,
                phase = random.nextFloat() * 6.28f,
                kind = Kind.FIZZ,
            )
        }

        val rate = when (effect) {
            LiquidEffect.VAPOR -> if (fill < 0.2f) 4f else 1.6f // Como el prototipo: más vapor al final
            LiquidEffect.BUBBLES -> 2.2f
            LiquidEffect.NONE -> 0f
        }
        mainBudget += rate * dt
        while (mainBudget >= 1f && particles.size < MAX_PARTICLES) {
            mainBudget -= 1f
            particles += if (effect == LiquidEffect.VAPOR) {
                Particle(
                    lane = random.nextFloat() * 1.2f - 0.6f,
                    v = fill,
                    speed = 0.08f + random.nextFloat() * 0.06f,
                    life = 2f + random.nextFloat() * 1.5f,
                    size = 0.06f + random.nextFloat() * 0.05f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.VAPOR,
                )
            } else {
                Particle(
                    lane = random.nextFloat() * 1.6f - 0.8f,
                    v = 0.02f,
                    speed = 0.15f + random.nextFloat() * 0.15f,
                    life = 8f,
                    size = 0.025f + random.nextFloat() * 0.035f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.BUBBLE,
                )
            }
        }
        if (fizzBudget > 1f) fizzBudget = 0f
        if (mainBudget > 1f) mainBudget = 0f
    }

    private fun position(g: VesselGeometry, p: Particle, wobbleAmount: Float): Offset {
        val v = p.v * g.maxFill
        val wobble = sin(p.phase + p.age * 3f) * wobbleAmount
        val u = 0.5f + (p.lane + wobble).coerceIn(-1f, 1f) * g.halfWidth(v)
        return Offset(g.px(u), g.py(v))
    }

    fun drawInLiquid(scope: DrawScope, g: VesselGeometry, fill: Float, bubbleColor: Color) = with(scope) {
        if (fill <= 0.001f) return@with
        clipPath(g.interior) {
            clipRect(top = g.surfaceY(fill)) {
                for (p in particles) {
                    when (p.kind) {
                        Kind.FIZZ -> drawCircle(Color.White.copy(alpha = 0.55f), g.unit * p.size, position(g, p, 0.03f))
                        Kind.BUBBLE -> {
                            val c = position(g, p, 0.08f)
                            val r = g.unit * p.size
                            drawCircle(bubbleColor, r, c)
                            drawCircle(Color.White.copy(alpha = 0.5f), r * 0.3f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
                        }
                        Kind.VAPOR -> Unit
                    }
                }
            }
        }
    }

    fun drawVapor(scope: DrawScope, g: VesselGeometry) = with(scope) {
        for (p in particles) {
            if (p.kind != Kind.VAPOR) continue
            val progress = p.age / p.life
            val center = position(g, p, 0.15f)
            val radius = g.unit * p.size * (1f + progress)
            val alpha = 0.35f * (1f - progress) * (progress * 6f).coerceAtMost(1f)
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)), center, radius),
                radius,
                center,
            )
        }
    }

    private companion object {
        const val MAX_PARTICLES = 90
    }
}
