package com.jjas.labpomodoro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jjas.labpomodoro.domain.model.LabPalette
import com.jjas.labpomodoro.domain.model.SessionType
import kotlin.random.Random

/** Colores del líquido: la paleta clásica es la del prototipo web; las demás son de Pro. */
object LiquidPalette {

    /** Tono (0–360) del líquido: al azar en trabajo, pero estable para la misma sesión del mismo plan. */
    fun hue(type: SessionType, planSeed: Long, index: Int, palette: LabPalette = LabPalette.CLASSIC): Float = when (type) {
        // Mismo número al azar que antes: con la paleta clásica los colores no cambian
        SessionType.WORK -> palette.hueAt(Random(planSeed + index).nextInt(360) / 360f)
        else -> hueOf(liquid(type, planSeed, index, palette))
    }

    fun liquid(type: SessionType, planSeed: Long, index: Int, palette: LabPalette = LabPalette.CLASSIC): Color = when (type) {
        SessionType.WORK -> Color.hsl(hue(type, planSeed, index, palette), palette.saturation, palette.lightness)
        SessionType.SHORT_BREAK -> Color(palette.shortBreak)
        SessionType.LONG_BREAK -> Color(palette.longBreak)
    }

    private fun hueOf(color: Color): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv[0]
    }

    /**
     * Cada sesión del plan usa un recipiente al azar, estable mientras dure el plan: de los de
     * siempre y los que se compraron en la tienda ([owned], nombres de [VesselShape]).
     */
    fun vessel(planSeed: Long, index: Int, owned: Set<String> = emptySet()): VesselShape {
        val pool = VesselShape.entries.filter { it in VesselShape.BASE || it.name in owned }
        return pool[Random(planSeed * 31 + index + 7).nextInt(pool.size)]
    }

    /** Burbujas del tono opuesto para que contrasten con el líquido. */
    fun bubble(type: SessionType, planSeed: Long, index: Int, palette: LabPalette = LabPalette.CLASSIC): Color =
        Color.hsl((hue(type, planSeed, index, palette) + 180f) % 360f, 0.9f, 0.7f, alpha = 0.7f)
}
