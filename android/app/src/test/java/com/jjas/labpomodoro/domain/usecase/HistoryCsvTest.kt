package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class HistoryCsvTest {

    private val zone = ZoneOffset.ofHours(-6)

    // Los mismos textos que values-es/strings_progress.xml (las pruebas de JVM no leen recursos)
    private val labels = HistoryCsv.Labels(
        header = "fecha,inicio,fin,tipo,minutos_planeados,minutos_reales,completada",
        work = "trabajo",
        shortBreak = "descanso corto",
        longBreak = "descanso largo",
        yes = "sí",
        no = "no",
    )

    private fun session(hour: Int, minutes: Int, type: SessionType, completed: Boolean, actual: Int = minutes * 60): SessionEntity {
        val start = LocalDateTime.of(2026, 10, 5, hour, 0).atZone(zone).toInstant().toEpochMilli()
        return SessionEntity(
            type = type,
            startedAtMillis = start,
            endedAtMillis = start + actual * 1000L,
            plannedSeconds = minutes * 60,
            actualSeconds = actual,
            completed = completed,
            epochDay = 0,
            hourOfDay = hour,
        )
    }

    @Test
    fun `encabezado y filas en hora local y en orden`() {
        val csv = HistoryCsv.build(
            listOf(
                session(10, 5, SessionType.SHORT_BREAK, completed = true),
                session(9, 25, SessionType.WORK, completed = false, actual = 754),
            ),
            zone,
            labels,
        )
        assertTrue(csv.startsWith(HistoryCsv.BOM))
        val lines = csv.removePrefix(HistoryCsv.BOM).lines().filter { it.isNotEmpty() }
        assertEquals(
            listOf(
                "fecha,inicio,fin,tipo,minutos_planeados,minutos_reales,completada",
                "2026-10-05,09:00:00,09:12:34,trabajo,25.0,12.6,no",
                "2026-10-05,10:00:00,10:05:00,descanso corto,5.0,5.0,sí",
            ),
            lines,
        )
    }

    @Test
    fun `sin historial queda solo el encabezado`() {
        val lines = HistoryCsv.build(emptyList(), zone, labels).removePrefix(HistoryCsv.BOM).lines().filter { it.isNotEmpty() }
        assertEquals(1, lines.size)
    }
}
