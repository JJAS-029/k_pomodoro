package com.jjas.labpomodoro.domain.model

/** Colores de las etiquetas (ARGB), legibles en el tema claro y en el oscuro. */
object TagColors {
    val all: List<Long> = listOf(
        0xFF42A5F5, // azul
        0xFF66BB6A, // verde
        0xFFFFA726, // naranja
        0xFFEC407A, // rosa
        0xFFAB47BC, // morado
        0xFF26C6DA, // turquesa
        0xFFEF5350, // rojo
        0xFFD4E157, // lima
    )

    fun of(index: Int): Long = all[index.mod(all.size)]

    /** Etiquetas gratis; con Pro, ilimitadas. */
    const val FREE_LIMIT = 3

    const val MAX_NAME = 24
}
