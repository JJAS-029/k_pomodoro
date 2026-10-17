package com.jjas.labpomodoro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class YearHeatmapTest {

    private val today = LocalDate.of(2026, 10, 15) // jueves

    @Test
    fun `semanas de lunes a domingo que terminan hoy`() {
        val weeks = YearHeatmap.weeks(emptyMap(), today)
        assertEquals(53, weeks.size)
        assertEquals(DayOfWeek.MONDAY, weeks.first().first()!!.date.dayOfWeek)
        assertEquals(today, weeks.last()[3]!!.date)
        // Viernes a domingo de esta semana aún no llegan
        assertNull(weeks.last()[4])
        assertNull(weeks.last()[6])
    }

    @Test
    fun `los minutos caen en su dia y su nivel`() {
        val weeks = YearHeatmap.weeks(mapOf(today.toEpochDay() to 130, today.minusDays(1).toEpochDay() to 25), today)
        assertEquals(4, weeks.last()[3]!!.level)
        assertEquals(2, weeks.last()[2]!!.level)
        assertEquals(0, weeks.last()[0]!!.level)
    }
}
