package com.jjas.labpomodoro.domain.model

/**
 * Qué tan difícil es conseguir un elemento.
 * - [BASIC]: los 30 primeros, los más abundantes. Se ganan cada 25 min de enfoque.
 * - [RARE]: del 31 al 92 (lo que existe en la naturaleza). Se ganan cada 60 min de enfoque.
 * - [SYNTHETIC]: los que solo se fabrican en laboratorio (Tc, Pm y del 93 en adelante).
 *   No salen como recompensa: solo se consiguen en el sintetizador.
 */
enum class Rarity(val label: String) {
    BASIC("Básico"),
    RARE("Raro"),
    SYNTHETIC("Sintético"),
}

private val SYNTHETIC_IN_NATURE_RANGE = setOf(43, 61)

val Element.rarity: Rarity
    get() = when {
        atomicNumber in SYNTHETIC_IN_NATURE_RANGE || atomicNumber > 92 -> Rarity.SYNTHETIC
        atomicNumber <= 30 -> Rarity.BASIC
        else -> Rarity.RARE
    }

/** Los elementos de cada rareza, en orden de número atómico. */
fun PeriodicTable.ofRarity(rarity: Rarity): List<Element> = elements.filter { it.rarity == rarity }

/** De dónde salió un elemento del inventario. */
enum class DiscoverySource {
    /** Recompensa por cada 25 min de enfoque acumulados. */
    BASIC_REWARD,

    /** Recompensa por cada 60 min de enfoque acumulados. */
    RARE_REWARD,

    /** Fabricado en el sintetizador. */
    FUSION,
}

fun ElementCategory.label(): String = when (this) {
    ElementCategory.ALKALI_METAL -> "Metal alcalino"
    ElementCategory.ALKALINE_EARTH_METAL -> "Metal alcalinotérreo"
    ElementCategory.TRANSITION_METAL -> "Metal de transición"
    ElementCategory.POST_TRANSITION_METAL -> "Otro metal"
    ElementCategory.METALLOID -> "Metaloide"
    ElementCategory.NONMETAL -> "No metal"
    ElementCategory.HALOGEN -> "Halógeno"
    ElementCategory.NOBLE_GAS -> "Gas noble"
    ElementCategory.LANTHANIDE -> "Lantánido"
    ElementCategory.ACTINIDE -> "Actínido"
    ElementCategory.UNKNOWN -> "Propiedades desconocidas"
}
