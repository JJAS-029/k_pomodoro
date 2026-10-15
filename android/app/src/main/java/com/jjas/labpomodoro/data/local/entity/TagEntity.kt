package com.jjas.labpomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Etiqueta de un plan ("Matemáticas", "Tesis"): en qué se trabajó. Cada sesión del plan la lleva,
 * para ver el enfoque por etiqueta en Progreso y en el CSV. [color] es un índice de
 * [com.jjas.labpomodoro.domain.model.TagColors].
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Int,
    val createdAtMillis: Long,
)

/** Fila de la consulta de enfoque por etiqueta. */
data class TagTotal(
    val tagId: Long,
    val name: String,
    val color: Int,
    val workSeconds: Long,
    val completed: Int,
)
