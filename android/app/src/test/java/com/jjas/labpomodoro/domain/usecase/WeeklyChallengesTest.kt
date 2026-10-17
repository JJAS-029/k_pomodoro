package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeeklyChallengesTest {

    private val thursday = LocalDate.of(2026, 10, 15)

    private fun work(day: LocalDate, hour: Int, minutes: Int = 25, tag: Long? = null, completed: Boolean = true) = SessionEntity(
        type = SessionType.WORK,
        startedAtMillis = 0,
        endedAtMillis = 0,
        plannedSeconds = minutes * 60,
        actualSeconds = minutes * 60,
        completed = completed,
        epochDay = day.toEpochDay(),
        hourOfDay = hour,
        tagId = tag,
    )

    @Test
    fun `tres retos distintos, iguales toda la semana`() {
        val monday = WeeklyChallenges.weekStart(thursday)
        val picked = WeeklyChallenges.pick(monday)
        assertEquals(3, picked.toSet().size)
        assertEquals(picked, WeeklyChallenges.pick(WeeklyChallenges.weekStart(thursday.plusDays(3))))
        // Alguna semana cercana sortea otros
        assertTrue((1..8).any { WeeklyChallenges.pick(monday.plusWeeks(it.toLong())) != picked })
    }

    @Test
    fun `solo cuenta la semana actual y los pomodoros completados`() {
        val sessions = listOf(
            work(thursday, 8, tag = 1),
            work(thursday.minusDays(3), 7), // lunes
            work(thursday.minusDays(4), 7), // domingo pasado: otra semana
            work(thursday, 8, completed = false),
        )
        val all = ChallengeKind.entries.associateWith { kind ->
            // Se evalúa cada reto aunque no salga esta semana
            WeeklyChallenges.measure(kind, sessions, thursday)
        }
        assertEquals(2, all[ChallengeKind.EARLY_BIRD])
        assertEquals(2, all[ChallengeKind.STEADY_DAYS])
        assertEquals(50, all[ChallengeKind.FOCUS_MINUTES])
        assertEquals(1, all[ChallengeKind.BIG_DAY])
        assertEquals(1, all[ChallengeKind.TAGGED])
        assertEquals(0, all[ChallengeKind.WEEKEND])
    }

    @Test
    fun `la clave cambia por semana`() {
        assertNotEquals(
            WeeklyChallenges.claimKey(thursday, ChallengeKind.TAGGED),
            WeeklyChallenges.claimKey(thursday.plusWeeks(1), ChallengeKind.TAGGED),
        )
    }
}
