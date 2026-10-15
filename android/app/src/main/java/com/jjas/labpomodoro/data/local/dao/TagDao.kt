package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.jjas.labpomodoro.data.local.entity.TagEntity
import com.jjas.labpomodoro.data.local.entity.TagTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM tags ORDER BY createdAtMillis")
    fun observeAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags")
    suspend fun all(): List<TagEntity>

    @Query("UPDATE tags SET name = :name, color = :color WHERE id = :id")
    suspend fun update(id: Long, name: String, color: Int)

    /** Borra la etiqueta; sus sesiones se quedan, sin etiqueta. */
    @Transaction
    suspend fun delete(id: Long) {
        untagSessions(id)
        deleteRow(id)
    }

    @Query("UPDATE sessions SET tagId = NULL WHERE tagId = :id")
    suspend fun untagSessions(id: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteRow(id: Long)

    /** Asigna la etiqueta a la sesión que empezó en [startedAtMillis] (se llama justo después de guardarla). */
    @Query("UPDATE sessions SET tagId = :tagId WHERE startedAtMillis = :startedAtMillis")
    suspend fun tagSession(startedAtMillis: Long, tagId: Long)

    /** Enfoque por etiqueta desde [fromEpochDay]; solo pomodoros completados, las etiquetas sin ellos no aparecen. */
    @Query(
        """
        SELECT t.id AS tagId, t.name AS name, t.color AS color,
               SUM(s.actualSeconds) AS workSeconds,
               COUNT(*) AS completed
        FROM tags t JOIN sessions s ON s.tagId = t.id
        WHERE s.type = 'WORK' AND s.completed = 1 AND s.epochDay >= :fromEpochDay
        GROUP BY t.id
        ORDER BY workSeconds DESC
        """
    )
    fun observeTotals(fromEpochDay: Long): Flow<List<TagTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<TagEntity>)

    @Query("DELETE FROM tags")
    suspend fun deleteAll()
}
