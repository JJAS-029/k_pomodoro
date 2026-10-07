package com.jjas.labpomodoro.ui.components

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory

/**
 * Cómo se comporta un elemento dentro del recipiente del timer. Son guiños a su química real,
 * no simulaciones exactas.
 */
enum class ElementBehavior(@StringRes val descriptionRes: Int) {
    /** Disuelto: solo da color, como los iones de los metales de transición en agua. */
    SOLUTION(R.string.el_behavior_solution),

    /** Metales alcalinos: reaccionan con el agua soltando hidrógeno y chispas. */
    REACTIVE(R.string.el_behavior_reactive),

    /** Prueba de la llama: chispas del color con el que arde. */
    FLAME(R.string.el_behavior_flame),

    /** Gases nobles, fósforos, filamentos y láseres: luz propia. */
    GLOW(R.string.el_behavior_glow),

    /** Metales: el líquido se vuelve opaco y con reflejos de espejo. */
    METALLIC(R.string.el_behavior_metallic),

    /** Halógenos: sueltan vapor de color. */
    COLORED_VAPOR(R.string.el_behavior_colored_vapor),

    /** Sólidos que no se disuelven: cristales que se asientan en el fondo. */
    PRECIPITATE(R.string.el_behavior_precipitate),

    /** Gases licuados: hierven y desbordan una niebla fría. */
    CRYO(R.string.el_behavior_cryo),

    /** Radiactivos: brillo que late y destellos de partículas. */
    RADIOACTIVE(R.string.el_behavior_radioactive),
}

/** De dónde sale su color está en el recurso `element_look_origins` (ver [elementLookOrigin]). */
data class ElementLook(val color: Color, val behavior: ElementBehavior)

