package com.jjas.labpomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un lugar donde el usuario se concentra (Pro, opcional). Las ubicaciones a menos de
 * [com.jjas.labpomodoro.data.repository.PlaceRepository.SAME_PLACE_METERS] se juntan en el mismo
 * lugar. Solo se guarda en el teléfono.
 */
@Entity(tableName = "places")
data class PlaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val createdAtMillis: Long,
)

/** Fila de la consulta de enfoque por lugar. */
data class PlaceTotal(
    val placeId: Long,
    val name: String,
    val workSeconds: Long,
    val completed: Int,
    val skipped: Int,
)
