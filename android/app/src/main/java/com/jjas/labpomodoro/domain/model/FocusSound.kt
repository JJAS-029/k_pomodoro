package com.jjas.labpomodoro.domain.model

import androidx.annotation.StringRes
import com.jjas.labpomodoro.R

/**
 * Sonidos de fondo para concentrarse. Todos se generan en el teléfono (sin archivos de audio):
 * ruido con distintos "colores" y dos ambientes hechos a partir de ruido filtrado.
 */
enum class FocusSound(@StringRes val labelRes: Int, @StringRes val descriptionRes: Int?) {
    OFF(R.string.set_sound_off, null),
    WHITE(R.string.set_sound_white, R.string.set_sound_white_desc),
    PINK(R.string.set_sound_pink, R.string.set_sound_pink_desc),
    BROWN(R.string.set_sound_brown, R.string.set_sound_brown_desc),
    RAIN(R.string.set_sound_rain, R.string.set_sound_rain_desc),
    WAVES(R.string.set_sound_waves, R.string.set_sound_waves_desc),
}
