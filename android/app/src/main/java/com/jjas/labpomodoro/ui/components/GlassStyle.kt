package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory
import com.jjas.labpomodoro.domain.model.MasteryRules

/**
 * Colores de "cristal líquido" de una familia: todos salen del mismo tono, así las sombras son
 * versiones más hondas del color y nunca un gris sucio.
 */
class GlassPalette(
    val top: Color,
    val mid: Color,
    val deep: Color,
    val border: Color,
    val number: Color,
    val symbolShadow: Color,
    val dimTop: Color,
    val dimDeep: Color,
    val dimBorder: Color,
    /** Color pleno del tono, para orbes y acentos (leyenda, barras). */
    val orb: Color,
)

private fun paletteOf(color: Color, light: Boolean): GlassPalette {
    val (h, s, _) = color.toHsl()
    // Los pasteles de la tabla tienen poca saturación en HSL; se sube un poco para que el cristal
    // tenga color, salvo los grises (desconocidos), que se quedan grises
    val sat = if (s < 0.05f) s else (s * 1.1f).coerceAtMost(0.9f)
    val dimSat = sat * 0.2f
    return if (light) {
        // Sobre blanco el cristal va más opaco y hondo para que el símbolo blanco se lea
        GlassPalette(
            top = Color.hsl(h, sat, 0.60f, 0.78f),
            mid = Color.hsl(h, sat, 0.42f, 0.82f),
            deep = Color.hsl(h, sat, 0.22f, 0.90f),
            border = Color.hsl(h, sat, 0.80f, 0.60f),
            // La esquina del número es la más clara del cristal: sobre blanco va oscuro para leerse
            number = Color.hsl(h, (sat * 0.8f).coerceAtMost(0.7f), 0.16f),
            symbolShadow = Color.hsl(h, sat, 0.15f, 0.85f),
            dimTop = Color.hsl(h, dimSat, 0.55f, 0.10f),
            dimDeep = Color.hsl(h, dimSat, 0.35f, 0.18f),
            dimBorder = Color.hsl(h, dimSat, 0.45f, 0.30f),
            orb = Color.hsl(h, sat.coerceAtLeast(if (s < 0.05f) 0f else 0.55f), 0.45f),
        )
    } else {
        GlassPalette(
            top = Color.hsl(h, sat, 0.72f, 0.32f),
            mid = Color.hsl(h, sat, 0.48f, 0.16f),
            deep = Color.hsl(h, sat, 0.20f, 0.55f),
            border = Color.hsl(h, sat, 0.75f, 0.45f),
            number = Color.hsl(h, 0.95f, 0.82f),
            symbolShadow = Color.hsl(h, sat, 0.45f, 0.9f),
            dimTop = Color.hsl(h, dimSat, 0.60f, 0.08f),
            dimDeep = Color.hsl(h, dimSat, 0.30f, 0.16f),
            dimBorder = Color.hsl(h, dimSat, 0.70f, 0.20f),
            orb = Color.hsl(h, sat, 0.70f),
        )
    }
}

// Una sola vez por familia y tema: la tabla dibuja 118 casillas
private val darkPalettes = ElementCategory.entries.associateWith { paletteOf(it.color(), light = false) }
private val lightPalettes = ElementCategory.entries.associateWith { paletteOf(it.color(), light = true) }

fun ElementCategory.glass(light: Boolean): GlassPalette =
    (if (light) lightPalettes else darkPalettes).getValue(this)

/** RGB → (tono en grados, saturación, luminosidad), lo que pide [Color.hsl]. */
private fun Color.toHsl(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        red -> ((green - blue) / d + (if (green < blue) 6f else 0f))
        green -> (blue - red) / d + 2f
        else -> (red - green) / d + 4f
    } * 60f
    return Triple(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), l)
}

/**
 * Fase (0..1) del destello de plata y oro. Una sola animación compartida por toda la pantalla:
 * crear una por casilla con 118 en pantalla sería un desperdicio. Si nadie la provee, cada
 * casilla brillante crea la suya (pantallas con una o dos casillas sueltas).
 */
val LocalGlassShimmer = compositionLocalOf<State<Float>?> { null }

/** Duración de una vuelta del destello. */
private const val SHIMMER_MS = 3_200

@Composable
fun rememberGlassShimmer(): State<Float> =
    rememberInfiniteTransition(label = "cristal").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SHIMMER_MS, easing = LinearEasing)),
        label = "destello",
    )

/** Avance (0..1) hacia el siguiente nivel de maestría; 1 si ya es oro. */
fun masteryProgress(element: Element, obtained: Int): Float {
    val (bronze, silver, gold) = MasteryRules.thresholds(element)
    val (from, to) = when {
        obtained < bronze -> 0 to bronze
        obtained < silver -> bronze to silver
        obtained < gold -> silver to gold
        else -> return 1f
    }
    return ((obtained - from).toFloat() / (to - from)).coerceIn(0f, 1f)
}
