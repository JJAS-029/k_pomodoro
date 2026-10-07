package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.Medals
import com.jjas.labpomodoro.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MedalCalculatorTest {

    private fun work(day: Long, hour: Int = 10, seconds: Int = 25 * 60, completed: Boolean = true) = SessionEntity(
        type = SessionType.WORK,
        startedAtMillis = 0,
        endedAtMillis = 0,
        plannedSeconds = seconds,
        actualSeconds = seconds,
        completed = completed,
        epochDay = day,
        hourOfDay = hour,
    )

    private fun List<MedalProgress>.of(medal: Medal) = first { it.medal == medal }

    @Test
    fun `sin historial no hay medallas`() {
        val result = MedalCalculator.calculate(emptyList(), emptyMap(), 0)
        assertEquals(Medal.entries.size, result.size)
        assertTrue(result.none { it.earned })
    }

    @Test
    fun `la racha mas larga se conserva aunque la actual se haya roto`() {
        val sessions = (100L..106L).map { work(it) } + work(200)
        val result = MedalCalculator.calculate(sessions, emptyMap(), 0)
        assertTrue(result.of(Medal.STREAK_7).earned)
        assertFalse(result.of(Medal.STREAK_30).earned)
        assertEquals(7L, result.of(Medal.STREAK_30).shown)
    }

    @Test
    fun `los pomodoros saltados no cuentan`() {
        val result = MedalCalculator.calculate(listOf(work(1, completed = false)), emptyMap(), 0)
        assertFalse(result.of(Medal.FIRST_POMODORO).earned)
    }

    @Test
    fun `madrugada noche y maraton`() {
        val sessions = List(5) { work(1, hour = 6) } + List(3) { work(1, hour = 23) } + List(2) { work(2, hour = 0) }
        val result = MedalCalculator.calculate(sessions, emptyMap(), 0)
        assertTrue(result.of(Medal.EARLY_BIRD).earned)
        assertTrue(result.of(Medal.NIGHT_OWL).earned)
        assertTrue(result.of(Medal.MARATHON).earned)
    }

    @Test
    fun `horas de enfoque fusiones y elementos`() {
        val sessions = List(24) { work(it.toLong()) } // 10 h
        val counts = (1..10).associateWith { 1 } + (11 to 15) // el 11 llega a oro
        val result = MedalCalculator.calculate(sessions, counts, fusions = 1)
        assertTrue(result.of(Medal.HOURS_10).earned)
        assertFalse(result.of(Medal.HOURS_50).earned)
        assertTrue(result.of(Medal.FIRST_FUSION).earned)
        assertTrue(result.of(Medal.COLLECTOR).earned)
        assertTrue(result.of(Medal.FIRST_GOLD).earned)
    }

    @Test
    fun `solo se avisan las ganadas que no se han visto`() {
        val result = MedalCalculator.calculate(listOf(work(1)), mapOf(1 to 1), fusions = 1)
        assertEquals(listOf(Medal.FIRST_FUSION), Medals.unseen(result, setOf(Medal.FIRST_POMODORO.name)))
    }
}
