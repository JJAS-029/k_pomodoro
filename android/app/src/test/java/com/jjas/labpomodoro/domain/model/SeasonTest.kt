package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SeasonTest {

    @Test
    fun `dia de muertos`() {
        assertEquals(Season.MUERTOS, Season.on(LocalDate.of(2026, 11, 2)))
        assertEquals(Season.MUERTOS, Season.on(LocalDate.of(2026, 10, 25)))
        assertNull(Season.on(LocalDate.of(2026, 11, 4)))
    }

    @Test
    fun `el invierno cruza el año`() {
        assertEquals(Season.WINTER, Season.on(LocalDate.of(2026, 12, 24)))
        assertEquals(Season.WINTER, Season.on(LocalDate.of(2027, 1, 6)))
        assertNull(Season.on(LocalDate.of(2027, 1, 7)))
        assertNull(Season.on(LocalDate.of(2026, 7, 1)))
    }
}
