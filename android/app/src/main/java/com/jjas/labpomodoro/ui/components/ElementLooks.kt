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

    /** Gases nobles, fósforos, filamentos y láseres: luz propia. */
    GLOW("Brilla con luz propia, como un letrero de neón, un fósforo o un filamento encendido."),

    /** Metales: el líquido se vuelve opaco y con reflejos de espejo. */
    METALLIC("Se ve como metal fundido, con reflejos que recorren la superficie."),

    /** Halógenos: sueltan vapor de color. */
    COLORED_VAPOR("Se evapora en un vapor de su color, como los halógenos."),

    /** Sólidos que no se disuelven: cristales que se asientan en el fondo. */
    PRECIPITATE("No se disuelve: forma cristales o gemas que caen y se asientan en el fondo."),

    /** Gases licuados: hierven y desbordan una niebla fría. */
    CRYO("Está licuado a temperaturas bajísimas: hierve y desborda niebla fría."),

    /** Radiactivos: brillo que late y destellos de partículas. */
    RADIOACTIVE("Es radiactivo: late con un brillo propio y suelta destellos."),
}

/** @param origin de dónde sale su color (se muestra en la ficha). */
data class ElementLook(val color: Color, val behavior: ElementBehavior, val origin: String = "")

// Los 118 tienen color y comportamiento propios, tomados de algo real: su aspecto, el color de sus
// iones en agua, el de su llama o su luminiscencia. Para los superpesados (100+) no se conoce: se
// reparten tonos distintos para que ninguno se repita
private val LOOKS: Map<Int, ElementLook> = mapOf(
    1 to ElementLook(Color(0xFFD9A3E8), ElementBehavior.GLOW, "Descarga de hidrógeno: rosa violáceo"),
    2 to ElementLook(Color(0xFFFFB3C1), ElementBehavior.GLOW, "Descarga de helio: rosa durazno"),
    3 to ElementLook(Color(0xFFD81B60), ElementBehavior.REACTIVE, "Llama carmín"),
    4 to ElementLook(Color(0xFF2BBF8A), ElementBehavior.PRECIPITATE, "Esmeralda: el berilo es su gema"),
    5 to ElementLook(Color(0xFF7EE04A), ElementBehavior.FLAME, "Llama verde lima"),
    6 to ElementLook(Color(0xFF3A3A3A), ElementBehavior.PRECIPITATE, "Grafito"),
    7 to ElementLook(Color(0xFFDCEFF7), ElementBehavior.CRYO, "Nitrógeno líquido, casi incoloro"),
    8 to ElementLook(Color(0xFF7FC8F8), ElementBehavior.CRYO, "Oxígeno líquido, azul pálido"),
    9 to ElementLook(Color(0xFFF2F29A), ElementBehavior.COLORED_VAPOR, "Gas amarillo pálido"),
    10 to ElementLook(Color(0xFFFF4A1C), ElementBehavior.GLOW, "Letrero de neón"),
    11 to ElementLook(Color(0xFFFFD000), ElementBehavior.REACTIVE, "Llama amarilla del sodio"),
    12 to ElementLook(Color(0xFFF7F7F7), ElementBehavior.FLAME, "Arde con luz blanca cegadora"),
    13 to ElementLook(Color(0xFFA9B0B5), ElementBehavior.METALLIC, "Aluminio mate"),
    14 to ElementLook(Color(0xFF6F7F96), ElementBehavior.PRECIPITATE, "Cristal gris azulado"),
    15 to ElementLook(Color(0xFF9CFF57), ElementBehavior.GLOW, "El fósforo blanco brilla verde en el aire"),
    16 to ElementLook(Color(0xFFEDE64A), ElementBehavior.PRECIPITATE, "Cristales amarillo limón"),
    17 to ElementLook(Color(0xFFBEDB55), ElementBehavior.COLORED_VAPOR, "Gas amarillo verdoso"),
    18 to ElementLook(Color(0xFFA57CFF), ElementBehavior.GLOW, "Descarga violeta"),
    19 to ElementLook(Color(0xFFC58BE8), ElementBehavior.REACTIVE, "Llama lila"),
    20 to ElementLook(Color(0xFFFF6A3D), ElementBehavior.FLAME, "Llama rojo ladrillo"),
    21 to ElementLook(Color(0xFFEFF7C8), ElementBehavior.GLOW, "Lámparas de estadio"),
    22 to ElementLook(Color(0xFF9D8BB0), ElementBehavior.METALLIC, "Titanio anodizado violeta"),
    23 to ElementLook(Color(0xFF7E57C2), ElementBehavior.SOLUTION, "Ion V2+ violeta"),
    24 to ElementLook(Color(0xFFFF7A1A), ElementBehavior.SOLUTION, "Dicromato naranja"),
    25 to ElementLook(Color(0xFF9C1E8F), ElementBehavior.SOLUTION, "Permanganato morado"),
    26 to ElementLook(Color(0xFFC1611F), ElementBehavior.SOLUTION, "Óxido, café rojizo"),
    27 to ElementLook(Color(0xFFF48FB1), ElementBehavior.SOLUTION, "Ion Co2+ rosa"),
    28 to ElementLook(Color(0xFF4CAF50), ElementBehavior.SOLUTION, "Ion Ni2+ verde"),
    29 to ElementLook(Color(0xFF1E88E5), ElementBehavior.SOLUTION, "Sulfato de cobre azul"),
    30 to ElementLook(Color(0xFFA8C8DE), ElementBehavior.METALLIC, "Galvanizado blanco azulado"),
    31 to ElementLook(Color(0xFFE0D2C0), ElementBehavior.METALLIC, "Se derrite en la mano: plata cálida"),
    32 to ElementLook(Color(0xFF9EA4A8), ElementBehavior.PRECIPITATE, "Cristal gris brillante"),
    33 to ElementLook(Color(0xFF7C7C80), ElementBehavior.PRECIPITATE, "Arsénico gris"),
    34 to ElementLook(Color(0xFFC62828), ElementBehavior.PRECIPITATE, "Selenio rojo"),
    35 to ElementLook(Color(0xFFA0341B), ElementBehavior.COLORED_VAPOR, "Vapor rojo pardo"),
    36 to ElementLook(Color(0xFFE8E6FF), ElementBehavior.GLOW, "Descarga blanca lavanda"),
    37 to ElementLook(Color(0xFFB0306A), ElementBehavior.REACTIVE, "Llama rojo violeta"),
    38 to ElementLook(Color(0xFFFF1A1A), ElementBehavior.FLAME, "Llama escarlata (fuegos artificiales)"),
    39 to ElementLook(Color(0xFF39FF14), ElementBehavior.GLOW, "Láser verde Nd:YAG"),
    40 to ElementLook(Color(0xFFF2F4FF), ElementBehavior.PRECIPITATE, "Cristales de zirconia"),
    41 to ElementLook(Color(0xFF6F7BD6), ElementBehavior.METALLIC, "Anodizado azul violeta"),
    42 to ElementLook(Color(0xFF8A9A8C), ElementBehavior.METALLIC, "Plata oscura verdosa"),
    43 to ElementLook(Color(0xFF7FE0C4), ElementBehavior.RADIOACTIVE, "Menta (trazador médico)"),
    44 to ElementLook(Color(0xFFB0233C), ElementBehavior.SOLUTION, "Rojo de rutenio"),
    45 to ElementLook(Color(0xFFD2566E), ElementBehavior.SOLUTION, "Sales de rodio rosadas"),
    46 to ElementLook(Color(0xFFCBC2B8), ElementBehavior.METALLIC, "Gris cálido"),
    47 to ElementLook(Color(0xFFF0F0F0), ElementBehavior.METALLIC, "Plata brillante"),
    48 to ElementLook(Color(0xFFFFA000), ElementBehavior.PRECIPITATE, "Pigmento amarillo cadmio"),
    49 to ElementLook(Color(0xFF4B3FD6), ElementBehavior.FLAME, "Línea índigo que le da nombre"),
    50 to ElementLook(Color(0xFFD9D9D9), ElementBehavior.METALLIC, "Estaño claro"),
    51 to ElementLook(Color(0xFFC0B8A8), ElementBehavior.PRECIPITATE, "Antimonio gris cálido"),
    52 to ElementLook(Color(0xFF8D6E63), ElementBehavior.PRECIPITATE, "Telurio pardo"),
    53 to ElementLook(Color(0xFF6A1B9A), ElementBehavior.COLORED_VAPOR, "Vapor violeta"),
    54 to ElementLook(Color(0xFF5C9DFF), ElementBehavior.GLOW, "Faros de xenón azules"),
    55 to ElementLook(Color(0xFF9C8CFF), ElementBehavior.REACTIVE, "Llama azul violeta"),
    56 to ElementLook(Color(0xFFC6E040), ElementBehavior.FLAME, "Llama verde amarillenta"),
    57 to ElementLook(Color(0xFFFFF4E0), ElementBehavior.GLOW, "Lámparas de arco de cine"),
    58 to ElementLook(Color(0xFFFFE066), ElementBehavior.GLOW, "Fósforo amarillo de los LED blancos"),
    59 to ElementLook(Color(0xFF7CB342), ElementBehavior.SOLUTION, "Ion Pr3+ verde"),
    60 to ElementLook(Color(0xFFB39DDB), ElementBehavior.SOLUTION, "Ion Nd3+ lila"),
    61 to ElementLook(Color(0xFF8FFFD0), ElementBehavior.RADIOACTIVE, "Brillo verde azulado"),
    62 to ElementLook(Color(0xFFFF8A50), ElementBehavior.GLOW, "Luminiscencia naranja"),
    63 to ElementLook(Color(0xFFFF1744), ElementBehavior.GLOW, "Fósforo rojo de pantallas"),
    64 to ElementLook(Color(0xFFA8B3A0), ElementBehavior.METALLIC, "Gris verdoso magnético"),
    65 to ElementLook(Color(0xFFB6FF3B), ElementBehavior.GLOW, "Fósforo verde amarillo"),
    66 to ElementLook(Color(0xFFD0C7A8), ElementBehavior.METALLIC, "Plata bronceada"),
    67 to ElementLook(Color(0xFFF6C1A8), ElementBehavior.SOLUTION, "Ion Ho3+ durazno"),
    68 to ElementLook(Color(0xFFFCC6DA), ElementBehavior.SOLUTION, "Ion Er3+ rosa pálido"),
    69 to ElementLook(Color(0xFF5E7BFF), ElementBehavior.GLOW, "Emisión azul del tulio"),
    70 to ElementLook(Color(0xFFE6EE9C), ElementBehavior.SOLUTION, "Ion Yb2+ verde amarillo"),
    71 to ElementLook(Color(0xFFD6C8FF), ElementBehavior.RADIOACTIVE, "Lutecio-177 para radioterapia"),
    72 to ElementLook(Color(0xFF8E9AA6), ElementBehavior.METALLIC, "Acero de barras de control"),
    73 to ElementLook(Color(0xFF5F6F8A), ElementBehavior.METALLIC, "Gris azul oscuro"),
    74 to ElementLook(Color(0xFFFFA94D), ElementBehavior.GLOW, "Filamento incandescente"),
    75 to ElementLook(Color(0xFF8F8478), ElementBehavior.METALLIC, "Gris pardo"),
    76 to ElementLook(Color(0xFF7A8FB0), ElementBehavior.METALLIC, "Gris azulado, el más denso"),
    77 to ElementLook(Color(0xFFE9E3B8), ElementBehavior.METALLIC, "Plata amarillenta, sales de mil colores"),
    78 to ElementLook(Color(0xFFDCE0F2), ElementBehavior.METALLIC, "Platino blanco frío"),
    79 to ElementLook(Color(0xFFFFC94A), ElementBehavior.METALLIC, "Oro"),
    80 to ElementLook(Color(0xFF9FB0C2), ElementBehavior.METALLIC, "Espejo líquido"),
    81 to ElementLook(Color(0xFF1FBF6A), ElementBehavior.FLAME, "Línea verde esmeralda"),
    82 to ElementLook(Color(0xFF6E7880), ElementBehavior.METALLIC, "Gris plomo opaco"),
    83 to ElementLook(Color(0xFFC58CE0), ElementBehavior.PRECIPITATE, "Cristales tornasol de bismuto"),
    84 to ElementLook(Color(0xFF4FC3F7), ElementBehavior.RADIOACTIVE, "Resplandor azul"),
    85 to ElementLook(Color(0xFF3E1A5E), ElementBehavior.COLORED_VAPOR, "Vapor violeta oscuro"),
    86 to ElementLook(Color(0xFFFF6E6E), ElementBehavior.RADIOACTIVE, "Gas radiactivo con brillo rojizo"),
    87 to ElementLook(Color(0xFFFF7F50), ElementBehavior.REACTIVE, "Coral"),
    88 to ElementLook(Color(0xFF7CFFB2), ElementBehavior.RADIOACTIVE, "Pintura luminosa verde de relojes antiguos"),
    89 to ElementLook(Color(0xFF7FD8FF), ElementBehavior.RADIOACTIVE, "Brilla azul pálido en la oscuridad"),
    90 to ElementLook(Color(0xFFE0E0C8), ElementBehavior.RADIOACTIVE, "Plata (camisas de lámparas de gas)"),
    91 to ElementLook(Color(0xFFB2C47A), ElementBehavior.RADIOACTIVE, "Oliva"),
    92 to ElementLook(Color(0xFFC6FF00), ElementBehavior.RADIOACTIVE, "Vidrio de uranio verde amarillo"),
    93 to ElementLook(Color(0xFF4DD0B0), ElementBehavior.RADIOACTIVE, "Ion Np5+ verde azulado"),
    94 to ElementLook(Color(0xFFFF6F3C), ElementBehavior.RADIOACTIVE, "Se calienta solo: rojo vivo"),
    95 to ElementLook(Color(0xFFFF8FB8), ElementBehavior.RADIOACTIVE, "Ion Am3+ rosa"),
    96 to ElementLook(Color(0xFFB070FF), ElementBehavior.RADIOACTIVE, "Brilla morado en la oscuridad"),
    97 to ElementLook(Color(0xFFD4E157), ElementBehavior.RADIOACTIVE, "Ion Bk3+ verde amarillo"),
    98 to ElementLook(Color(0xFF5EEB8F), ElementBehavior.RADIOACTIVE, "Ion Cf3+ verde"),
    99 to ElementLook(Color(0xFF3D8BFF), ElementBehavior.RADIOACTIVE, "Brilla azul por su propia radiación"),
    100 to ElementLook(Color(0xFFFFB74D), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    101 to ElementLook(Color(0xFFE57373), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    102 to ElementLook(Color(0xFFBA68C8), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    103 to ElementLook(Color(0xFF4DB6AC), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    104 to ElementLook(Color(0xFFAED581), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    105 to ElementLook(Color(0xFFFF8A65), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    106 to ElementLook(Color(0xFF9575CD), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    107 to ElementLook(Color(0xFFFFFF8D), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    108 to ElementLook(Color(0xFFFFD54F), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    109 to ElementLook(Color(0xFFF06292), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    110 to ElementLook(Color(0xFF81C784), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    111 to ElementLook(Color(0xFFE6B422), ElementBehavior.RADIOACTIVE, "Imaginario: dorado como el oro, que está en su grupo"),
    112 to ElementLook(Color(0xFFB8C2CC), ElementBehavior.RADIOACTIVE, "Imaginario: plateado como el mercurio, que está en su grupo"),
    113 to ElementLook(Color(0xFF7986CB), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    114 to ElementLook(Color(0xFFA1887F), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    115 to ElementLook(Color(0xFFD500F9), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    116 to ElementLook(Color(0xFF26C6DA), ElementBehavior.RADIOACTIVE, "Nadie lo ha visto en cantidad visible: su color es imaginario"),
    117 to ElementLook(Color(0xFF8E24AA), ElementBehavior.RADIOACTIVE, "Imaginario: violeta como los halógenos de su grupo"),
    118 to ElementLook(Color(0xFFEDEDED), ElementBehavior.RADIOACTIVE, "Imaginario: claro como los gases nobles de su grupo"),
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
