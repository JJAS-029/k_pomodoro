package com.jjas.labpomodoro.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.BurnerStyle
import com.jjas.labpomodoro.domain.model.FlameColor
import com.jjas.labpomodoro.domain.model.GlassTint
import com.jjas.labpomodoro.domain.model.LabBackground

/**
 * Tema de laboratorio activo: tinte del cristal (null = transparente), colores de la llama, fondo
 * y si se usan los colores del sistema (Material You, Pro).
 */
data class LabThemeUi(
    val glassTint: Color? = null,
    val flame: FlameTones = FlameTones.Blue,
    val background: LabBackground = LabBackground.NONE,
    val dynamicColor: Boolean = false,
    val burner: BurnerStyle = BurnerStyle.BUNSEN,
) {
    companion object {
        fun of(tint: GlassTint, flame: FlameColor, background: LabBackground, dynamicColor: Boolean, burner: BurnerStyle) =
            LabThemeUi(tint.argb?.let(::Color), FlameTones.of(flame), background, dynamicColor, burner)
    }
}

/** Lo fija la actividad a partir de los ajustes; los recipientes y el mechero lo leen. */
val LocalLabTheme = staticCompositionLocalOf { LabThemeUi() }
