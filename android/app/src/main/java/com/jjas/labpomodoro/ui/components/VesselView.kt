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
import kotlin.math.exp
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

/** Lo que dura el chapoteo al cambiar de sesión. */
private const val SLOSH_SECONDS = 3f

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
    behavior: ElementBehavior? = null,
) {
    val animatedFill by animateFloatAsState(fill.coerceIn(0f, 1f), tween(300, easing = LinearEasing), label = "fill")
    val animatedColor by animateColorAsState(liquidColor, tween(600), label = "liquid")
    val currentFill by rememberUpdatedState(animatedFill)
    val currentEffect by rememberUpdatedState(effect)
    val currentAnimate by rememberUpdatedState(animate)
    val currentBehavior by rememberUpdatedState(behavior)

    // Al cambiar de recipiente las partículas viejas ya no tienen sentido
    val particles = remember(shape) { ParticleSystem() }
    var frameNanos by remember { mutableLongStateOf(0L) }
    // Cambio de sesión: el líquido nuevo llega chapoteando y se calma solo
    val sloshStart = remember(shape, effect) { System.nanoTime() }

    LaunchedEffect(animate, effect, shape, behavior) {
        var last = 0L
        while (currentAnimate || particles.isNotEmpty()) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                particles.update(dt, currentFill, currentEffect, currentBehavior, spawn = currentAnimate)
                frameNanos = now
            }
        }
    }

    Canvas(modifier) {
        val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
        val t = frameNanos / 1_000_000_000f
        // El halo va fuera del recorte: debe difuminarse más allá del lienzo, sin bordes rectos
        drawHalo(g, animatedFill, animatedColor, behavior, t)
        val sloshAge = ((frameNanos - sloshStart) / 1_000_000_000f).takeIf { frameNanos > sloshStart && it < SLOSH_SECONDS }
        clipRect {
            drawLiquid(g, animatedFill, animatedColor, t, waving = animate, behavior = behavior, sloshAge = sloshAge)
            particles.drawInLiquid(this, g, animatedFill, bubbleColor, animatedColor)
            drawGlass(g)
            particles.drawVapor(
                this,
                g,
                vaporColor = if (behavior == ElementBehavior.COLORED_VAPOR) animatedColor else Color.White,
                sparkColor = animatedColor,
            )
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

/** Brillo propio alrededor del líquido: constante en los luminosos, late en los radiactivos. */
private fun DrawScope.drawHalo(g: VesselGeometry, fill: Float, color: Color, behavior: ElementBehavior?, t: Float) {
    val alpha = when (behavior) {
        ElementBehavior.GLOW -> 0.32f + 0.04f * sin(t * 9f) // leve parpadeo de tubo de descarga
        ElementBehavior.RADIOACTIVE -> 0.18f + 0.14f * sin(t * 2.2f)
        else -> return
    }
    if (fill <= 0.02f) return
    val center = Offset(g.px(0.5f), g.py(fill * g.maxFill / 2f))
    val radius = g.width * 0.95f
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, radius), radius, center)
}

