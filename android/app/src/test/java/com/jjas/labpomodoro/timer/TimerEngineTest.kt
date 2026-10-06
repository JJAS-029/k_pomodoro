package com.jjas.labpomodoro.timer

import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.local.entity.HourTotal
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class TimerEngineTest {

    private class FakeSessionDao : SessionDao {
        val inserted = mutableListOf<SessionEntity>()
        override suspend fun insert(session: SessionEntity): Long {
            inserted += session
            return inserted.size.toLong()
        }
        override fun observeRecent(limit: Int): Flow<List<SessionEntity>> = emptyFlow()
        override fun observeActiveDays(): Flow<List<Long>> = emptyFlow()
        override fun observeProductiveHours(): Flow<List<HourTotal>> = emptyFlow()
        override fun observeTotalWorkSeconds(): Flow<Long> = emptyFlow()
        override suspend fun all(): List<SessionEntity> = inserted.toList()
    }

    private class FakeScheduler : DeadlineScheduler {
        var scheduledAt: Long? = null
        override fun schedule(atElapsedRealtime: Long) { scheduledAt = atElapsedRealtime }
        override fun cancel() { scheduledAt = null }
    }

    private val minute = 60_000L
    // 1 h con pomodoros de 25 min: 25 · 5 · 25 · 5 · 10 (recortado)
    private val config = SessionConfig(totalHours = 1, workMinutes = 25, shortBreakMinutes = 5)

    private class Fixture(scope: TestScope) {
        val dao = FakeSessionDao()
        val scheduler = FakeScheduler()
        var serviceStarts = 0
        private val start = Instant.parse("2026-10-04T15:00:00Z")
        val time = object : TimeSource {
            override fun elapsedRealtime() = scope.testScheduler.currentTime
            override fun now(): Instant = start.plusMillis(scope.testScheduler.currentTime)
        }
        val engine = TimerEngine(
            time = time,
            sessions = SessionRepository(dao, Clock.fixed(start, ZoneOffset.UTC)),
            scheduler = scheduler,
            serviceLauncher = { serviceStarts++ },
            scope = scope.backgroundScope,
        )
        val active get() = engine.state.value as TimerState.Active
    }

    @Test
    fun `al iniciar arranca el primer pomodoro, el servicio y la alarma`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)

        assertEquals(0, f.active.index)
        assertEquals(SessionType.WORK, f.active.current.type)
        assertEquals(25 * minute, f.active.remainingMillis(f.time.elapsedRealtime()))
        assertEquals(1, f.serviceStarts)
        assertEquals(25 * minute, f.scheduler.scheduledAt)
    }

    @Test
    fun `al acabar el tiempo guarda la sesion completada y pasa al descanso`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)

        advanceTimeBy(25 * minute + 1)
        runCurrent()

        assertEquals(1, f.active.index)
        assertEquals(SessionType.SHORT_BREAK, f.active.current.type)
        val saved = f.dao.inserted.single()
        assertTrue(saved.completed)
        assertEquals(SessionType.WORK, saved.type)
        assertEquals(25 * 60, saved.actualSeconds)
        assertEquals(1, f.active.completedWorkSessions)
    }

    @Test
    fun `la pausa congela el tiempo y continuar lo retoma`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)
        advanceTimeBy(10 * minute)

        f.engine.pause()
        assertEquals(null, f.scheduler.scheduledAt)
        advanceTimeBy(60 * minute)
        runCurrent()
        assertEquals(0, f.active.index) // La sesión no terminó durante la pausa
        assertEquals(15 * minute, f.active.remainingMillis(f.time.elapsedRealtime()))

        f.engine.resume()
        assertEquals(f.time.elapsedRealtime() + 15 * minute, f.scheduler.scheduledAt)
        advanceTimeBy(15 * minute + 1)
        runCurrent()
        assertEquals(1, f.active.index)
    }

    @Test
    fun `saltar guarda la sesion como no completada con el tiempo real corrido`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)
        advanceTimeBy(10 * minute)

        f.engine.skip()

        val saved = f.dao.inserted.single()
        assertFalse(saved.completed)
        assertEquals(10 * 60, saved.actualSeconds)
        assertEquals(0, f.active.completedWorkSessions)
        assertEquals(1, f.active.index)
    }

    @Test
    fun `una alarma adelantada no termina la sesion`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)
        advanceTimeBy(5 * minute)

        f.engine.onDeadline()

        assertEquals(0, f.active.index)
        assertTrue(f.dao.inserted.isEmpty())
    }

    @Test
    fun `al terminar el plan queda el resumen y se cancela la alarma`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)

        advanceTimeBy(70 * minute + 1)
        runCurrent()

        val finished = f.engine.state.value as TimerState.Finished
        assertEquals(3, finished.completedWorkSessions)
        assertEquals(60 * 60L, finished.completedWorkSeconds)
        assertEquals(5, f.dao.inserted.size)
        assertEquals(null, f.scheduler.scheduledAt)

        f.engine.dismissFinished()
        assertEquals(TimerState.Idle, f.engine.state.value)
    }

    @Test
    fun `detener vuelve a idle sin guardar nada`() = runTest {
        val f = Fixture(this)
        f.engine.start(config)
        advanceTimeBy(3 * minute)

        f.engine.reset()

        assertEquals(TimerState.Idle, f.engine.state.value)
        assertTrue(f.dao.inserted.isEmpty())
        assertEquals(null, f.scheduler.scheduledAt)
    }
}
