package com.jjas.labpomodoro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CountdownTest {

    private val today = LocalDate.of(2026, 10, 16)

    @Test
    fun `dias que faltan`() {
        assertEquals(12, Countdown.daysLeft(LocalDate.of(2026, 10, 28), today))
        assertEquals(0, Countdown.daysLeft(today, today))
        assertEquals(-1, Countdown.daysLeft(today.minusDays(1), today))
    }

    @Test
    fun `cruza de año`() {
        assertEquals(77, Countdown.daysLeft(LocalDate.of(2027, 1, 1), today))
    }
}