private fun DrawScope.drawLiquid(
    g: VesselGeometry,
    fill: Float,
    liquid: Color,
    t: Float,
    waving: Boolean,
    behavior: ElementBehavior? = null,
    sloshAge: Float? = null,
) {
    if (fill <= 0.001f) return
    clipPath(g.interior) {
        val surface = g.surfaceY(fill)
        val amplitude = if (waving) g.unit * 0.012f else 0f
        // Chapoteo: el líquido se inclina de lado a lado y se amortigua en un par de segundos
        val slosh = if (waving && sloshAge != null) g.unit * 0.05f * exp(-sloshAge * 2.2f) * sin(sloshAge * 7f) else 0f

        /** Altura de la superficie: dos ondas que viajan en sentidos opuestos, más el chapoteo. */
        fun waveY(xn: Float, phase: Float, scale: Float): Float {
            val a = amplitude * scale
            val x = xn * 2f * PI.toFloat()
            return surface + a * sin(x * 1.5f + t * 2.2f + phase) + a * 0.55f * sin(x * 2.7f - t * 1.5f + phase * 1.7f) +
                slosh * (xn - 0.5f) * 2f
        }

        fun surfacePath(phase: Float, scale: Float, lift: Float, closed: Boolean) = Path().apply {
            val steps = 28
            for (i in 0..steps) {
                val xn = i.toFloat() / steps
                val x = g.left + g.width * xn
                val y = waveY(xn, phase, scale) - lift
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            if (closed) {
                lineTo(g.right, g.bottom)
                lineTo(g.left, g.bottom)
                close()
            }
        }

        // Capa de atrás, un poco más alta y oscura: da profundidad a la superficie
        if (waving) {
            drawPath(surfacePath(phase = 2.1f, scale = 1.3f, lift = amplitude * 0.8f, closed = true), lerp(liquid, Color.Black, 0.25f).copy(alpha = 0.55f))
        }
        val path = surfacePath(phase = 0f, scale = 1f, lift = 0f, closed = true)
        // Profundidad: más claro en la superficie y más oscuro al fondo
        val (top, bottom) = when (behavior) {
            // Metal: opaco y con mucho contraste, como un espejo
            ElementBehavior.METALLIC -> lerp(liquid, Color.White, 0.45f) to lerp(liquid, Color.Black, 0.55f)
            // Luminoso: más claro, parece encendido
            ElementBehavior.GLOW -> lerp(liquid, Color.White, 0.4f).copy(alpha = 0.95f) to liquid.copy(alpha = 0.9f)
            else -> lerp(liquid, Color.White, 0.18f).copy(alpha = 0.9f) to lerp(liquid, Color.Black, 0.35f).copy(alpha = 0.95f)
        }
        drawPath(path, Brush.verticalGradient(listOf(top, bottom), startY = surface, endY = g.bottom))
        if (behavior == ElementBehavior.METALLIC) {
            // Reflejo que recorre el metal de lado a lado
            val x = g.left + g.width * ((t * 0.18f) % 1.6f - 0.3f)
            val band = g.width * 0.22f
            clipPath(path) {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent),
                        startX = x - band,
                        endX = x + band,
                    ),
                    topLeft = Offset(x - band, surface - g.unit),
                    size = Size(band * 2, g.bottom - surface + g.unit),
                )
            }
        }
        // Menisco: brillo que sigue la ola de enfrente
        drawPath(
            surfacePath(phase = 0f, scale = 1f, lift = 0f, closed = false),
            Color.White.copy(alpha = 0.3f),
            style = Stroke(width = g.stroke * 0.6f, cap = StrokeCap.Round),
        )
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

private enum class Kind {
    FIZZ,
    BUBBLE,
    VAPOR,

    /** Chispa que salta de la superficie (alcalinos y prueba de la llama). */
    SPARK,

    /** Cristal que cae y se asienta en el fondo (precipitado). */
    FLAKE,

    /** Destello breve dentro del líquido (radiactivos). */
    SPARKLE,
}

private class Particle(
    /** Carril horizontal −1..1: se escala con el ancho del recipiente a esa altura. */
    var lane: Float,
    /** Altura en "fracción de llenado": 1 = superficie con el recipiente lleno; el vapor pasa de 1. */
    var v: Float,
    /** Velocidad vertical (fracciones por segundo); cambia con [gravity]. */
    var speed: Float,
    val life: Float,
    val size: Float,
    val phase: Float,
    val kind: Kind,
    /** Aceleración vertical: negativa = cae. */
    var gravity: Float = 0f,
    /** Deriva horizontal en carriles por segundo (chispas que salen en arco). */
    val drift: Float = 0f,
    /** Segundos que se queda pegada a la pared antes de moverse. */
    val stick: Float = 0f,
) {
    var age = 0f
    var bounced = false
}

private class ParticleSystem {
    private val particles = ArrayList<Particle>()
    private val random = Random(System.nanoTime())
    private var fizzBudget = 0f
    private var mainBudget = 0f
    private var specialBudget = 0f

    fun isNotEmpty() = particles.isNotEmpty()

