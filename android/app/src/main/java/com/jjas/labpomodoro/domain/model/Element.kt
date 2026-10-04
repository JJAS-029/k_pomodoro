package com.jjas.labpomodoro.domain.model

enum class ElementCategory {
    ALKALI_METAL,
    ALKALINE_EARTH_METAL,
    TRANSITION_METAL,
    POST_TRANSITION_METAL,
    METALLOID,
    NONMETAL,
    HALOGEN,
    NOBLE_GAS,
    LANTHANIDE,
    ACTINIDE,
    UNKNOWN,
}

/**
 * Elemento químico del catálogo (datos fijos). Lo que el usuario ha conseguido vive en Room
 * (inventario); aquí solo está la información para dibujar la tabla periódica.
 *
 * @param group columna 1–18, o null para lantánidos y actínidos (van en las filas separadas del bloque f).
 */
data class Element(
    val atomicNumber: Int,
    val symbol: String,
    val name: String,
    val group: Int?,
    val category: ElementCategory,
) {
    val period: Int
        get() = when (atomicNumber) {
            in 1..2 -> 1
            in 3..10 -> 2
            in 11..18 -> 3
            in 19..36 -> 4
            in 37..54 -> 5
            in 55..86 -> 6
            else -> 7
        }
}
