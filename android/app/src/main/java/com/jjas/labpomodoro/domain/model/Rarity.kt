package com.jjas.labpomodoro.domain.model

import androidx.annotation.StringRes
import com.jjas.labpomodoro.R

/**
 * Qué tan difícil es conseguir un elemento.
 * - [BASIC]: los 30 primeros, los más abundantes. Se ganan cada 25 min de enfoque.
 * - [RARE]: del 31 al 92 (lo que existe en la naturaleza). Se ganan cada 60 min de enfoque.
 * - [SYNTHETIC]: los que solo se fabrican en laboratorio (Tc, Pm y del 93 en adelante).
 *   No salen como recompensa: solo se consiguen en el sintetizador.
 */
enum class Rarity(@StringRes val labelRes: Int) {
    BASIC(R.string.el_rarity_basic),
    RARE(R.string.el_rarity_rare),
    SYNTHETIC(R.string.el_rarity_synthetic),
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

@StringRes
fun ElementCategory.labelRes(): Int = when (this) {
    ElementCategory.ALKALI_METAL -> R.string.el_category_alkali_metal
    ElementCategory.ALKALINE_EARTH_METAL -> R.string.el_category_alkaline_earth_metal
    ElementCategory.TRANSITION_METAL -> R.string.el_category_transition_metal
    ElementCategory.POST_TRANSITION_METAL -> R.string.el_category_post_transition_metal
    ElementCategory.METALLOID -> R.string.el_category_metalloid
    ElementCategory.NONMETAL -> R.string.el_category_nonmetal
    ElementCategory.HALOGEN -> R.string.el_category_halogen
    ElementCategory.NOBLE_GAS -> R.string.el_category_noble_gas
    ElementCategory.LANTHANIDE -> R.string.el_category_lanthanide
    ElementCategory.ACTINIDE -> R.string.el_category_actinide
    ElementCategory.UNKNOWN -> R.string.el_category_unknown
}