    fun update(dt: Float, fill: Float, effect: LiquidEffect, behavior: ElementBehavior?, spawn: Boolean) {
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.age += dt
            // Pegada a la pared: espera antes de soltarse
            if (p.age >= p.stick) {
                p.speed += p.gravity * dt
                // Los cristales caen con la resistencia del líquido: velocidad límite
                if (p.kind == Kind.FLAKE) p.speed = p.speed.coerceAtLeast(-0.12f)
                p.v += p.speed * dt
                p.lane = (p.lane + p.drift * dt).coerceIn(-1f, 1f)
            }
            if (p.v <= FLOOR) {
                p.v = FLOOR
                if (p.kind == Kind.FLAKE) {
                    // Rebota una vez al tocar el fondo y luego se queda quieto
                    if (!p.bounced && p.speed < -0.04f) {
                        p.speed = -p.speed * 0.35f
                        p.bounced = true
                    } else {
                        p.speed = 0f
                        p.gravity = 0f
                    }
                }
            }
            val popped = (p.kind == Kind.FIZZ || p.kind == Kind.BUBBLE) && p.v >= fill
            // Si el líquido baja de su altura, el destello ya no tiene dónde estar
            val stranded = (p.kind == Kind.SPARKLE || p.kind == Kind.FLAKE) && p.v > fill
            // La chispa vuelve a caer al líquido y se apaga
            val landed = p.kind == Kind.SPARK && p.speed < 0f && p.v < fill
            if (p.age >= p.life || popped || stranded || landed) iterator.remove()
        }

        if (!spawn || effect == LiquidEffect.NONE || dt == 0f || fill <= 0.02f) return

        spawnSpecial(dt, fill, behavior)

