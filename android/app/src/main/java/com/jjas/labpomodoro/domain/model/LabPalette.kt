package com.jjas.labpomodoro.domain.model

import androidx.annotation.StringRes
import com.jjas.labpomodoro.R

/**
 * Paletas de laboratorio (Pro): de qué gama salen los colores de los pomodoros y qué color tiene
 * cada descanso. Los recipientes con un elemento de la colección conservan el color del elemento.
 *
 * @param hues rangos de tono (0..360) de donde se elige el de cada pomodoro.
 * @param shortBreak, longBreak colores ARGB de los descansos.
 */
enum class LabPalette(
    @StringRes val labelRes: Int,
    val hues: List<ClosedFloatingPointRange<Float>>,
    val saturation: Float,
    val lightness: Float,
    val shortBreak: Long,
    val longBreak: Long,
) {
    CLASSIC(R.string.set_palette_classic, listOf(0f..360f), 0.8f, 0.6f, 0xFF2ECC71, 0xFFE74C3C),
    NEON(R.string.set_palette_neon, listOf(290f..360f, 0f..15f, 165f..200f, 85f..130f), 1f, 0.58f, 0xFF00FFC6, 0xFFFF2E9A),
    SUNSET(R.string.set_palette_sunset, listOf(330f..360f, 0f..50f), 0.85f, 0.6f, 0xFFFFB347, 0xFFFF5E62),
    OCEAN(R.string.set_palette_ocean, listOf(165f..235f), 0.75f, 0.55f, 0xFF4DD0E1, 0xFF3949AB),
    AURORA(R.string.set_palette_aurora, listOf(100f..180f, 260f..300f), 0.8f, 0.62f, 0xFF69F0AE, 0xFFB388FF),
    PASTEL(R.string.set_palette_pastel, listOf(0f..360f), 0.6f, 0.78f, 0xFFA8E6CF, 0xFFFFB7B2),
    ;

    /** Tono de un pomodoro a partir de un número 0..1: los rangos pesan según lo anchos que son. */
    fun hueAt(fraction: Float): Float {
        val total = hues.sumOf { (it.endInclusive - it.start).toDouble() }.toFloat()
        var left = fraction.coerceIn(0f, 1f) * total
        for (range in hues) {
            val width = range.endInclusive - range.start
            if (left <= width) return (range.start + left) % 360f
            left -= width
        }
        return hues.last().endInclusive % 360f
    }
}
