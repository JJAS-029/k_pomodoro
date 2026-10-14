package com.jjas.labpomodoro.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Firmas de elementos especiales: tonos Liquid Glass hechos a mano para que se vean más
 * espectaculares que los calculados (oro cálido, mercurio plateado con rebote azul, etc.). El resto
 * de los elementos usa [LiquidTones.from] con su color.
 */
object ElementSignatures {

    private fun tones(highlight: Long, deep: Long, glow: Long) = LiquidTones(Color(highlight), Color(deep), Color(glow))

    private val signatures: Map<Int, LiquidTones> = mapOf(
        // Litio: carmín de su llama
        3 to tones(0xFFFF8FB1, 0xFF7A0A2E, 0xFFFF2E63),
        // Neón: el naranja rojizo de los letreros
        10 to tones(0xFFFFD2B8, 0xFFB3261E, 0xFFFF5A2A),
        // Sodio: amarillo intenso de las lámparas de calle
        11 to tones(0xFFFFF4A3, 0xFF8A5A00, 0xFFFFC21A),
        // Cloro: verde amarillento
        17 to tones(0xFFF2FFB0, 0xFF4F6B00, 0xFFC6FF3D),
        // Potasio: lila de su llama
        19 to tones(0xFFEAD7FF, 0xFF4B1F7A, 0xFFB57BFF),
        // Cobre: cobrizo con reflejos naranjas
        29 to tones(0xFFFFC9A8, 0xFF6E2A10, 0xFFFF8A50),
        // Bromo: rojo vino que humea
        35 to tones(0xFFFFB08A, 0xFF4A1205, 0xFFFF5A2A),
        // Plata: blanco brillante con rebote frío
        47 to tones(0xFFFFFFFF, 0xFF5D6670, 0xFFDDE7F2),
        // Yodo: violeta profundo
        53 to tones(0xFFE2B8FF, 0xFF2E0F4F, 0xFF9C4DFF),
        // Platino: gris perla cálido
        78 to tones(0xFFF8F4EE, 0xFF6B645C, 0xFFE8DDCC),
        // Oro: dorado cálido con brillo
        79 to tones(0xFFFFF3B0, 0xFF8A5A00, 0xFFFFC94A),
        // Mercurio: espejo plateado con rebote azul
        80 to tones(0xFFF5F7FA, 0xFF4A5560, 0xFF9EC9FF),
        // Radio: verde fosforescente
        88 to tones(0xFFC9FFE0, 0xFF0B5A2E, 0xFF5CFF9E),
        // Uranio: verde amarillo del vidrio de uranio
        92 to tones(0xFFF0FFB0, 0xFF3D5A00, 0xFFB8FF3D),
        // Plutonio: rojo que brilla por su propio calor
        94 to tones(0xFFFFB8A0, 0xFF6B0F05, 0xFFFF4A1F),
    )

    /** La firma del elemento, o null si usa los tonos calculados de su color. */
    fun tones(atomicNumber: Int): LiquidTones? = signatures[atomicNumber]
}
