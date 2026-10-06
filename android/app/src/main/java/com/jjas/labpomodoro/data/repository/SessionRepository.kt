package com.jjas.labpomodoro.data.repository

import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.local.entity.HourTotal
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.Streak
import com.jjas.labpomodoro.domain.usecase.StreakCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val clock: Clock,
) {

    /**
     * Guarda una sesión terminada (el TimerEngine lo llama al completar o saltar).
     * [actualSeconds] es el tiempo corrido sin contar pausas, por eso no se deduce de inicio y fin.
     */
    suspend fun record(
        type: SessionType,
        startedAt: Instant,
        endedAt: Instant,
        plannedSeconds: Int,
        actualSeconds: Int,
        completed: Boolean,
    ): Long {
        val local = startedAt.atZone(clock.zone)
        return sessionDao.insert(
            SessionEntity(
                type = type,
                startedAtMillis = startedAt.toEpochMilli(),
                endedAtMillis = endedAt.toEpochMilli(),
                plannedSeconds = plannedSeconds,
                actualSeconds = actualSeconds.coerceIn(0, plannedSeconds),
                completed = completed,
                epochDay = local.toLocalDate().toEpochDay(),
                hourOfDay = local.hour,
            )
        )
    }

    fun observeRecent(limit: Int = 50): Flow<List<SessionEntity>> = sessionDao.observeRecent(limit)

    /** "Hoy" se evalúa cada vez que cambian los datos. */
    val streak: Flow<Streak> = sessionDao.observeActiveDays().map { days ->
        StreakCalculator.calculate(days.map(LocalDate::ofEpochDay), LocalDate.now(clock))
    }

    val productiveHours: Flow<List<HourTotal>> = sessionDao.observeProductiveHours()

    val totalWorkSeconds: Flow<Long> = sessionDao.observeTotalWorkSeconds()

    /** Todo el historial, para las estadísticas. */
    val allSessions: Flow<List<SessionEntity>> = sessionDao.observeAll()

    /** Todo el historial, para exportarlo. */
    suspend fun all(): List<SessionEntity> = sessionDao.all()
}
