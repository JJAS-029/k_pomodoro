package com.jjas.labpomodoro.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.FlameColor
import com.jjas.labpomodoro.domain.model.GlassTint

/** Tema de laboratorio activo: tinte del cristal (null = transparente) y colores de la llama. */
data class LabThemeUi(val glassTint: Color? = null, val flame: FlameTones = FlameTones.Blue) {
    companion object {
        fun of(tint: GlassTint, flame: FlameColor) = LabThemeUi(tint.argb?.let(::Color), FlameTones.of(flame))
    }
}

/** Lo fija la actividad a partir de los ajustes; los recipientes y el mechero lo leen. */
val LocalLabTheme = staticCompositionLocalOf { LabThemeUi() }
