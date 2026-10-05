package com.jjas.labpomodoro.ui.components

import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory

/**
 * Cómo se comporta un elemento dentro del recipiente del timer. Son guiños a su química real,
 * no simulaciones exactas.
 */
enum class ElementBehavior(val description: String) {
    /** Disuelto: solo da color, como los iones de los metales de transición en agua. */
    SOLUTION("Tiñe el líquido con el color de sus sales disueltas."),

    /** Metales alcalinos: reaccionan con el agua soltando hidrógeno y chispas. */
    REACTIVE("Reacciona con el agua: burbujea con fuerza y suelta chispas del color de su llama."),

    /** Prueba de la llama: chispas del color con el que arde. */
    FLAME("Suelta chispas del color con el que arde en la prueba de la llama."),

    /** Gases nobles y fósforos: brillan como un letrero luminoso. */
    GLOW("Brilla como dentro de un tubo de descarga o un fósforo luminiscente."),

    /** Metales: el líquido se vuelve opaco y con reflejos de espejo. */
    METALLIC("Se ve como metal fundido, con reflejos que recorren la superficie."),

    /** Halógenos: sueltan vapor de color. */
    COLORED_VAPOR("Se evapora en un vapor de su color, como los halógenos."),

    /** Sólidos que no se disuelven: cristales que se asientan en el fondo. */
    PRECIPITATE("No se disuelve: forma cristales que caen y se asientan en el fondo."),

    /** Gases licuados: hierven y desbordan una niebla fría. */
    CRYO("Está licuado a temperaturas bajísimas: hierve y desborda niebla fría."),

    /** Radiactivos: brillo que late y destellos de partículas. */
    RADIOACTIVE("Es radiactivo: late con un brillo propio y suelta destellos."),
}

data class ElementLook(val color: Color, val behavior: ElementBehavior)