        // Efervescencia: la mayoría nace en el fondo y algunas en las paredes. Los alcalinos
        // reaccionan con el agua y los gases licuados hierven: mucha más
        val fizzRate = when (behavior) {
            ElementBehavior.REACTIVE -> 32f
            ElementBehavior.CRYO -> 24f
            ElementBehavior.METALLIC -> 2f
            else -> 9f
        }
        fizzBudget += fizzRate * dt
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
                // Las de la pared se quedan pegadas un momento, como en un vaso de refresco
                stick = if (fromWall) 0.3f + random.nextFloat() * 1.2f else 0f,
            )
        }

        val vaporBoost = when (behavior) {
            ElementBehavior.CRYO -> 3f
            ElementBehavior.COLORED_VAPOR -> 1.6f
            else -> 1f
        }
        val rate = when (effect) {
            LiquidEffect.VAPOR -> (if (fill < 0.2f) 4f else 1.6f) * vaporBoost // Como el prototipo: más vapor al final
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
                    size = (0.06f + random.nextFloat() * 0.05f) * if (behavior == ElementBehavior.CRYO) 1.6f else 1f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.VAPOR,
                )
            } else {
                Particle(
                    lane = random.nextFloat() * 1.6f - 0.8f,
                    v = 0.02f,
                    speed = 0.06f,
                    life = 9f,
                    size = 0.025f + random.nextFloat() * 0.035f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.BUBBLE,
                    // Arranca lenta y acelera al subir, como una burbuja de verdad
                    gravity = 0.08f + random.nextFloat() * 0.08f,
                    stick = random.nextFloat() * 0.4f,
                )
            }
        }
        if (fizzBudget > 1f) fizzBudget = 0f
        if (mainBudget > 1f) mainBudget = 0f
    }

    private fun spawnSpecial(dt: Float, fill: Float, behavior: ElementBehavior?) {
        val rate = when (behavior) {
            ElementBehavior.REACTIVE -> 3f
            ElementBehavior.FLAME -> 5f
            ElementBehavior.PRECIPITATE -> 2.5f
            ElementBehavior.RADIOACTIVE -> 7f
            else -> return
        }
        specialBudget += rate * dt
        while (specialBudget >= 1f && particles.size < MAX_PARTICLES) {
            specialBudget -= 1f
            particles += when (behavior) {
                ElementBehavior.PRECIPITATE -> Particle(
                    lane = random.nextFloat() * 1.6f - 0.8f,
                    v = fill * (0.7f + random.nextFloat() * 0.3f),
                    speed = -0.01f,
                    gravity = -(0.12f + random.nextFloat() * 0.08f),
                    life = 16f,
                    size = 0.012f + random.nextFloat() * 0.012f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.FLAKE,
                )
                ElementBehavior.RADIOACTIVE -> Particle(
                    lane = random.nextFloat() * 1.8f - 0.9f,
                    v = random.nextFloat() * fill,
                    speed = 0f,
                    life = 0.25f + random.nextFloat() * 0.25f,
                    size = 0.008f + random.nextFloat() * 0.01f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.SPARKLE,
                )
                else -> Particle(
                    lane = random.nextFloat() * 1.4f - 0.7f,
                    v = fill + 0.01f,
                    // Sale disparada hacia arriba, describe un arco y vuelve a caer
                    speed = 0.45f + random.nextFloat() * 0.35f,
                    gravity = -1.6f,
                    drift = random.nextFloat() * 1.2f - 0.6f,
                    life = 1.4f,
                    size = 0.008f + random.nextFloat() * 0.008f,
                    phase = random.nextFloat() * 6.28f,
                    kind = Kind.SPARK,
                )
            }
        }
        if (specialBudget > 1f) specialBudget = 0f
    }

    private fun position(g: VesselGeometry, p: Particle, wobbleAmount: Float): Offset {
        val v = p.v * g.maxFill
        val wobble = sin(p.phase + p.age * 3f) * wobbleAmount
        val u = 0.5f + (p.lane + wobble).coerceIn(-1f, 1f) * g.halfWidth(v)
        return Offset(g.px(u), g.py(v))
    }

    fun drawInLiquid(scope: DrawScope, g: VesselGeometry, fill: Float, bubbleColor: Color, liquidColor: Color) = with(scope) {
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
                        Kind.FLAKE -> {
                            // Mientras cae se mece un poco; en el fondo queda quieto
                            val c = position(g, p, if (p.v > 0.02f) 0.02f else 0f)
                            val r = g.unit * p.size
                            val crystal = Path().apply {
                                moveTo(c.x, c.y - r)
                                lineTo(c.x + r * 0.7f, c.y)
                                lineTo(c.x, c.y + r)
                                lineTo(c.x - r * 0.7f, c.y)
                                close()
                            }
                            drawPath(crystal, lerp(liquidColor, Color.White, 0.5f))
                        }
                        Kind.SPARKLE -> {
                            // Destello que aparece y se apaga
                            val flash = sin(PI.toFloat() * p.age / p.life)
                            val c = position(g, p, 0f)
                            drawCircle(liquidColor.copy(alpha = 0.5f * flash), g.unit * p.size * 3f, c)
                            drawCircle(Color.White.copy(alpha = 0.9f * flash), g.unit * p.size, c)
                        }
                        Kind.VAPOR, Kind.SPARK -> Unit
                    }
                }
            }
        }
    }

    fun drawVapor(scope: DrawScope, g: VesselGeometry, vaporColor: Color, sparkColor: Color) = with(scope) {
        for (p in particles) {
            if (p.kind == Kind.SPARK) {
                // Chispa del color de la llama del elemento, con centro blanco
                val fade = 1f - p.age / p.life
                val c = position(g, p, 0.05f)
                val r = g.unit * p.size
                drawCircle(sparkColor.copy(alpha = 0.8f * fade), r * 2.2f, c)
                drawCircle(Color.White.copy(alpha = fade), r, c)
                continue
            }
            if (p.kind != Kind.VAPOR) continue
            val progress = p.age / p.life
            val center = position(g, p, 0.15f)
            val radius = g.unit * p.size * (1f + progress)
            val alpha = 0.35f * (1f - progress) * (progress * 6f).coerceAtMost(1f)
            drawCircle(
                Brush.radialGradient(listOf(vaporColor.copy(alpha = alpha), vaporColor.copy(alpha = 0f)), center, radius),
                radius,
                center,
            )
        }
    }

    private companion object {
        const val MAX_PARTICLES = 90

        /** Altura del fondo, para que nada atraviese el vidrio. */
        const val FLOOR = 0.015f
    }
}
