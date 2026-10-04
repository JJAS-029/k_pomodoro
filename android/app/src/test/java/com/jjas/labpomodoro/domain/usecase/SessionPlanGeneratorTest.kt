package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.model.SessionType.LONG_BREAK
import com.jjas.labpomodoro.domain.model.SessionType.SHORT_BREAK
import com.jjas.labpomodoro.domain.model.SessionType.WORK
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionPlanGeneratorTest {

    private fun plan(config: SessionConfig) = SessionPlanGenerator.generate(config)
    private fun types(config: SessionConfig): List<SessionType> = plan(config).map { it.type }
    private fun workMinutes(config: SessionConfig) =
        plan(config).filter { it.type == WORK }.map { it.durationSeconds / 60 }

    @Test
    fun `plan por defecto - 2 h de 25 min con descanso largo cada 4`() {
        // 120 / 25 = 4 enteros + 20 min
        val config = SessionConfig()
        assertEquals(
            listOf(WORK, SHORT_BREAK, WORK, SHORT_BREAK, WORK, SHORT_BREAK, WORK, LONG_BREAK, WORK),
            types(config),
        )
        assertEquals(listOf(25, 25, 25, 25, 20), workMinutes(config))
    }

    @Test
    fun `recortar nunca se pasa de las horas pedidas`() {
        // El caso del bug: 2 h con pomodoros de 50 min
        val config = SessionConfig(totalHours = 2, workMinutes = 50)
        assertEquals(listOf(50, 50, 20), workMinutes(config))
        assertEquals(120, workMinutes(config).sum())
    }

    @Test
    fun `pomodoros enteros redondea hacia arriba como el prototipo`() {
        val config = SessionConfig(totalHours = 2, workMinutes = 50, rounding = PlanRounding.WHOLE_POMODOROS)
        assertEquals(listOf(50, 50, 50), workMinutes(config))
    }

    @Test
    fun `division exacta da lo mismo con ambos modos`() {
        val trim = SessionConfig(totalHours = 2, workMinutes = 30)
        val whole = trim.copy(rounding = PlanRounding.WHOLE_POMODOROS)
        assertEquals(plan(trim), plan(whole))
        assertEquals(listOf(30, 30, 30, 30), workMinutes(trim))
    }

    @Test
    fun `nunca termina en descanso y alterna trabajo y descanso`() {
        val result = types(SessionConfig(totalHours = 5, workMinutes = 45, pomodorosUntilLong = 3))
        assertEquals(WORK, result.first())
        assertEquals(WORK, result.last())
        result.forEachIndexed { i, type -> assertEquals(i % 2 == 0, type == WORK) }
    }

    @Test
    fun `las duraciones de descanso salen de la configuracion`() {
        val result = plan(SessionConfig(totalHours = 2, workMinutes = 30, shortBreakMinutes = 7, longBreakMinutes = 20, pomodorosUntilLong = 2))
        assertEquals(7 * 60, result[1].durationSeconds)
        assertEquals(LONG_BREAK, result[3].type)
        assertEquals(20 * 60, result[3].durationSeconds)
    }

    @Test
    fun `menos tiempo que un pomodoro da un solo bloque recortado`() {
        val config = SessionConfig(totalHours = 1, workMinutes = 90)
        assertEquals(listOf(60), workMinutes(config))
        assertEquals(listOf(90), workMinutes(config.copy(rounding = PlanRounding.WHOLE_POMODOROS)))
    }
}
