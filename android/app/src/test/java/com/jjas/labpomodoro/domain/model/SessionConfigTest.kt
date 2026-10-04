package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionConfigTest {

    @Test
    fun `los valores por defecto son los del prototipo y recortan`() {
        val config = SessionConfig()
        assertEquals(2, config.totalHours)
        assertEquals(25, config.workMinutes)
        assertEquals(5, config.shortBreakMinutes)
        assertEquals(15, config.longBreakMinutes)
        assertEquals(4, config.pomodorosUntilLong)
        assertEquals(PlanRounding.TRIM_LAST, config.rounding)
    }

    @Test
    fun `normalized fuerza los valores a sus rangos`() {
        val config = SessionConfig(
            totalHours = 0,
            workMinutes = 500,
            shortBreakMinutes = -3,
            longBreakMinutes = 1,
            pomodorosUntilLong = 99,
        ).normalized()

        assertEquals(1, config.totalHours)
        assertEquals(120, config.workMinutes)
        assertEquals(1, config.shortBreakMinutes)
        assertEquals(5, config.longBreakMinutes)
        assertEquals(10, config.pomodorosUntilLong)
    }

    @Test
    fun `normalized no toca valores validos`() {
        val config = SessionConfig(totalHours = 3, workMinutes = 50, rounding = PlanRounding.WHOLE_POMODOROS)
        assertEquals(config, config.normalized())
    }
}
