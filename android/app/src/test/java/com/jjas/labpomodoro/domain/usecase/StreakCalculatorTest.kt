package com.jjas.labpomodoro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakCalculatorTest {

    private val today = LocalDate.of(2026, 10, 4)
    private fun daysAgo(n: Long) = today.minusDays(n)

    @Test
    fun `sin actividad no hay racha`() {
        assertEquals(Streak(0, 0), StreakCalculator.calculate(emptyList(), today))
    }

    @Test
    fun `solo hoy cuenta como racha de 1`() {
        assertEquals(Streak(1, 1), StreakCalculator.calculate(listOf(today), today))
    }

    @Test
    fun `la racha sigue viva si el ultimo dia fue ayer`() {
        val days = listOf(daysAgo(3), daysAgo(2), daysAgo(1))
        assertEquals(Streak(3, 3), StreakCalculator.calculate(days, today))
    }

    @Test
    fun `la racha se rompe si el ultimo dia fue antier`() {
        val days = listOf(daysAgo(4), daysAgo(3), daysAgo(2))
        assertEquals(Streak(0, 3), StreakCalculator.calculate(days, today))
    }

    @Test
    fun `un hueco reinicia la racha actual pero conserva la mas larga`() {
        val days = listOf(daysAgo(10), daysAgo(9), daysAgo(8), daysAgo(7), daysAgo(1), today)
        assertEquals(Streak(2, 4), StreakCalculator.calculate(days, today))
    }

    @Test
    fun `ignora duplicados y el orden de entrada`() {
        val days = listOf(today, daysAgo(1), today, daysAgo(2), daysAgo(1))
        assertEquals(Streak(3, 3), StreakCalculator.calculate(days, today))
    }
}