// Los 118 tienen color y comportamiento propios, tomados de algo real: su aspecto, el color de sus
// iones en agua, el de su llama o su luminiscencia. Para los superpesados (100+) no se conoce: se
// reparten tonos distintos para que ninguno se repita
private val LOOKS: Map<Int, ElementLook> = mapOf(
    1 to ElementLook(Color(0xFFD9A3E8), ElementBehavior.GLOW),
    2 to ElementLook(Color(0xFFFFB3C1), ElementBehavior.GLOW),
    3 to ElementLook(Color(0xFFD81B60), ElementBehavior.REACTIVE),
    4 to ElementLook(Color(0xFF2BBF8A), ElementBehavior.PRECIPITATE),
    5 to ElementLook(Color(0xFF7EE04A), ElementBehavior.FLAME),
    6 to ElementLook(Color(0xFF3A3A3A), ElementBehavior.PRECIPITATE),
    7 to ElementLook(Color(0xFFDCEFF7), ElementBehavior.CRYO),
    8 to ElementLook(Color(0xFF7FC8F8), ElementBehavior.CRYO),
    9 to ElementLook(Color(0xFFF2F29A), ElementBehavior.COLORED_VAPOR),
    10 to ElementLook(Color(0xFFFF4A1C), ElementBehavior.GLOW),
    11 to ElementLook(Color(0xFFFFD000), ElementBehavior.REACTIVE),
    12 to ElementLook(Color(0xFFF7F7F7), ElementBehavior.FLAME),
    13 to ElementLook(Color(0xFFA9B0B5), ElementBehavior.METALLIC),
    14 to ElementLook(Color(0xFF6F7F96), ElementBehavior.PRECIPITATE),
    15 to ElementLook(Color(0xFF9CFF57), ElementBehavior.GLOW),
    16 to ElementLook(Color(0xFFEDE64A), ElementBehavior.PRECIPITATE),
    17 to ElementLook(Color(0xFFBEDB55), ElementBehavior.COLORED_VAPOR),
    18 to ElementLook(Color(0xFFA57CFF), ElementBehavior.GLOW),
    19 to ElementLook(Color(0xFFC58BE8), ElementBehavior.REACTIVE),
    20 to ElementLook(Color(0xFFFF6A3D), ElementBehavior.FLAME),
    21 to ElementLook(Color(0xFFEFF7C8), ElementBehavior.GLOW),
    22 to ElementLook(Color(0xFF9D8BB0), ElementBehavior.METALLIC),
    23 to ElementLook(Color(0xFF7E57C2), ElementBehavior.SOLUTION),
    24 to ElementLook(Color(0xFFFF7A1A), ElementBehavior.SOLUTION),
    25 to ElementLook(Color(0xFF9C1E8F), ElementBehavior.SOLUTION),
    26 to ElementLook(Color(0xFFC1611F), ElementBehavior.SOLUTION),
    27 to ElementLook(Color(0xFFF48FB1), ElementBehavior.SOLUTION),
    28 to ElementLook(Color(0xFF4CAF50), ElementBehavior.SOLUTION),
    29 to ElementLook(Color(0xFF1E88E5), ElementBehavior.SOLUTION),
    30 to ElementLook(Color(0xFFA8C8DE), ElementBehavior.METALLIC),
    31 to ElementLook(Color(0xFFE0D2C0), ElementBehavior.METALLIC),
    32 to ElementLook(Color(0xFF9EA4A8), ElementBehavior.PRECIPITATE),
    33 to ElementLook(Color(0xFF7C7C80), ElementBehavior.PRECIPITATE),
    34 to ElementLook(Color(0xFFC62828), ElementBehavior.PRECIPITATE),
    35 to ElementLook(Color(0xFFA0341B), ElementBehavior.COLORED_VAPOR),
    36 to ElementLook(Color(0xFFE8E6FF), ElementBehavior.GLOW),
    37 to ElementLook(Color(0xFFB0306A), ElementBehavior.REACTIVE),
    38 to ElementLook(Color(0xFFFF1A1A), ElementBehavior.FLAME),
    39 to ElementLook(Color(0xFF39FF14), ElementBehavior.GLOW),
    40 to ElementLook(Color(0xFFF2F4FF), ElementBehavior.PRECIPITATE),
    41 to ElementLook(Color(0xFF6F7BD6), ElementBehavior.METALLIC),
    42 to ElementLook(Color(0xFF8A9A8C), ElementBehavior.METALLIC),
    43 to ElementLook(Color(0xFF7FE0C4), ElementBehavior.RADIOACTIVE),
    44 to ElementLook(Color(0xFFB0233C), ElementBehavior.SOLUTION),
    45 to ElementLook(Color(0xFFD2566E), ElementBehavior.SOLUTION),
    46 to ElementLook(Color(0xFFCBC2B8), ElementBehavior.METALLIC),
    47 to ElementLook(Color(0xFFF0F0F0), ElementBehavior.METALLIC),
    48 to ElementLook(Color(0xFFFFA000), ElementBehavior.PRECIPITATE),
    49 to ElementLook(Color(0xFF4B3FD6), ElementBehavior.FLAME),
    50 to ElementLook(Color(0xFFD9D9D9), ElementBehavior.METALLIC),
    51 to ElementLook(Color(0xFFC0B8A8), ElementBehavior.PRECIPITATE),
    52 to ElementLook(Color(0xFF8D6E63), ElementBehavior.PRECIPITATE),
    53 to ElementLook(Color(0xFF6A1B9A), ElementBehavior.COLORED_VAPOR),
    54 to ElementLook(Color(0xFF5C9DFF), ElementBehavior.GLOW),
    55 to ElementLook(Color(0xFF9C8CFF), ElementBehavior.REACTIVE),
    56 to ElementLook(Color(0xFFC6E040), ElementBehavior.FLAME),
    57 to ElementLook(Color(0xFFFFF4E0), ElementBehavior.GLOW),
    58 to ElementLook(Color(0xFFFFE066), ElementBehavior.GLOW),
    59 to ElementLook(Color(0xFF7CB342), ElementBehavior.SOLUTION),
    60 to ElementLook(Color(0xFFB39DDB), ElementBehavior.SOLUTION),
    61 to ElementLook(Color(0xFF8FFFD0), ElementBehavior.RADIOACTIVE),
    62 to ElementLook(Color(0xFFFF8A50), ElementBehavior.GLOW),
    63 to ElementLook(Color(0xFFFF1744), ElementBehavior.GLOW),
    64 to ElementLook(Color(0xFFA8B3A0), ElementBehavior.METALLIC),
    65 to ElementLook(Color(0xFFB6FF3B), ElementBehavior.GLOW),
    66 to ElementLook(Color(0xFFD0C7A8), ElementBehavior.METALLIC),
    67 to ElementLook(Color(0xFFF6C1A8), ElementBehavior.SOLUTION),
    68 to ElementLook(Color(0xFFFCC6DA), ElementBehavior.SOLUTION),
    69 to ElementLook(Color(0xFF5E7BFF), ElementBehavior.GLOW),
    70 to ElementLook(Color(0xFFE6EE9C), ElementBehavior.SOLUTION),
    71 to ElementLook(Color(0xFFD6C8FF), ElementBehavior.RADIOACTIVE),
    72 to ElementLook(Color(0xFF8E9AA6), ElementBehavior.METALLIC),
    73 to ElementLook(Color(0xFF5F6F8A), ElementBehavior.METALLIC),
    74 to ElementLook(Color(0xFFFFA94D), ElementBehavior.GLOW),
    75 to ElementLook(Color(0xFF8F8478), ElementBehavior.METALLIC),
    76 to ElementLook(Color(0xFF7A8FB0), ElementBehavior.METALLIC),
    77 to ElementLook(Color(0xFFE9E3B8), ElementBehavior.METALLIC),
    78 to ElementLook(Color(0xFFDCE0F2), ElementBehavior.METALLIC),
    79 to ElementLook(Color(0xFFFFC94A), ElementBehavior.METALLIC),
    80 to ElementLook(Color(0xFF9FB0C2), ElementBehavior.METALLIC),
    81 to ElementLook(Color(0xFF1FBF6A), ElementBehavior.FLAME),
    82 to ElementLook(Color(0xFF6E7880), ElementBehavior.METALLIC),
    83 to ElementLook(Color(0xFFC58CE0), ElementBehavior.PRECIPITATE),
    84 to ElementLook(Color(0xFF4FC3F7), ElementBehavior.RADIOACTIVE),
    85 to ElementLook(Color(0xFF3E1A5E), ElementBehavior.COLORED_VAPOR),
    86 to ElementLook(Color(0xFFFF6E6E), ElementBehavior.RADIOACTIVE),
    87 to ElementLook(Color(0xFFFF7F50), ElementBehavior.REACTIVE),
    88 to ElementLook(Color(0xFF7CFFB2), ElementBehavior.RADIOACTIVE),
    89 to ElementLook(Color(0xFF7FD8FF), ElementBehavior.RADIOACTIVE),
    90 to ElementLook(Color(0xFFE0E0C8), ElementBehavior.RADIOACTIVE),
    91 to ElementLook(Color(0xFFB2C47A), ElementBehavior.RADIOACTIVE),
    92 to ElementLook(Color(0xFFC6FF00), ElementBehavior.RADIOACTIVE),
    93 to ElementLook(Color(0xFF4DD0B0), ElementBehavior.RADIOACTIVE),
    94 to ElementLook(Color(0xFFFF6F3C), ElementBehavior.RADIOACTIVE),
    95 to ElementLook(Color(0xFFFF8FB8), ElementBehavior.RADIOACTIVE),
    96 to ElementLook(Color(0xFFB070FF), ElementBehavior.RADIOACTIVE),
    97 to ElementLook(Color(0xFFD4E157), ElementBehavior.RADIOACTIVE),
    98 to ElementLook(Color(0xFF5EEB8F), ElementBehavior.RADIOACTIVE),
    99 to ElementLook(Color(0xFF3D8BFF), ElementBehavior.RADIOACTIVE),
    100 to ElementLook(Color(0xFFFFB74D), ElementBehavior.RADIOACTIVE),
    101 to ElementLook(Color(0xFFE57373), ElementBehavior.RADIOACTIVE),
    102 to ElementLook(Color(0xFFBA68C8), ElementBehavior.RADIOACTIVE),
    103 to ElementLook(Color(0xFF4DB6AC), ElementBehavior.RADIOACTIVE),
    104 to ElementLook(Color(0xFFAED581), ElementBehavior.RADIOACTIVE),
    105 to ElementLook(Color(0xFFFF8A65), ElementBehavior.RADIOACTIVE),
    106 to ElementLook(Color(0xFF9575CD), ElementBehavior.RADIOACTIVE),
    107 to ElementLook(Color(0xFFFFFF8D), ElementBehavior.RADIOACTIVE),
    108 to ElementLook(Color(0xFFFFD54F), ElementBehavior.RADIOACTIVE),
    109 to ElementLook(Color(0xFFF06292), ElementBehavior.RADIOACTIVE),
    110 to ElementLook(Color(0xFF81C784), ElementBehavior.RADIOACTIVE),
    111 to ElementLook(Color(0xFFE6B422), ElementBehavior.RADIOACTIVE),
    112 to ElementLook(Color(0xFFB8C2CC), ElementBehavior.RADIOACTIVE),
    113 to ElementLook(Color(0xFF7986CB), ElementBehavior.RADIOACTIVE),
    114 to ElementLook(Color(0xFFA1887F), ElementBehavior.RADIOACTIVE),
    115 to ElementLook(Color(0xFFD500F9), ElementBehavior.RADIOACTIVE),
    116 to ElementLook(Color(0xFF26C6DA), ElementBehavior.RADIOACTIVE),
    117 to ElementLook(Color(0xFF8E24AA), ElementBehavior.RADIOACTIVE),
    118 to ElementLook(Color(0xFFEDEDED), ElementBehavior.RADIOACTIVE),
)

/** Aspecto en el recipiente. La regla por familia es solo un respaldo: la tabla cubre los 118. */
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
