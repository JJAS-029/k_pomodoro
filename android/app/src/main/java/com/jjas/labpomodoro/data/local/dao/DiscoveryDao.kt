package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.data.local.entity.ElementCount
import com.jjas.labpomodoro.domain.model.DiscoverySource
import kotlinx.coroutines.flow.Flow
import androidx.room.OnConflictStrategy

@Dao
interface DiscoveryDao {

    @Insert
    suspend fun insert(discovery: DiscoveryEntity): Long

    @Query("SELECT COUNT(*) FROM discoveries WHERE source = :source")
    suspend fun countBySource(source: DiscoverySource): Int

    @Query("SELECT COUNT(*) FROM discoveries WHERE source = :source")
    fun observeCountBySource(source: DiscoverySource): Flow<Int>

    @Query("SELECT * FROM discoveries WHERE seen = 0 ORDER BY id")
    fun observeUnseen(): Flow<List<DiscoveryEntity>>

    @Query("UPDATE discoveries SET seen = 1 WHERE seen = 0")
    suspend fun markAllSeen()

    /** Veces que se ha obtenido cada elemento, contando recompensas y fusiones. */
    @Query("SELECT atomicNumber, COUNT(*) AS total FROM discoveries GROUP BY atomicNumber")
    suspend fun countsByElement(): List<ElementCount>

    @Query("SELECT atomicNumber, COUNT(*) AS total FROM discoveries GROUP BY atomicNumber")
    fun observeCountsByElement(): Flow<List<ElementCount>>

    @Query("SELECT * FROM discoveries ORDER BY id")
    suspend fun all(): List<DiscoveryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<DiscoveryEntity>)

    @Query("DELETE FROM discoveries")
    suspend fun deleteAll()
}
