package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.HourTotal
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Query("SELECT * FROM sessions ORDER BY startedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SessionEntity>>

    /** Días (epochDay) con al menos un pomodoro de trabajo completado; base de la racha. */
    @Query("SELECT DISTINCT epochDay FROM sessions WHERE type = 'WORK' AND completed = 1 ORDER BY epochDay")
    fun observeActiveDays(): Flow<List<Long>>

    /** Tiempo de trabajo completado por hora del día, para detectar las horas pico. */
    @Query(
        """
        SELECT hourOfDay, SUM(actualSeconds) AS totalSeconds, COUNT(*) AS sessions
        FROM sessions
        WHERE type = 'WORK' AND completed = 1
        GROUP BY hourOfDay
        ORDER BY hourOfDay
        """
    )
    fun observeProductiveHours(): Flow<List<HourTotal>>

    @Query("SELECT COALESCE(SUM(actualSeconds), 0) FROM sessions WHERE type = 'WORK' AND completed = 1")
    fun observeTotalWorkSeconds(): Flow<Long>

    @Query("SELECT * FROM sessions ORDER BY startedAtMillis")
    suspend fun all(): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startedAtMillis")
    fun observeAll(): Flow<List<SessionEntity>>
}