// Colores tomados de su aspecto real, del color de sus iones en agua o de su llama
private val LOOKS: Map<Int, ElementLook> = mapOf(
    1 to ElementLook(Color(0xFFB3E5FC), ElementBehavior.SOLUTION),
    2 to ElementLook(Color(0xFFFFAB91), ElementBehavior.GLOW),
    3 to ElementLook(Color(0xFFE53950), ElementBehavior.REACTIVE),
    4 to ElementLook(Color(0xFFB0BEC5), ElementBehavior.METALLIC),
    5 to ElementLook(Color(0xFF7CB342), ElementBehavior.FLAME),
    6 to ElementLook(Color(0xFF4E4E4E), ElementBehavior.PRECIPITATE),
    7 to ElementLook(Color(0xFFBBDEFB), ElementBehavior.CRYO),
    8 to ElementLook(Color(0xFF81D4FA), ElementBehavior.CRYO),
    9 to ElementLook(Color(0xFFF0F4A0), ElementBehavior.COLORED_VAPOR),
    10 to ElementLook(Color(0xFFFF5722), ElementBehavior.GLOW),
    11 to ElementLook(Color(0xFFFFC107), ElementBehavior.REACTIVE),
    12 to ElementLook(Color(0xFFF5F5F5), ElementBehavior.FLAME),
    13 to ElementLook(Color(0xFFCFD8DC), ElementBehavior.METALLIC),
    14 to ElementLook(Color(0xFF78909C), ElementBehavior.PRECIPITATE),
    15 to ElementLook(Color(0xFFB2FF59), ElementBehavior.GLOW),
    16 to ElementLook(Color(0xFFFFEB3B), ElementBehavior.PRECIPITATE),
    17 to ElementLook(Color(0xFFC6E36B), ElementBehavior.COLORED_VAPOR),
    18 to ElementLook(Color(0xFFB388FF), ElementBehavior.GLOW),
    19 to ElementLook(Color(0xFFCE93D8), ElementBehavior.REACTIVE),
    20 to ElementLook(Color(0xFFFF7043), ElementBehavior.FLAME),
    21 to ElementLook(Color(0xFFE0E0E0), ElementBehavior.METALLIC),
    22 to ElementLook(Color(0xFF9FA8DA), ElementBehavior.METALLIC),
    23 to ElementLook(Color(0xFF5C6BC0), ElementBehavior.SOLUTION),
    24 to ElementLook(Color(0xFFFF9800), ElementBehavior.SOLUTION),
    25 to ElementLook(Color(0xFF8E24AA), ElementBehavior.SOLUTION),
    26 to ElementLook(Color(0xFFD87C2A), ElementBehavior.SOLUTION),
    27 to ElementLook(Color(0xFFF06292), ElementBehavior.SOLUTION),
    28 to ElementLook(Color(0xFF66BB6A), ElementBehavior.SOLUTION),
    29 to ElementLook(Color(0xFF29B6F6), ElementBehavior.SOLUTION),
    30 to ElementLook(Color(0xFFB0BEC5), ElementBehavior.METALLIC),
    31 to ElementLook(Color(0xFFCFD8DC), ElementBehavior.METALLIC),
    32 to ElementLook(Color(0xFF90A4AE), ElementBehavior.PRECIPITATE),
    33 to ElementLook(Color(0xFF9E9E9E), ElementBehavior.PRECIPITATE),
    34 to ElementLook(Color(0xFFE53935), ElementBehavior.PRECIPITATE),
    35 to ElementLook(Color(0xFFB23C17), ElementBehavior.COLORED_VAPOR),
    36 to ElementLook(Color(0xFFE1F5FE), ElementBehavior.GLOW),
    37 to ElementLook(Color(0xFFC2185B), ElementBehavior.REACTIVE),
    38 to ElementLook(Color(0xFFD50000), ElementBehavior.FLAME),
    42 to ElementLook(Color(0xFF64B5F6), ElementBehavior.SOLUTION),
    46 to ElementLook(Color(0xFFD7CCC8), ElementBehavior.METALLIC),
    47 to ElementLook(Color(0xFFEEEEEE), ElementBehavior.METALLIC),
    48 to ElementLook(Color(0xFFFFD54F), ElementBehavior.PRECIPITATE),
    50 to ElementLook(Color(0xFFBDBDBD), ElementBehavior.METALLIC),
    51 to ElementLook(Color(0xFFB0BEC5), ElementBehavior.PRECIPITATE),
    52 to ElementLook(Color(0xFF8D6E63), ElementBehavior.PRECIPITATE),
    53 to ElementLook(Color(0xFF7B1FA2), ElementBehavior.COLORED_VAPOR),
    54 to ElementLook(Color(0xFF64B5F6), ElementBehavior.GLOW),
    55 to ElementLook(Color(0xFF7986CB), ElementBehavior.REACTIVE),
    56 to ElementLook(Color(0xFF9CCC65), ElementBehavior.FLAME),
    58 to ElementLook(Color(0xFFFFF176), ElementBehavior.GLOW),
    59 to ElementLook(Color(0xFF9CCC65), ElementBehavior.SOLUTION),
    60 to ElementLook(Color(0xFFB39DDB), ElementBehavior.SOLUTION),
    63 to ElementLook(Color(0xFFFF1744), ElementBehavior.GLOW),
    65 to ElementLook(Color(0xFF76FF03), ElementBehavior.GLOW),
    68 to ElementLook(Color(0xFFF8BBD0), ElementBehavior.SOLUTION),
    74 to ElementLook(Color(0xFF9E9E9E), ElementBehavior.METALLIC),
    78 to ElementLook(Color(0xFFCFD8DC), ElementBehavior.METALLIC),
    79 to ElementLook(Color(0xFFFFD54F), ElementBehavior.METALLIC),
    80 to ElementLook(Color(0xFFBDBDBD), ElementBehavior.METALLIC),
    82 to ElementLook(Color(0xFF78909C), ElementBehavior.METALLIC),
    83 to ElementLook(Color(0xFFBA68C8), ElementBehavior.METALLIC),
    84 to ElementLook(Color(0xFF4FC3F7), ElementBehavior.RADIOACTIVE),
    85 to ElementLook(Color(0xFF4A148C), ElementBehavior.COLORED_VAPOR),
    86 to ElementLook(Color(0xFFEF5350), ElementBehavior.GLOW),
    87 to ElementLook(Color(0xFFFF8A65), ElementBehavior.REACTIVE),
    88 to ElementLook(Color(0xFF80DEEA), ElementBehavior.RADIOACTIVE),
    89 to ElementLook(Color(0xFF4FC3F7), ElementBehavior.RADIOACTIVE),
    92 to ElementLook(Color(0xFFC6FF00), ElementBehavior.RADIOACTIVE),
    94 to ElementLook(Color(0xFFFF7043), ElementBehavior.RADIOACTIVE),
)

/** Aspecto en el recipiente: el de la tabla, o uno por familia si no tiene uno propio. */
fun Element.look(): ElementLook = LOOKS[atomicNumber] ?: ElementLook(
    color = category.color(),
    behavior = when {
        atomicNumber == 43 || atomicNumber == 61 || atomicNumber > 92 -> ElementBehavior.RADIOACTIVE
        else -> when (category) {
            ElementCategory.ALKALI_METAL -> ElementBehavior.REACTIVE
            ElementCategory.ALKALINE_EARTH_METAL -> ElementBehavior.FLAME
            ElementCategory.TRANSITION_METAL, ElementCategory.POST_TRANSITION_METAL -> ElementBehavior.METALLIC
            ElementCategory.METALLOID, ElementCategory.NONMETAL -> ElementBehavior.PRECIPITATE
            ElementCategory.HALOGEN -> ElementBehavior.COLORED_VAPOR
            ElementCategory.NOBLE_GAS, ElementCategory.LANTHANIDE -> ElementBehavior.GLOW
            ElementCategory.ACTINIDE, ElementCategory.UNKNOWN -> ElementBehavior.RADIOACTIVE
        }
    },
)
