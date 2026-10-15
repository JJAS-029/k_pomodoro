package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
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
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random
import kotlin.math.pow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.abs
import kotlin.math.tan
import com.jjas.labpomodoro.ui.theme.isLight
import androidx.compose.material3.MaterialTheme

enum class LiquidEffect {
    NONE,

    /** Trabajo: el líquido se evapora y sale vapor por la boca del recipiente. */
    VAPOR,

    /** Descanso: suben burbujas grandes de color. */
    BUBBLES,
}

/** Vidrio y vapor según el fondo: blancos sobre negro, grises sobre el tema claro. */
private class GlassStyle(val outline: Color, val marks: Color, val highlight: Color, val vapor: Color)

private val GlassOnDark = GlassStyle(Color(0xFFCCCCCC), Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.14f), Color.White)
private val GlassOnLight = GlassStyle(Color(0xFF8C9199), Color(0xFF5F6368).copy(alpha = 0.45f), Color(0xFF5F6368).copy(alpha = 0.12f), Color(0xFF9AA0A6))

@Composable
private fun glassStyle(): GlassStyle = if (MaterialTheme.colorScheme.isLight) GlassOnLight else GlassOnDark

/** Cómo se dibuja el vidrio. LIQUID_GLASS es el de la app; CLASSIC queda para comparar en debug. */
enum class GlassLook { CLASSIC, LIQUID_GLASS }

/**
 * Colores del líquido "Liquid Glass": brillo (más claro), sombra (más oscura y más saturada, nunca
 * gris) y resplandor (el tono más vivo, para el rebote de luz). Se calculan del color base con
 * tonos vecinos, o vienen de la firma de un elemento especial ([ElementSignatures]).
 */
class LiquidTones(val highlight: Color, val deep: Color, val glow: Color) {
    companion object {
        fun from(base: Color): LiquidTones {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(base.toArgb(), hsv)
            // Tonos vecinos (no solo más claro u oscuro): el líquido pasa por varios matices, como la luz real
            fun tone(hueShift: Float, s: Float, v: Float) =
                Color.hsv((hsv[0] + hueShift + 360f) % 360f, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f), base.alpha)
            return LiquidTones(
                highlight = tone(14f, hsv[1] * 0.75f, hsv[2] * 0.4f + 0.6f),
                deep = tone(-16f, hsv[1] * 1.25f + 0.1f, hsv[2] * 0.5f),
                glow = tone(6f, hsv[1] * 0.95f + 0.05f, 1f),
            )
        }
    }
}

