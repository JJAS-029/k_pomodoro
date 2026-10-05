package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.Rarity
import com.jjas.labpomodoro.domain.model.ofRarity
import kotlin.random.Random

/**
 * Recompensas por tiempo de enfoque **acumulado** (solo pomodoros completados): cada 25 min
 * un elemento básico y cada 60 min uno raro. Se calcula sobre el total, así que funciona igual
 * con pomodoros cortos o largos y nunca se dan de más aunque se recalcule varias veces.
 */
object RewardSchedule {
    const val BASIC_EVERY_SECONDS = 25 * 60L
    const val RARE_EVERY_SECONDS = 60 * 60L

    data class Pending(val basic: Int, val rare: Int) {
        val isEmpty: Boolean get() = basic <= 0 && rare <= 0
    }

    /** Cuántas recompensas faltan por entregar dado lo que ya se entregó. */
    fun pending(totalWorkSeconds: Long, grantedBasic: Int, grantedRare: Int): Pending = Pending(
        basic = (totalWorkSeconds / BASIC_EVERY_SECONDS - grantedBasic).toInt().coerceAtLeast(0),
        rare = (totalWorkSeconds / RARE_EVERY_SECONDS - grantedRare).toInt().coerceAtLeast(0),
    )

    /** Avance (0..1) hacia la siguiente recompensa de cada tipo. */
    fun progress(totalWorkSeconds: Long, everySeconds: Long): Float =
        (totalWorkSeconds % everySeconds).toFloat() / everySeconds
}

/** Elige qué elemento sale en una recompensa. */
class ElementPicker(private val random: Random = Random.Default) {

    /**
     * Favorece los que aún no se tienen (75 %) para que la tabla avance, pero a veces repite:
     * los repetidos sirven como material para el sintetizador.
     */
    fun pick(rarity: Rarity, owned: Set<Int>): Element {
        val pool = PeriodicTable.ofRarity(rarity)
        val missing = pool.filter { it.atomicNumber !in owned }
        return if (missing.isNotEmpty() && random.nextFloat() < NEW_ELEMENT_CHANCE) {
            missing.random(random)
        } else {
            pool.random(random)
        }
    }

    private companion object {
        const val NEW_ELEMENT_CHANCE = 0.75f
    }
}

/**
 * Sintetizador: fusión nuclear simplificada. Dos núcleos se unen y sus protones se suman, así que
 * A + B produce el elemento con número atómico A + B (por ejemplo H + U → Np). Gasta una unidad
 * de cada uno (dos si es el mismo elemento).
 */
object Fusion {

    data class Recipe(val a: Element, val b: Element) {
        val result: Element get() = PeriodicTable[a.atomicNumber + b.atomicNumber]
    }

    /**
     * Combinaciones posibles para fabricar [target] con lo que hay en el inventario
     * ([quantities]: número atómico → unidades). Primero las que gastan lo más abundante.
     */
    fun recipesFor(target: Int, quantities: Map<Int, Int>): List<Recipe> {
        if (target !in 2..PeriodicTable.SIZE) return emptyList()
        return (1..target / 2)
            .mapNotNull { a ->
                val b = target - a
                val qa = quantities[a] ?: 0
                val qb = quantities[b] ?: 0
                val enough = if (a == b) qa >= 2 else qa >= 1 && qb >= 1
                if (enough) Recipe(PeriodicTable[a], PeriodicTable[b]) else null
            }
            .sortedByDescending { minOf(quantities[it.a.atomicNumber] ?: 0, quantities[it.b.atomicNumber] ?: 0) }
    }
}
