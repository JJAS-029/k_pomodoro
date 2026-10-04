package com.jjas.labpomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cuántas unidades tiene el usuario de cada elemento. Se precargan las 118 filas en 0. */
@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val atomicNumber: Int,
    val quantity: Int = 0,
    val firstObtainedAtMillis: Long? = null,
)
