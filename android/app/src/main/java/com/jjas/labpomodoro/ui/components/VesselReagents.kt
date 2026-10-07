package com.jjas.labpomodoro.ui.components

import com.jjas.labpomodoro.data.repository.InventoryItem
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.PlannedSession
import com.jjas.labpomodoro.domain.model.SessionType
import kotlin.random.Random

/**
 * Qué elemento lleva cada recipiente de trabajo. Es una función pura del plan, así que la app, el
 * widget y la notificación coinciden, aunque la app se cierre y se vuelva a abrir a mitad del plan.
 */
object VesselReagents {

    /** Los elementos que ya se tenían al empezar el plan (los nuevos se usan en el siguiente). */
    fun pool(items: List<InventoryItem>, planStartMillis: Long): List<Element> =
        items.filter { it.firstObtainedAtMillis != null && it.firstObtainedAtMillis <= planStartMillis }.map { it.element }

    /**
     * El favorito si se eligió uno (y se tenía al empezar); si no, uno al azar de [pool], estable
     * para esa sesión del plan. null en los descansos o si aún no hay elementos.
     */
    fun reagent(plan: List<PlannedSession>, index: Int, planSeed: Long, pool: List<Element>, favorite: Int): Element? {
        if (plan.getOrNull(index)?.type != SessionType.WORK || pool.isEmpty()) return null
        pool.firstOrNull { it.atomicNumber == favorite }?.let { return it }
        return pool[Random(planSeed * 17 + index).nextInt(pool.size)]
    }
}
