package com.jjas.labpomodoro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class StreakReminderTest {

    private val today = LocalDate.of(2026, 10, 6)

    @Test
    fun `si hoy ya trabajo no recuerda nada`() {
        assertNull(StreakReminder.message(listOf(today.minusDays(1), today), today))
    }

    @Test
    fun `racha en riesgo dice cuantos dias lleva`() {
        val days = (1L..5L).map { today.minusDays(it) }
        assertEquals(StreakReminder.Message.StreakAtRisk(5), StreakReminder.message(days, today))
    }

    @Test
    fun `sin racha invita a empezar una`() {
        assertEquals(StreakReminder.Message.StartNew, StreakReminder.message(listOf(today.minusDays(4)), today))
    }
}
