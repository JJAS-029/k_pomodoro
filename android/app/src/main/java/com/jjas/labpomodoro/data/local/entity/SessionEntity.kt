package com.jjas.labpomodoro.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.jjas.labpomodoro.domain.model.SessionType

/**
 * Una sesión terminada (trabajo o descanso). [epochDay] y [hourOfDay] se calculan en la zona horaria
 * del teléfono al guardar, para que la racha y las horas pico no cambien si el usuario viaja después.
 */
@Entity(
    tableName = "sessions",
    indices = [Index("epochDay"), Index("type", "completed")],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: SessionType,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val plannedSeconds: Int,
    val actualSeconds: Int,
    /** false si se saltó: saltar no es completar y no cuenta para rachas ni recompensas. */
    val completed: Boolean,
    val epochDay: Long,
    val hourOfDay: Int,
)

/** Fila de la consulta de horas productivas. */
data class HourTotal(
    val hourOfDay: Int,
    val totalSeconds: Long,
    val sessions: Int,
)
