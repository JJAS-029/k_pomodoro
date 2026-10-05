package com.jjas.labpomodoro.ui.components

import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.SessionType
import kotlin.random.Random

/** Colores del líquido, iguales a los del prototipo web. */
object LiquidPalette {

    private val ShortBreak = Color(0xFF2ECC71)
    private val LongBreak = Color(0xFFE74C3C)

    /** Tono (0–360) del líquido: al azar en trabajo, pero estable para la misma sesión del mismo plan. */
    fun hue(type: SessionType, planSeed: Long, index: Int): Float = when (type) {
        SessionType.WORK -> Random(planSeed + index).nextInt(360).toFloat()
        SessionType.SHORT_BREAK -> 120f
        SessionType.LONG_BREAK -> 0f
    }

    fun liquid(type: SessionType, planSeed: Long, index: Int): Color = when (type) {
        SessionType.WORK -> Color.hsl(hue(type, planSeed, index), 0.8f, 0.6f)
        SessionType.SHORT_BREAK -> ShortBreak
        SessionType.LONG_BREAK -> LongBreak
    }

    /** Cada sesión del plan usa un recipiente al azar, estable mientras dure el plan. */
    fun vessel(planSeed: Long, index: Int): VesselShape =
        VesselShape.entries[Random(planSeed * 31 + index + 7).nextInt(VesselShape.entries.size)]

    /** Burbujas del tono opuesto para que contrasten con el líquido. */
    fun bubble(type: SessionType, planSeed: Long, index: Int): Color =
        Color.hsl((hue(type, planSeed, index) + 180f) % 360f, 0.9f, 0.7f, alpha = 0.7f)
}
