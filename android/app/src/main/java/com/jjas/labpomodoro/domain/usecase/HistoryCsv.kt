package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Historial de sesiones en CSV, listo para abrir en Excel, Google Sheets o Numbers. */
object HistoryCsv {

    /** Marca de orden de bytes: sin ella Excel muestra mal los acentos. */
    const val BOM = "\uFEFF"

    private val DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun build(sessions: List<SessionEntity>, zone: ZoneId): String = buildString {
        append(BOM)
        appendLine("fecha,inicio,fin,tipo,minutos_planeados,minutos_reales,completada")
        sessions.sortedBy { it.startedAtMillis }.forEach { s ->
            val start = Instant.ofEpochMilli(s.startedAtMillis).atZone(zone)
            val end = Instant.ofEpochMilli(s.endedAtMillis).atZone(zone)
            appendLine(
                listOf(
                    DATE.format(start),
                    TIME.format(start),
                    TIME.format(end),
                    s.type.csvName(),
                    minutes(s.plannedSeconds),
                    minutes(s.actualSeconds),
                    if (s.completed) "sí" else "no",
                ).joinToString(",")
            )
        }
    }

    // Con un decimal y punto, para que las hojas de cálculo lo lean como número
    private fun minutes(seconds: Int): String = "%.1f".format(Locale.ROOT, seconds / 60.0)

    private fun SessionType.csvName() = when (this) {
        SessionType.WORK -> "trabajo"
        SessionType.SHORT_BREAK -> "descanso corto"
        SessionType.LONG_BREAK -> "descanso largo"
    }
}
