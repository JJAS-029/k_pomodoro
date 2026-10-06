package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StatsCalculatorTest {

    // Lunes 5 de octubre de 2026
    private val today = LocalDate.of(2026, 10, 5)

    private fun s(day: LocalDate, hour: Int, minutes: Int, completed: Boolean = true, type: SessionType = SessionType.WORK) =
        SessionEntity(
            type = type,
            startedAtMillis = 0,
            endedAtMillis = 0,
            plannedSeconds = minutes * 60,
            actualSeconds = minutes * 60,
            completed = completed,
            epochDay = day.toEpochDay(),
            hourOfDay = hour,
        )

    private val history = listOf(
        s(today, 9, 25),
        s(today, 10, 25),
        s(today, 11, 25, completed = false),
        s(today, 11, 5, type = SessionType.SHORT_BREAK),
        s(today.minusDays(3), 21, 50), // viernes
        s(today.minusDays(40), 9, 25), // fuera de 30 días
    )

    @Test
    fun `resumen de la semana`() {
        val stats = StatsCalculator.calculate(history, today, StatsRange.WEEK)
        assertEquals((25 + 25 + 50) * 60L, stats.workSeconds)
        assertEquals(3, stats.completed)
        assertEquals(1, stats.skipped)
        assertEquals(75, stats.completionRate)
        assertEquals(2, stats.activeDays)
        assertEquals(50 * 60L, stats.averagePerActiveDaySeconds)
    }

    @Test
    fun `linea de tiempo diaria con dias vacios`() {
        val timeline = StatsCalculator.calculate(history, today, StatsRange.WEEK).timeline
        assertEquals(7, timeline.size)
        assertEquals(today.minusDays(6), timeline.first().start)
        assertEquals(50 * 60L, timeline.first { it.start == today }.seconds)
        assertEquals(0L, timeline.first { it.start == today.minusDays(1) }.seconds)
    }

    @Test
    fun `mejor dia y hora`() {
        val stats = StatsCalculator.calculate(history, today, StatsRange.MONTH)
        assertEquals(DayOfWeek.MONDAY, stats.bestWeekday) // 50 min el lunes = 50 el viernes; gana el primero
        assertEquals(21, stats.bestHour) // 50 min a las 21 vs 25 a las 9 y 25 a las 10
    }

    @Test
    fun `todo agrupa por mes`() {
        val stats = StatsCalculator.calculate(history, today, StatsRange.ALL)
        assertEquals(4 * 25 * 60L + 25 * 60L, stats.workSeconds)
        // Desde agosto (hace 40 días) hasta octubre
        assertEquals(listOf(8, 9, 10), stats.timeline.map { it.start.monthValue })
    }

    @Test
    fun `sin historial`() {
        val stats = StatsCalculator.calculate(emptyList(), today, StatsRange.WEEK)
        assertTrue(stats.isEmpty)
        assertNull(stats.completionRate)
        assertNull(stats.bestHour)
    }
}
