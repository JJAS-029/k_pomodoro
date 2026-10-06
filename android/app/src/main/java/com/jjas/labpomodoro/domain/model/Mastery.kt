package com.jjas.labpomodoro.domain.model

/**
 * Maestría de un elemento según cuántas veces se ha obtenido en total (gastarlo en el sintetizador
 * no la baja). Le da sentido a los repetidos una vez descubierta la tabla.
 */
enum class Mastery(val label: String) {
    NONE("Sin descubrir"),
    DISCOVERED("Descubierto"),
    BRONZE("Bronce"),
    SILVER("Plata"),
    GOLD("Oro"),
}

object MasteryRules {

    /** Veces que hay que obtener el elemento para bronce, plata y oro. */
    fun thresholds(element: Element): IntArray =
        // Los sintéticos solo salen del sintetizador: metas más cortas para que no sean eternos
        if (element.rarity == Rarity.SYNTHETIC) intArrayOf(2, 4, 6) else intArrayOf(5, 10, 15)

    fun level(element: Element, obtained: Int): Mastery {
        if (obtained <= 0) return Mastery.NONE
        val (bronze, silver, gold) = thresholds(element)
        return when {
            obtained >= gold -> Mastery.GOLD
            obtained >= silver -> Mastery.SILVER
            obtained >= bronze -> Mastery.BRONZE
            else -> Mastery.DISCOVERED
        }
    }

    /** El siguiente nivel y cuántas veces faltan para llegar; null si ya es oro. */
    fun next(element: Element, obtained: Int): Pair<Mastery, Int>? {
        val (bronze, silver, gold) = thresholds(element)
        return when {
            obtained < bronze -> Mastery.BRONZE to bronze - obtained
            obtained < silver -> Mastery.SILVER to silver - obtained
            obtained < gold -> Mastery.GOLD to gold - obtained
            else -> null
        }
    }

    /**
     * El nivel que alcanza la tabla completa: el más bajo de los 118 (bronce solo cuando todos son
     * al menos bronce). [counts]: número atómico → veces obtenido.
     */
    fun tableLevel(counts: Map<Int, Int>): Mastery =
        PeriodicTable.elements.minOf { level(it, counts[it.atomicNumber] ?: 0) }
}
