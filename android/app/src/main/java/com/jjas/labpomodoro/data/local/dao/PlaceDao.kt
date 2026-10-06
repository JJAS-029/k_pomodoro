package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.PlaceEntity
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {

    @Insert
    suspend fun insert(place: PlaceEntity): Long

    @Query("SELECT * FROM places")
    suspend fun all(): List<PlaceEntity>

    @Query("UPDATE places SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    /** Asigna el lugar a la sesión que empezó en [startedAtMillis] (se llama justo después de guardarla). */
    @Query("UPDATE sessions SET placeId = :placeId WHERE startedAtMillis = :startedAtMillis")
    suspend fun tagSession(startedAtMillis: Long, placeId: Long)

    /** Enfoque por lugar desde [fromEpochDay]; los lugares sin sesiones no aparecen. */
    @Query(
        """
        SELECT p.id AS placeId, p.name AS name,
               COALESCE(SUM(CASE WHEN s.completed = 1 THEN s.actualSeconds ELSE 0 END), 0) AS workSeconds,
               SUM(CASE WHEN s.completed = 1 THEN 1 ELSE 0 END) AS completed,
               SUM(CASE WHEN s.completed = 0 THEN 1 ELSE 0 END) AS skipped
        FROM places p JOIN sessions s ON s.placeId = p.id
        WHERE s.type = 'WORK' AND s.epochDay >= :fromEpochDay
        GROUP BY p.id
        ORDER BY workSeconds DESC
        """
    )
    fun observeTotals(fromEpochDay: Long): Flow<List<PlaceTotal>>
}
