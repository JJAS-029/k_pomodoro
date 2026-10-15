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
 * Colores "neón vial" de una familia: la casilla es un frasco casi negro (casi blanco en el tema
 * claro) con borde luminoso y el líquido de la familia adentro. Todos salen del mismo matiz.
 */
class GlassPalette(
    /** Cuerpo del frasco, de arriba a abajo. */
    val top: Color,
    val mid: Color,
    val deep: Color,
    /** Borde neón. */
    val border: Color,
    val number: Color,
    val symbol: Color,
    val symbolShadow: Color,
    val liquidTop: Color,
    val liquidDeep: Color,
    val dimTop: Color,
    val dimDeep: Color,
    val dimBorder: Color,
    /** Color pleno del tono, para orbes y acentos (leyenda, barras). */
    val orb: Color,
)

private fun paletteOf(category: ElementCategory, light: Boolean): GlassPalette {
    val h = category.hue()
    val s = category.saturation()
    val vivid = (s + 0.2f).coerceAtMost(1f)
    return if (light) {
        GlassPalette(
            top = Color.hsl(h, s * 0.3f, 0.98f),
            mid = Color.hsl(h, s * 0.3f, 0.95f),
            deep = Color.hsl(h, s * 0.35f, 0.91f),
            border = Color.hsl(h, vivid, 0.45f),
            number = Color.hsl(h, vivid, 0.32f),
            symbol = Color.hsl(h, vivid * 0.8f, 0.2f),
            symbolShadow = Color.White.copy(alpha = 0.9f),
            liquidTop = Color.hsl(h, s, 0.72f, 0.9f),
            liquidDeep = Color.hsl(h, vivid, 0.55f, 0.95f),
            dimTop = Color.hsl(h, s * 0.15f, 0.96f),
            dimDeep = Color.hsl(h, s * 0.15f, 0.92f),
            dimBorder = Color.hsl(h, s * 0.3f, 0.75f, 0.8f),
            orb = Color.hsl(h, s, 0.5f),
        )
    } else {
        GlassPalette(
            top = Color.hsl(h, s * 0.5f, 0.11f),
            mid = Color.hsl(h, s * 0.4f, 0.06f),
            deep = Color(0xFF040507),
            border = Color.hsl(h, vivid, 0.62f),
            number = Color.hsl(h, vivid, 0.76f),
            symbol = Color.White,
            symbolShadow = Color.hsl(h, vivid, 0.55f, 0.95f),
            liquidTop = Color.hsl(h, s, 0.58f, 0.85f),
            liquidDeep = Color.hsl(h, vivid, 0.3f, 0.95f),
            dimTop = Color.hsl(h, s * 0.15f, 0.1f),
            dimDeep = Color(0xFF050507),
            dimBorder = Color.hsl(h, s * 0.3f, 0.35f, 0.6f),
            orb = Color.hsl(h, vivid, 0.62f),
        )
    }
}

// Una sola vez por familia y tema: la tabla dibuja 118 casillas
private val darkPalettes = ElementCategory.entries.associateWith { paletteOf(it, light = false) }
private val lightPalettes = ElementCategory.entries.associateWith { paletteOf(it, light = true) }

fun ElementCategory.glass(light: Boolean): GlassPalette =
    (if (light) lightPalettes else darkPalettes).getValue(this)

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
