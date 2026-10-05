package com.jjas.labpomodoro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.domain.model.DiscoverySource
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {

    @Insert
    suspend fun insert(discovery: DiscoveryEntity): Long

    @Query("SELECT COUNT(*) FROM discoveries WHERE source = :source")
    suspend fun countBySource(source: DiscoverySource): Int

    @Query("SELECT * FROM discoveries WHERE seen = 0 ORDER BY id")
    fun observeUnseen(): Flow<List<DiscoveryEntity>>

    @Query("UPDATE discoveries SET seen = 1 WHERE seen = 0")
    suspend fun markAllSeen()
}
