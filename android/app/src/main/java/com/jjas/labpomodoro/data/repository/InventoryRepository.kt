package com.jjas.labpomodoro.data.repository

import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.PeriodicTable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Un elemento del catálogo junto con lo que el usuario tiene de él. */
data class InventoryItem(val element: Element, val quantity: Int, val firstObtainedAtMillis: Long?)

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val clock: Clock,
) {

    val items: Flow<List<InventoryItem>> = inventoryDao.observeAll().map { rows ->
        rows.map { InventoryItem(PeriodicTable[it.atomicNumber], it.quantity, it.firstObtainedAtMillis) }
    }

    val discoveredCount: Flow<Int> = inventoryDao.observeDiscoveredCount()

    /** Las recompensas de la Fase 4 llaman a esto. */
    suspend fun add(atomicNumber: Int, amount: Int = 1) {
        require(atomicNumber in 1..PeriodicTable.SIZE) { "Número atómico inválido: $atomicNumber" }
        require(amount > 0) { "La cantidad debe ser positiva" }
        inventoryDao.add(atomicNumber, amount, clock.millis())
    }
}
