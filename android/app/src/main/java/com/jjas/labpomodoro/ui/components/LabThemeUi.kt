package com.jjas.labpomodoro.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.FlameColor
import com.jjas.labpomodoro.domain.model.GlassTint
import com.jjas.labpomodoro.domain.model.LabBackground

/** Tema de laboratorio activo: tinte del cristal (null = transparente), colores de la llama y fondo. */
data class LabThemeUi(
    val glassTint: Color? = null,
    val flame: FlameTones = FlameTones.Blue,
    val background: LabBackground = LabBackground.NONE,
) {
    companion object {
        fun of(tint: GlassTint, flame: FlameColor, background: LabBackground) =
            LabThemeUi(tint.argb?.let(::Color), FlameTones.of(flame), background)
    }
}

/** Lo fija la actividad a partir de los ajustes; los recipientes y el mechero lo leen. */
val LocalLabTheme = staticCompositionLocalOf { LabThemeUi() }
