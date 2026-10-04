package com.jjas.labpomodoro.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jjas.labpomodoro.data.local.entity.HourTotal
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.Streak
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class LabDatabaseTest {

    private val zone: ZoneId = ZoneOffset.ofHours(-6)
    private val now = LocalDateTime.of(2026, 10, 4, 12, 0).atZone(zone).toInstant()
    private val clock = Clock.fixed(now, zone)

    private lateinit var db: LabDatabase
    private lateinit var sessions: SessionRepository
    private lateinit var inventory: InventoryRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), LabDatabase::class.java)
            .addCallback(LabDatabase.SeedInventory)
            .build()
        sessions = SessionRepository(db.sessionDao(), clock)
        inventory = InventoryRepository(db.inventoryDao(), clock)
    }

    @After
    fun tearDown() = db.close()

    /** Guarda un pomodoro que empezó a las [hour] horas de hace [daysAgo] días. */
    private suspend fun work(daysAgo: Long, hour: Int, minutes: Long = 25, completed: Boolean = true) {
        val start = LocalDateTime.of(2026, 10, 4, hour, 0).minusDays(daysAgo).atZone(zone).toInstant()
        sessions.record(SessionType.WORK, start, start.plusSeconds(minutes * 60), (minutes * 60).toInt(), completed)
    }

    @Test
    fun inventarioPrecargadoConLos118EnCero() = runTest {
        val items = inventory.items.first()
        assertEquals(PeriodicTable.SIZE, items.size)
        assertEquals(0, items.sumOf { it.quantity })
        assertEquals(0, inventory.discoveredCount.first())
    }

    @Test
    fun agregarElementoSumaYGuardaElPrimerHallazgo() = runTest {
        inventory.add(26)
        inventory.add(26, amount = 2)

        val iron = inventory.items.first().single { it.element.symbol == "Fe" }
        assertEquals(3, iron.quantity)
        assertEquals(clock.millis(), iron.firstObtainedAtMillis)
        assertEquals(1, inventory.discoveredCount.first())
    }

    @Test
    fun rachaYHorasProductivasSoloCuentanTrabajoCompletado() = runTest {
        work(daysAgo = 2, hour = 9)
        work(daysAgo = 1, hour = 9)
        work(daysAgo = 0, hour = 9)
        work(daysAgo = 0, hour = 21, minutes = 50)
        // Saltada: no cuenta para la racha ni para las horas
        work(daysAgo = 3, hour = 9, completed = false)
        // Un descanso tampoco cuenta
        val breakStart = now.minusSeconds(3600)
        sessions.record(SessionType.SHORT_BREAK, breakStart, breakStart.plusSeconds(300), 300, completed = true)

        assertEquals(Streak(current = 3, longest = 3), sessions.streak.first())
        assertEquals(
            listOf(HourTotal(9, 3 * 25 * 60L, 3), HourTotal(21, 50 * 60L, 1)),
            sessions.productiveHours.first(),
        )
        assertEquals((3 * 25 + 50) * 60L, sessions.totalWorkSeconds.first())
        assertEquals(6, sessions.observeRecent().first().size)
    }

    @Test
    fun guardaElDiaYLaHoraLocales() = runTest {
        // 23:30 hora local = 05:30 UTC del día siguiente: debe contar para el día local
        val start = LocalDateTime.of(2026, 10, 3, 23, 30).atZone(zone).toInstant()
        sessions.record(SessionType.WORK, start, start.plusSeconds(1500), 1500, completed = true)

        val saved = sessions.observeRecent().first().single()
        assertEquals(LocalDateTime.of(2026, 10, 3, 0, 0).toLocalDate().toEpochDay(), saved.epochDay)
        assertEquals(23, saved.hourOfDay)
        assertNotNull(saved.id)
    }
}