/** Contorno de los recipientes de la repisa y la guía, con el mismo vidrio del recipiente grande. */
@Composable
fun glassOutlineColor(): Color = glassStyle().outline

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
    /** Pro, Android 13+: el vidrio deforma el líquido como una lente. */
    refraction: Boolean = false,
    /** Pantalla principal: el líquido se inclina con el teléfono y hace ondas al tocarlo. */
    interactive: Boolean = false,
    look: GlassLook = GlassLook.LIQUID_GLASS,
    /** Tonos propios (firma de un elemento); si no, se calculan del color del líquido. */
    tones: LiquidTones? = null,
) {
    val glassShader = remember { createGlassShader() }
    val glass = glassStyle()
    val tilt by rememberLiquidTilt(interactive)
    val ripples = remember { Ripples() }
    val haptics = LocalHapticFeedback.current
    // Cada toque vuelve a arrancar el ciclo de cuadros si estaba detenido
    var touches by remember { mutableIntStateOf(0) }
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

    LaunchedEffect(animate, effect, shape, behavior, touches) {
        var last = 0L
        while (currentAnimate || particles.isNotEmpty() || ripples.isActive(System.nanoTime())) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                particles.update(dt, currentFill, currentEffect, currentBehavior, spawn = currentAnimate)
                frameNanos = now
            }
        }
    }

    val touchModifier = if (!interactive) Modifier else Modifier.pointerInput(shape) {
        detectTapGestures { position ->
            val g = VesselGeometry.fit(shape, size.width.toFloat(), size.height.toFloat(), headroom = 0.18f)
            val fill = currentFill
            if (fill <= 0.02f || position.x !in g.left..g.right || position.y !in g.top..g.bottom) return@detectTapGestures
            val u = (position.x - g.left) / g.width
            ripples.add(u, System.nanoTime())
            val surfaceV = g.levelV(fill)
            particles.splash(lane = ((u - 0.5f) / g.halfWidth(surfaceV)).coerceIn(-1f, 1f), fill = fill)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            touches++
        }
    }

    // Capas: halo o aurora, líquido y gotas (a través de la lente del vidrio) y el vidrio con el vapor
    Box(modifier.then(touchModifier)) {
        // Fuera de la lente: los efectos de render recortan a su borde y el halo debe salirse
        Canvas(Modifier.matchParentSize()) {
            val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
            val t = frameNanos / 1_000_000_000f
            if (behavior == ElementBehavior.GLOW) drawAurora(g, animatedFill, animatedColor, t) else drawHalo(g, animatedFill, animatedColor, behavior, t)
        }
        Box(Modifier.matchParentSize().glassRefraction(shape, if (refraction) glassShader else null)) {
            Canvas(Modifier.matchParentSize()) {
                val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
                val t = frameNanos / 1_000_000_000f
                val sloshAge = ((frameNanos - sloshStart) / 1_000_000_000f).takeIf { frameNanos > sloshStart && it < SLOSH_SECONDS }
                // Inclinación (la superficie se mantiene horizontal) y ondas de los toques
                val slope = -tan(tilt)
                val now = frameNanos
                val surfaceOffset: (Float) -> Float = { xn -> slope * (xn - 0.5f) * g.width + ripples.offset(xn, now, g) }
                clipRect {
                    // Estilo plano: el vidrio tiene un tinte propio detrás del líquido
                    val liquidTones = tones ?: LiquidTones.from(animatedColor)
                    if (look == GlassLook.LIQUID_GLASS) drawFrostedBody(g, liquidTones, glass)
                    drawLiquid(
                        g, animatedFill, animatedColor, t, waving = animate || interactive, behavior = behavior,
                        sloshAge = sloshAge, surfaceOffset = surfaceOffset, look = look, tones = liquidTones,
                    )
                    particles.drawInLiquid(this, g, animatedFill, bubbleColor, animatedColor)
                }
            }
            if (behavior == ElementBehavior.METALLIC || behavior == ElementBehavior.GLOW) {
                LiquidBlobs(
                    shape = shape,
                    fill = animatedFill,
                    color = animatedColor,
                    hot = behavior == ElementBehavior.GLOW,
                    time = frameNanos / 1_000_000_000f,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
            clipRect {
                // Descanso: un chorrito cae al centro mientras el recipiente se llena. Se dibuja
                // antes del vidrio y recortado (arriba de la boca y dentro del recipiente), para
                // que el borde quede encima y se vea caer adentro, sin pisar el fondo
                if (animate && effect == LiquidEffect.BUBBLES && animatedFill < 0.97f) {
                    val t = frameNanos / 1_000_000_000f
                    // Las dos partes se juntan en la boca (debajo del labio), sin hueco
                    clipRect(bottom = g.mouthY) { drawPour(g, animatedFill, animatedColor, t) }
                    clipPath(g.interior) { clipRect(top = g.mouthY) { drawPour(g, animatedFill, animatedColor, t) } }
                }
                when (look) {
                    GlassLook.CLASSIC -> drawGlass(g, glass)
                    GlassLook.LIQUID_GLASS -> drawGlassLiquid(g, glass, tones ?: LiquidTones.from(animatedColor))
                }
                particles.drawVapor(
                    this,
                    g,
                    vaporColor = if (behavior == ElementBehavior.COLORED_VAPOR) animatedColor else glass.vapor,
                    sparkColor = animatedColor,
                    condenseColor = glass.vapor,
                )
            }
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
    /** Fase 0..1 del burbujeo suave (recipiente actual de la repisa); null = quieto. */
    bubbles: Float? = null,
    tones: LiquidTones? = null,
) {
    Canvas(modifier) {
        val g = VesselGeometry.fit(shape, size.width, size.height, strokeWidth = 1.5f * density)
        drawLiquid(g, fill, liquidColor, t = 0f, waving = false, look = GlassLook.LIQUID_GLASS, tones = tones)
        if (bubbles != null && fill > 0.08f) {
            clipPath(g.interior) {
                clipRect(top = g.surfaceY(fill)) {
                    // Tres burbujitas que suben desfasadas y se desvanecen al llegar arriba
                    for (i in 0..2) {
                        val k = (bubbles + i / 3f) % 1f
                        val v = g.levelV(fill * k)
                        val u = 0.5f + g.halfWidth(v) * 0.5f * sin(i * 2.1f + k * 6f)
                        drawCircle(Color.White.copy(alpha = 0.6f * (1f - k)), g.stroke * 0.9f, Offset(g.px(u), g.py(v)))
                    }
                }
            }
        }
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

/**
 * Aurora de los luminosos: tres resplandores de tonos vecinos que giran despacio alrededor del
 * recipiente y se mezclan entre sí.
 */
private fun DrawScope.drawAurora(g: VesselGeometry, fill: Float, color: Color, t: Float) {
    if (fill <= 0.02f) return
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val center = Offset(g.px(0.5f), g.py(g.levelV(fill / 2f)))
    val orbit = g.width * 0.28f
    val radius = g.width * 0.85f
    listOf(0f, 35f, -35f).forEachIndexed { i, shift ->
        val hue = (hsv[0] + shift + 360f) % 360f
        val tint = Color.hsv(hue, hsv[1].coerceAtLeast(0.5f), 1f)
        // Cada resplandor gira a su ritmo y en su propio sentido
        val angle = t * (0.25f + 0.1f * i) * (if (i % 2 == 0) 1f else -1f) + i * 2.1f
        val c = Offset(center.x + orbit * cos(angle), center.y + orbit * 0.6f * sin(angle))
        val alpha = 0.22f + 0.05f * sin(t * 1.3f + i)
        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = alpha), Color.Transparent), c, radius), radius, c)
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
    val center = Offset(g.px(0.5f), g.py(g.levelV(fill / 2f)))
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
    /** Desplazamiento extra de la superficie (px) en cada punto 0..1 a lo ancho: inclinación y ondas. */
    surfaceOffset: (Float) -> Float = { 0f },
    look: GlassLook = GlassLook.CLASSIC,
    tones: LiquidTones? = null,
) {
    if (fill <= 0.001f) return
    clipPath(g.interior) {
        val surface = g.surfaceY(fill)
        val amplitude = if (waving) g.unit * 0.012f else 0f
        // Chapoteo: el líquido se inclina de lado a lado y se amortigua en un par de segundos
        val slosh = if (waving && sloshAge != null) g.unit * 0.05f * exp(-sloshAge * 2.2f) * sin(sloshAge * 7f) else 0f

        // Liquid Glass: la superficie sube junto a las paredes (tensión superficial)
        val surfaceV = g.levelV(fill)
        val wallHalf = g.halfWidth(surfaceV).coerceAtLeast(0.01f)
        fun meniscus(xn: Float): Float {
            if (look != GlassLook.LIQUID_GLASS) return 0f
            val e = (abs(xn - 0.5f) / wallHalf).coerceIn(0f, 1f)
            return g.unit * 0.03f * e.pow(6)
        }

        /** Altura de la superficie: dos ondas que viajan en sentidos opuestos, más el chapoteo. */
        fun waveY(xn: Float, phase: Float, scale: Float): Float {
            val a = amplitude * scale
            val x = xn * 2f * PI.toFloat()
            return surface + a * sin(x * 1.5f + t * 2.2f + phase) + a * 0.55f * sin(x * 2.7f - t * 1.5f + phase * 1.7f) +
                slosh * (xn - 0.5f) * 2f + surfaceOffset(xn) - meniscus(xn)
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

        val tones = if (look == GlassLook.LIQUID_GLASS) tones ?: LiquidTones.from(liquid) else null
        // Capa de atrás, un poco más alta y oscura: da profundidad a la superficie
        if (waving) {
            val backColor = tones?.highlight?.copy(alpha = 0.6f) ?: lerp(liquid, Color.Black, 0.25f).copy(alpha = 0.55f)
            drawPath(surfacePath(phase = 2.1f, scale = 1.3f, lift = amplitude * 0.8f, closed = true), backColor)
        }
        val path = surfacePath(phase = 0f, scale = 1f, lift = 0f, closed = true)
        if (tones != null && behavior != ElementBehavior.METALLIC && behavior != ElementBehavior.GLOW) {
            // Liquid Glass: sombra saturada (sin negro) y rebote de luz abajo a la derecha
            drawPath(
                path,
                Brush.linearGradient(
                    0f to tones.highlight.copy(alpha = 0.92f),
                    0.45f to liquid.copy(alpha = 0.95f),
                    0.85f to tones.deep,
                    1f to tones.glow,
                    start = Offset(g.left, surface),
                    end = Offset(g.right, g.bottom),
                ),
            )
            // Brillo interno junto a las paredes (luz que atraviesa el líquido)
            clipPath(path) {
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.White.copy(alpha = 0.22f),
                        0.2f to Color.Transparent,
                        0.8f to Color.Transparent,
                        1f to tones.highlight.copy(alpha = 0.3f),
                        startX = g.left,
                        endX = g.right,
                    ),
                    topLeft = Offset(g.left, surface - g.unit),
                    size = Size(g.width, g.bottom - surface + g.unit),
                    blendMode = BlendMode.Plus,
                )
            }
            drawPath(
                surfacePath(phase = 0f, scale = 1f, lift = 0f, closed = false),
                Color.White.copy(alpha = 0.6f),
                style = Stroke(width = g.stroke * 0.55f, cap = StrokeCap.Round),
            )
            return@clipPath
        }
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

/** Chorrito que cae desde arriba hasta la superficie, ondulando un poco. */
private fun DrawScope.drawPour(g: VesselGeometry, fill: Float, color: Color, t: Float) {
    val start = g.top - g.height * 0.12f
    val end = g.surfaceY(fill)
    if (end <= start) return
    val x = g.px(0.5f)
    val width = g.stroke * 0.8f
    val stream = Path().apply {
        val steps = 16
        for (i in 0..steps) {
            val k = i.toFloat() / steps
            val y = start + (end - start) * k
            // La ondulación crece al caer, como un chorro de verdad
            val wobble = sin(t * 9f + k * 6f) * width * 0.5f * k
            if (i == 0) moveTo(x + wobble, y) else lineTo(x + wobble, y)
        }
    }
    drawPath(stream, color, style = Stroke(width = width, cap = StrokeCap.Butt))
    drawPath(stream, Color.White.copy(alpha = 0.3f), style = Stroke(width = width * 0.3f, cap = StrokeCap.Butt))
}

/** Marcas de medición de cada forma (las comparten los estilos que las usan). */
private fun DrawScope.drawMarks(g: VesselGeometry, color: Color) {
    when (g.shape) {
        VesselShape.BEAKER, VesselShape.TEST_TUBE -> {
            val hw = g.halfWidth(0.5f)
            for (i in 1..4) {
                val v = i / 5f * g.maxFill
                val length = if (i % 2 == 0) hw * 0.35f else hw * 0.2f
                drawLine(color, Offset(g.px(0.5f + hw - length), g.py(v)), Offset(g.px(0.5f + hw - 0.02f), g.py(v)), g.stroke * 0.5f, StrokeCap.Round)
            }
        }
        VesselShape.GRADUATED_CYLINDER -> {
            val hw = g.halfWidth(0.5f)
            for (i in 1..9) {
                val v = g.cylinderFloor + i / 10f * (g.maxFill - g.cylinderFloor)
                val length = when {
                    i % 5 == 0 -> hw * 0.55f
                    i % 2 == 0 -> hw * 0.35f
                    else -> hw * 0.2f
                }
                drawLine(color, Offset(g.px(0.5f + hw - length), g.py(v)), Offset(g.px(0.5f + hw - 0.02f), g.py(v)), g.stroke * 0.45f, StrokeCap.Round)
            }
        }
        VesselShape.VOLUMETRIC_FLASK -> {
            val y = g.py(VesselGeometry.VOLUMETRIC_MARK)
            val hw = g.halfWidth(VesselGeometry.VOLUMETRIC_MARK) * 1.3f
            drawLine(color, Offset(g.px(0.5f - hw), y), Offset(g.px(0.5f + hw), y), g.stroke * 0.6f, StrokeCap.Round)
        }
        else -> Unit
    }
}

/** Dónde poner el reflejo: a la izquierda del cuerpo, a media altura del líquido posible. */
private fun reflexX(g: VesselGeometry): Float = g.px(0.5f - g.halfWidth(g.maxFill * 0.45f) * 0.62f)

/** Liquid Glass: cuerpo de vidrio esmerilado (detrás del líquido) con un tinte del líquido. */
private fun DrawScope.drawFrostedBody(g: VesselGeometry, tones: LiquidTones, glass: GlassStyle) {
    val light = glass === GlassOnLight
    val frost = Brush.linearGradient(
        listOf(
            Color.White.copy(alpha = if (light) 0.7f else 0.13f),
            Color.White.copy(alpha = if (light) 0.35f else 0.04f),
            tones.glow.copy(alpha = if (light) 0.12f else 0.08f),
        ),
        start = Offset(g.left, g.top),
        end = Offset(g.right, g.bottom),
    )
    if (g.lip != null) {
        // Empieza bajo el aro del labio, sin encimarse en él
        clipRect(top = g.wallTop) { drawPath(g.interior, frost) }
        return
    }
    // El vaso no tiene labio: el esmerilado llena el pico y se desvanece hacia la boca, sin un corte recto
    drawContext.canvas.saveLayer(Rect(g.left - g.width, g.top - g.stroke, g.right + g.width, g.bottom + g.stroke), Paint())
    drawPath(g.glassArea, frost)
    drawRect(
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = g.top, endY = g.top + g.height * 0.14f),
        topLeft = Offset(g.left - g.width, g.top - g.stroke),
        size = Size(g.width * 3f, g.height + g.stroke * 2f),
        blendMode = BlendMode.DstIn,
    )
    drawContext.canvas.restore()
}

/**
 * Liquid Glass: reflejo que sigue el contorno y
 * borde especular en diagonal (blanco arriba a la izquierda, teñido del líquido abajo a la derecha).
 */
private fun DrawScope.drawGlassLiquid(g: VesselGeometry, glass: GlassStyle, tones: LiquidTones) {
    val light = glass === GlassOnLight
    // Reflejo que sigue la curva del vidrio (cono, bola, cuello), con un núcleo más nítido
    clipPath(g.interior) {
        clipRect(top = g.wallTop) {
        val ribbon = Path()
        val topV = (g.bottom - g.mouthY) / g.height
        val steps = 48
        val vs = List(steps + 1) { 0.08f + (topV - 0.1f) * it / steps }
        val xs = vs.map { g.halfWidth(it) }
        // Promedio móvil: el reflejo dobla en curva donde el cuello se une al cuerpo, sin quiebre
        val window = 4
        vs.forEachIndexed { i, v ->
            val near = xs.subList(maxOf(0, i - window), minOf(steps, i + window) + 1)
            val x = g.px(0.5f - near.average().toFloat() * 0.68f)
            if (i == 0) ribbon.moveTo(x, g.py(v)) else ribbon.lineTo(x, g.py(v))
        }
        val ribbonW = g.width * 0.075f
        drawPath(ribbon, Color.White.copy(alpha = if (light) 0.6f else 0.26f), style = Stroke(width = ribbonW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(ribbon, Color.White.copy(alpha = if (light) 0.85f else 0.42f), style = Stroke(width = ribbonW * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    }
    drawMarks(g, if (light) Color.Black.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.38f))
    // Borde especular: en el tema claro el blanco no se vería, así que arranca en gris azulado
    val rim = Brush.linearGradient(
        0f to (if (light) Color(0xFF6B7A90) else Color.White).copy(alpha = 0.95f),
        0.35f to (if (light) Color(0xFF9AA6B8) else Color.White).copy(alpha = 0.3f),
        0.7f to tones.highlight.copy(alpha = 0.4f),
        1f to tones.glow.copy(alpha = 0.9f),
        start = Offset(g.left, g.top),
        end = Offset(g.right, g.bottom),
    )
    val rimStroke = Stroke(width = g.stroke * 0.85f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    if (g.shape == VesselShape.GRADUATED_CYLINDER) {
        // El pie de la probeta es vidrio macizo: sin el rebote de color del líquido
        val footTop = g.py(g.cylinderFloor)
        clipRect(top = g.wallTop, bottom = footTop) { drawPath(g.bodyOutline, rim, style = rimStroke) }
        clipRect(top = footTop) {
            drawPath(g.bodyOutline, (if (light) Color(0xFF8A96A8) else Color.White).copy(alpha = 0.55f), style = rimStroke)
        }
    } else {
        clipRect(top = g.wallTop) { drawPath(g.bodyOutline, rim, style = rimStroke) }
    }
    g.lip?.let { (u0, u1, h) ->
        drawRoundRect(rim, topLeft = Offset(g.px(u0), g.top), size = Size(g.px(u1) - g.px(u0), h), cornerRadius = CornerRadius(h / 2), style = Stroke(width = g.stroke * 0.7f))
    }
}

private fun DrawScope.drawGlass(g: VesselGeometry, glass: GlassStyle) {
    val highlight = glass.highlight
    when (g.shape) {
        VesselShape.BEAKER, VesselShape.TEST_TUBE -> {
            // Marcas de medición y reflejo vertical
            val hw = g.halfWidth(0.5f)
            for (i in 1..4) {
                val v = i / 5f * g.maxFill
                val length = if (i % 2 == 0) hw * 0.35f else hw * 0.2f
                drawLine(
                    glass.marks,
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
        VesselShape.GRADUATED_CYLINDER -> {
            // Graduación fina: una marca por décimo, más larga cada cinco
            val hw = g.halfWidth(0.5f)
            for (i in 1..9) {
                val v = g.cylinderFloor + i / 10f * (g.maxFill - g.cylinderFloor)
                val length = when {
                    i % 5 == 0 -> hw * 0.55f
                    i % 2 == 0 -> hw * 0.35f
                    else -> hw * 0.2f
                }
                drawLine(glass.marks, Offset(g.px(0.5f + hw - length), g.py(v)), Offset(g.px(0.5f + hw - 0.02f), g.py(v)), g.stroke * 0.45f, StrokeCap.Round)
            }
            drawLine(highlight, Offset(g.px(0.5f - hw * 0.7f), g.py(0.85f)), Offset(g.px(0.5f - hw * 0.7f), g.py(0.15f)), g.stroke * 1.2f, StrokeCap.Round)
        }
        VesselShape.ROUND_FLASK, VesselShape.VOLUMETRIC_FLASK -> {
            if (g.shape == VesselShape.VOLUMETRIC_FLASK) {
                // Marca de aforo: una línea que rodea el cuello
                val y = g.py(VesselGeometry.VOLUMETRIC_MARK)
                val hw = g.halfWidth(VesselGeometry.VOLUMETRIC_MARK) * 1.3f
                drawLine(glass.outline, Offset(g.px(0.5f - hw), y), Offset(g.px(0.5f + hw), y), g.stroke * 0.6f, StrokeCap.Round)
            }
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
    drawPath(g.outline, glass.outline, style = Stroke(width = g.stroke, cap = StrokeCap.Round))
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

    /** Gotita que salta al tocar el líquido y vuelve a caer. */
    DROP,

    /** Gota de condensación pegada al vidrio sobre el líquido; al rato resbala. */
    CONDENSE,

    /** Anillo que deja una burbuja al reventar en la superficie. */
    RING,
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
    private var condenseBudget = 0f
    private var pourBudget = 0f

    fun isNotEmpty() = particles.isNotEmpty()

    fun update(dt: Float, fill: Float, effect: LiquidEffect, behavior: ElementBehavior?, spawn: Boolean) {
        val rings = ArrayList<Particle>()
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
            val landed = (p.kind == Kind.SPARK || p.kind == Kind.DROP) && p.speed < 0f && p.v < fill
            // La gota de condensación se une al líquido al alcanzarlo
            val merged = p.kind == Kind.CONDENSE && p.v <= fill
            if (popped && p.kind == Kind.BUBBLE) {
                rings += Particle(lane = p.lane, v = fill, speed = 0f, life = 0.6f, size = p.size, phase = 0f, kind = Kind.RING)
            }
            if (p.age >= p.life || popped || stranded || landed || merged) iterator.remove()
        }
        if (particles.size + rings.size <= MAX_PARTICLES) particles += rings

        if (!spawn || effect == LiquidEffect.NONE || dt == 0f || fill <= 0.02f) return

        spawnSpecial(dt, fill, behavior)
        if (effect == LiquidEffect.VAPOR) spawnCondensation(dt, fill)
        if (effect == LiquidEffect.BUBBLES && fill < 0.97f) spawnPourSplash(dt, fill)

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

    /** Trabajo: el vapor deja gotitas en el vidrio, sobre el líquido, que al rato resbalan. */
    private fun spawnCondensation(dt: Float, fill: Float) {
        if (fill > 0.93f || particles.count { it.kind == Kind.CONDENSE } >= MAX_CONDENSE) return
        condenseBudget += 0.6f * dt
        while (condenseBudget >= 1f) {
            condenseBudget -= 1f
            particles += Particle(
                lane = (0.86f + random.nextFloat() * 0.1f) * if (random.nextBoolean()) 1f else -1f,
                v = fill + 0.04f + random.nextFloat() * (0.98f - fill - 0.04f).coerceAtLeast(0f),
                speed = 0f,
                // Se queda pegada unos segundos y luego resbala despacio
                stick = 3f + random.nextFloat() * 6f,
                gravity = -0.012f,
                life = 30f,
                size = 0.006f + random.nextFloat() * 0.01f,
                phase = random.nextFloat() * 6.28f,
                kind = Kind.CONDENSE,
            )
        }
    }

    /** Descanso: gotitas que salpican donde el chorro pega en la superficie. */
    private fun spawnPourSplash(dt: Float, fill: Float) {
        pourBudget += 5f * dt
        while (pourBudget >= 1f && particles.size < MAX_PARTICLES) {
            pourBudget -= 1f
            particles += Particle(
                lane = random.nextFloat() * 0.2f - 0.1f,
                v = fill + 0.005f,
                speed = 0.12f + random.nextFloat() * 0.15f,
                gravity = -1.8f,
                drift = random.nextFloat() * 1.2f - 0.6f,
                life = 0.6f,
                size = 0.006f + random.nextFloat() * 0.006f,
                phase = random.nextFloat() * 6.28f,
                kind = Kind.DROP,
            )
        }
    }

    /** Salpicadura al tocar el líquido: unas gotitas saltan desde la superficie en [lane]. */
    fun splash(lane: Float, fill: Float) {
        repeat(10) {
            if (particles.size >= MAX_PARTICLES) return
            particles += Particle(
                lane = (lane + random.nextFloat() * 0.3f - 0.15f).coerceIn(-1f, 1f),
                v = fill + 0.01f,
                speed = 0.4f + random.nextFloat() * 0.4f,
                gravity = -1.8f,
                drift = random.nextFloat() * 0.8f - 0.4f,
                life = 1.2f,
                size = 0.018f + random.nextFloat() * 0.018f,
                phase = random.nextFloat() * 6.28f,
                kind = Kind.DROP,
            )
        }
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
        val v = g.levelV(p.v)
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
                        Kind.VAPOR, Kind.SPARK, Kind.DROP, Kind.CONDENSE, Kind.RING -> Unit
                    }
                }
            }
        }
    }

    fun drawVapor(scope: DrawScope, g: VesselGeometry, vaporColor: Color, sparkColor: Color, condenseColor: Color) = with(scope) {
        for (p in particles) {
            if (p.kind == Kind.CONDENSE) {
                // Gotita translúcida con un brillo; aparece poco a poco
                val c = position(g, p, 0f)
                val r = g.unit * p.size
                val alpha = (p.age / 1.5f).coerceAtMost(1f)
                drawCircle(condenseColor.copy(alpha = 0.35f * alpha), r, c)
                drawCircle(Color.White.copy(alpha = 0.5f * alpha), r * 0.35f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
                continue
            }
            if (p.kind == Kind.RING) {
                // Anillo que se abre sobre la superficie y se desvanece
                val progress = p.age / p.life
                val c = position(g, p, 0f)
                val r = g.unit * p.size * (1.2f + progress * 3f)
                drawOval(
                    sparkColor.copy(alpha = 0.6f * (1f - progress)),
                    topLeft = Offset(c.x - r, c.y - r * 0.3f),
                    size = Size(2 * r, r * 0.6f),
                    style = Stroke(width = g.stroke * 0.35f),
                )
                continue
            }
            if (p.kind == Kind.SPARK) {
                // Chispa del color de la llama del elemento, con centro blanco
                val fade = 1f - p.age / p.life
                val c = position(g, p, 0.05f)
                val r = g.unit * p.size
                drawCircle(sparkColor.copy(alpha = 0.8f * fade), r * 2.2f, c)
                drawCircle(Color.White.copy(alpha = fade), r, c)
                continue
            }
            if (p.kind == Kind.DROP) {
                // Gotita del color del líquido con un brillo
                val c = position(g, p, 0f)
                val r = g.unit * p.size
                drawCircle(sparkColor, r, c)
                drawCircle(Color.White.copy(alpha = 0.6f), r * 0.35f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
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
        const val MAX_CONDENSE = 16

        /** Altura del fondo, para que nada atraviese el vidrio. */
        const val FLOOR = 0.015f
    }
}

/** Ondas que se abren desde donde se tocó el líquido y se apagan en un par de segundos. */
private class Ripples {
    private class Ripple(val u: Float, val start: Long)

    private val list = ArrayList<Ripple>()

    fun add(u: Float, now: Long) {
        list.removeAll { (now - it.start) / 1_000_000_000f > LIFE }
        if (list.size >= MAX) list.removeAt(0)
        list += Ripple(u, now)
    }

    fun isActive(now: Long) = list.any { (now - it.start) / 1_000_000_000f < LIFE }

    /** Altura de las ondas (px) en el punto [xn] (0..1 a lo ancho). */
    fun offset(xn: Float, now: Long, g: VesselGeometry): Float {
        var sum = 0f
        for (r in list) {
            val age = (now - r.start) / 1_000_000_000f
            if (age !in 0f..LIFE) continue
            // Distancia en "unidades" del recipiente: el frente avanza y deja ondas detrás
            val d = abs(xn - r.u) * g.width / g.unit
            val front = age * 2.4f
            if (d > front) continue
            sum += g.unit * 0.065f * exp(-age * 1.5f) * exp(-d * 1.1f) * sin(d * 9f - age * 13f)
        }
        return sum
    }

    private companion object {
        const val LIFE = 2.5f
        const val MAX = 6
    }
}
