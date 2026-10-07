package com.jjas.labpomodoro.domain.usecase

import android.content.res.Resources
import com.jjas.labpomodoro.R
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

    /** Textos del archivo en el idioma del usuario (encabezado, tipos y sí/no). */
    data class Labels(
        val header: String,
        val work: String,
        val shortBreak: String,
        val longBreak: String,
        val yes: String,
        val no: String,
    ) {
        companion object {
            fun from(resources: Resources) = Labels(
                header = resources.getString(R.string.prog_csv_header),
                work = resources.getString(R.string.prog_csv_work),
                shortBreak = resources.getString(R.string.prog_csv_short_break),
                longBreak = resources.getString(R.string.prog_csv_long_break),
                yes = resources.getString(R.string.prog_csv_yes),
                no = resources.getString(R.string.prog_csv_no),
            )
        }
    }

    fun build(sessions: List<SessionEntity>, zone: ZoneId, resources: Resources): String =
        build(sessions, zone, Labels.from(resources))

    fun build(sessions: List<SessionEntity>, zone: ZoneId, labels: Labels): String = buildString {
        append(BOM)
        appendLine(labels.header)
        sessions.sortedBy { it.startedAtMillis }.forEach { s ->
            val start = Instant.ofEpochMilli(s.startedAtMillis).atZone(zone)
            val end = Instant.ofEpochMilli(s.endedAtMillis).atZone(zone)
            appendLine(
                listOf(
                    DATE.format(start),
                    TIME.format(start),
                    TIME.format(end),
                    s.type.csvName(labels),
                    minutes(s.plannedSeconds),
                    minutes(s.actualSeconds),
                    if (s.completed) labels.yes else labels.no,
                ).joinToString(",")
            )
        }
    }

    // Con un decimal y punto, para que las hojas de cálculo lo lean como número
    private fun minutes(seconds: Int): String = "%.1f".format(Locale.ROOT, seconds / 60.0)

    private fun SessionType.csvName(labels: Labels) = when (this) {
        SessionType.WORK -> labels.work
        SessionType.SHORT_BREAK -> labels.shortBreak
        SessionType.LONG_BREAK -> labels.longBreak
    }
}
