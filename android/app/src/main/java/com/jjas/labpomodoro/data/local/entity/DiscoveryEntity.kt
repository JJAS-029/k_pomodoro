package com.jjas.labpomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jjas.labpomodoro.domain.model.DiscoverySource

/**
 * Cada elemento conseguido, uno por fila: sirve para contar las recompensas ya entregadas y para
 * avisar de los hallazgos nuevos ([seen] = false hasta que el usuario los ve).
 */
@Entity(tableName = "discoveries", indices = [Index("source"), Index("seen")])
data class DiscoveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val atomicNumber: Int,
    val source: DiscoverySource,
    val obtainedAtMillis: Long,
    val seen: Boolean = false,
)
