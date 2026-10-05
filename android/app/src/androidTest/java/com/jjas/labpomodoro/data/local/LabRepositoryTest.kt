package com.jjas.labpomodoro.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.Rarity
import com.jjas.labpomodoro.domain.model.rarity
import com.jjas.labpomodoro.domain.usecase.Fusion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class LabRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-10-04T18:00:00Z"), ZoneOffset.UTC)
    private lateinit var db: LabDatabase
    private lateinit var lab: LabRepository
    private lateinit var inventory: InventoryRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LabDatabase::class.java)
            .addCallback(LabDatabase.SeedInventory)
            .build()
        lab = LabRepository(db, db.inventoryDao(), db.discoveryDao(), clock)
        inventory = InventoryRepository(db.inventoryDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun total() = inventory.items.first().sumOf { it.quantity }

    @Test
    fun una_hora_da_dos_basicos_y_un_raro_y_no_se_repite() = runTest {
        lab.syncRewards(60 * 60L)
        lab.syncRewards(60 * 60L)
        val unseen = lab.unseen.first()
        assertEquals(3, unseen.size)
        assertEquals(2, unseen.count { it.element.rarity == Rarity.BASIC })
        assertEquals(1, unseen.count { it.element.rarity == Rarity.RARE })
        assertEquals(3, total())

        lab.markAllSeen()
        assertTrue(lab.unseen.first().isEmpty())
    }

    @Test
    fun la_fusion_gasta_los_ingredientes_y_entrega_el_resultado() = runTest {
        db.inventoryDao().add(1, 1, 0)
        db.inventoryDao().add(92, 1, 0)
        assertTrue(lab.fuse(Fusion.Recipe(PeriodicTable[1], PeriodicTable[92])))

        val items = inventory.items.first()
        assertEquals(0, items[0].quantity)
        assertEquals(0, items[91].quantity)
        assertEquals(1, items[92].quantity)
        // Lo gastado sigue contando como descubierto
        assertEquals(3, inventory.discoveredCount.first())
    }

    @Test
    fun si_falta_un_ingrediente_no_se_gasta_nada() = runTest {
        db.inventoryDao().add(1, 1, 0)
        assertFalse(lab.fuse(Fusion.Recipe(PeriodicTable[1], PeriodicTable[92])))
        assertEquals(1, inventory.items.first()[0].quantity)
        assertEquals(1, total())
    }
}
