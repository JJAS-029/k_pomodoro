package com.jjas.labpomodoro.domain.model

import androidx.annotation.StringRes
import com.jjas.labpomodoro.R

/** Tinte del cristal de los recipientes (Pro). [argb] null = transparente, como hasta ahora. */
enum class GlassTint(@StringRes val labelRes: Int, val argb: Long?) {
    CLEAR(R.string.set_tint_clear, null),
    GREEN(R.string.set_tint_green, 0xFF7CFFB2),
    AMBER(R.string.set_tint_amber, 0xFFFFC46B),
    BLUE(R.string.set_tint_blue, 0xFF7FC8FF),
    ROSE(R.string.set_tint_rose, 0xFFFF9EC7),
}

/**
 * Color de la llama del mechero (Pro), con los colores de la prueba de la flama: cada sal metálica
 * tiñe la llama de un color.
 */
enum class FlameColor(@StringRes val labelRes: Int, val swatch: Long) {
    BLUE(R.string.set_flame_blue, 0xFF448AFF),
    GREEN(R.string.set_flame_green, 0xFF4CFF8A),
    VIOLET(R.string.set_flame_violet, 0xFFC77DFF),
    ORANGE(R.string.set_flame_orange, 0xFFFFB347),
    RED(R.string.set_flame_red, 0xFFFF5A6E),
}

/** Fondo de laboratorio detrás del recipiente (Pro). */
enum class LabBackground(@StringRes val labelRes: Int) {
    NONE(R.string.set_bg_none),
    TILES(R.string.set_bg_tiles),
    GRAPH(R.string.set_bg_graph),
    FORMULAS(R.string.set_bg_formulas),
}
