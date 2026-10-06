package com.jjas.labpomodoro.data.repository

import androidx.room.withTransaction
import com.jjas.labpomodoro.data.local.LabDatabase
import com.jjas.labpomodoro.data.local.dao.DiscoveryDao
import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.Rarity
import com.jjas.labpomodoro.domain.usecase.ElementPicker
import com.jjas.labpomodoro.domain.usecase.Fusion
import com.jjas.labpomodoro.domain.usecase.RewardSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Un hallazgo para mostrar en la app. */
data class Discovery(val id: Long, val element: Element, val source: DiscoverySource)

/** Recompensas y sintetizador: todo lo que cambia el inventario pasa por aquí, en transacción. */
@Singleton
class LabRepository @Inject constructor(
    private val db: LabDatabase,
    private val inventoryDao: InventoryDao,
    private val discoveryDao: DiscoveryDao,
    private val clock: Clock,
) {
    private val picker = ElementPicker()
    private val mutex = Mutex()

    /** Hallazgos que el usuario aún no ha visto, para el aviso en la pantalla principal. */
    val unseen: Flow<List<Discovery>> = discoveryDao.observeUnseen().map { rows ->
        rows.map { Discovery(it.id, PeriodicTable[it.atomicNumber], it.source) }
    }

    suspend fun markAllSeen() = discoveryDao.markAllSeen()

    /**
     * Entrega las recompensas que correspondan al tiempo de enfoque acumulado. Es idempotente:
     * cuenta lo ya entregado en la tabla de hallazgos, así que se puede llamar cuantas veces sea.
     */
    suspend fun syncRewards(totalWorkSeconds: Long) = mutex.withLock {
        db.withTransaction {
            val pending = RewardSchedule.pending(
                totalWorkSeconds,
                grantedBasic = discoveryDao.countBySource(DiscoverySource.BASIC_REWARD),
                grantedRare = discoveryDao.countBySource(DiscoverySource.RARE_REWARD),
            )
            if (pending.isEmpty) return@withTransaction
            val counts = currentCounts().toMutableMap()
            repeat(pending.basic) { grant(Rarity.BASIC, DiscoverySource.BASIC_REWARD, counts) }
            repeat(pending.rare) { grant(Rarity.RARE, DiscoverySource.RARE_REWARD, counts) }
        }
    }

    private suspend fun grant(rarity: Rarity, source: DiscoverySource, counts: MutableMap<Int, Int>) {
        val element = picker.pick(rarity, counts)
        give(element.atomicNumber, source)
        counts[element.atomicNumber] = (counts[element.atomicNumber] ?: 0) + 1
    }

    /** Veces obtenido; los descubiertos sin registro (de antes de la tabla de hallazgos) cuentan como 1. */
    private suspend fun currentCounts(): Map<Int, Int> {
        val counts = discoveryDao.countsByElement().associate { it.atomicNumber to it.total }.toMutableMap()
        inventoryDao.discoveredAtomicNumbers().forEach { z -> counts[z] = maxOf(counts[z] ?: 0, 1) }
        return counts
    }

    /** Veces que se ha obtenido cada elemento, para la maestría (bronce, plata y oro). */
    val obtainedCounts: Flow<Map<Int, Int>> = combine(
        discoveryDao.observeCountsByElement(),
        inventoryDao.observeAll(),
    ) { rows, inventory ->
        val counts = rows.associate { it.atomicNumber to it.total }.toMutableMap()
        inventory.filter { it.firstObtainedAtMillis != null }.forEach { counts[it.atomicNumber] = maxOf(counts[it.atomicNumber] ?: 0, 1) }
        counts
    }

    /**
     * Fusiona [recipe] en el sintetizador: gasta los dos ingredientes y entrega el resultado.
     * Devuelve false si ya no alcanzaban (por ejemplo, con dos toques seguidos).
     */
    suspend fun fuse(recipe: Fusion.Recipe): Boolean = mutex.withLock {
        val a = recipe.a.atomicNumber
        val b = recipe.b.atomicNumber
        try {
            db.withTransaction {
                val consumed = if (a == b) {
                    inventoryDao.consume(a, 2) == 1
                } else {
                    inventoryDao.consume(a, 1) == 1 && inventoryDao.consume(b, 1) == 1
                }
                // Si faltó alguno, la excepción deshace lo que ya se había gastado
                if (!consumed) throw NotEnoughMaterial()
                give(recipe.result.atomicNumber, DiscoverySource.FUSION)
            }
            true
        } catch (_: NotEnoughMaterial) {
            false
        }
    }

    /** Solo pruebas (botón en debug): obtiene [times] veces cada elemento, ya vistos. */
    suspend fun grantEachForTesting(times: Int) = mutex.withLock {
        db.withTransaction {
            val now = clock.millis()
            for (z in 1..PeriodicTable.SIZE) repeat(times) {
                inventoryDao.add(z, 1, now)
                discoveryDao.insert(DiscoveryEntity(atomicNumber = z, source = DiscoverySource.FUSION, obtainedAtMillis = now, seen = true))
            }
        }
    }

    private suspend fun give(atomicNumber: Int, source: DiscoverySource) {
        val now = clock.millis()
        inventoryDao.add(atomicNumber, 1, now)
        discoveryDao.insert(DiscoveryEntity(atomicNumber = atomicNumber, source = source, obtainedAtMillis = now))
    }

    private class NotEnoughMaterial : IllegalStateException("No alcanzan los ingredientes")
}
