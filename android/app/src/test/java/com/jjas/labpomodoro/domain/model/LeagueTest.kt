package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LeagueTest {

    // Martes 6 de octubre de 2026, mediodía UTC
    private val now = Instant.parse("2026-10-06T12:00:00Z")

    @Test
    fun `semana iso en utc`() {
        assertEquals("2026-W41", LeagueRules.weekId(now))
        assertEquals(Instant.parse("2026-10-05T00:00:00Z"), LeagueRules.weekStart(now))
        assertEquals("2026-W40", LeagueRules.previousWeekId(now))
    }

    @Test
    fun `los primeros suben y los ultimos bajan`() {
        assertEquals(LeagueOutcome.PROMOTED, LeagueRules.outcome(League.CARBON, rank = 7, size = 30))
        assertEquals(LeagueOutcome.STAYED, LeagueRules.outcome(League.CARBON, rank = 8, size = 30))
        assertEquals(LeagueOutcome.DEMOTED, LeagueRules.outcome(League.CARBON, rank = 26, size = 30))
        assertEquals(LeagueOutcome.STAYED, LeagueRules.outcome(League.CARBON, rank = 25, size = 30))
    }

    @Test
    fun `no se sube del platino ni se baja del hidrogeno`() {
        assertEquals(LeagueOutcome.STAYED, LeagueRules.outcome(League.PLATINUM, rank = 1, size = 30))
        assertEquals(LeagueOutcome.STAYED, LeagueRules.outcome(League.HYDROGEN, rank = 30, size = 30))
    }

    @Test
    fun `las zonas son proporcionales al grupo`() {
        assertEquals(2, LeagueRules.promoteCount(10))
        assertEquals(2, LeagueRules.demoteCount(10))
        // En grupos chicos sube el primero y nadie baja
        assertEquals(LeagueOutcome.PROMOTED, LeagueRules.outcome(League.NEON, rank = 1, size = 6))
        assertEquals(LeagueOutcome.STAYED, LeagueRules.outcome(League.NEON, rank = 6, size = 6))
    }

    @Test
    fun `los bots completan el grupo y son iguales en todos los telefonos`() {
        val a = LeagueRules.bots("2026-W41_2_0", League.CARBON, realPlayers = 3, weekProgress = 0.5f)
        val b = LeagueRules.bots("2026-W41_2_0", League.CARBON, realPlayers = 3, weekProgress = 0.5f)
        assertEquals(7, a.size)
        assertEquals(a, b)
        assertTrue(a.all { it.isBot && it.nickname.startsWith("Asistente") })
        assertTrue(LeagueRules.bots("x", League.CARBON, realPlayers = 12, weekProgress = 1f).isEmpty())
    }

    @Test
    fun `los bots avanzan con la semana`() {
        val start = LeagueRules.bots("g", League.HELIUM, 1, 0f)
        val end = LeagueRules.bots("g", League.HELIUM, 1, 1f)
        assertTrue(start.all { it.xp == 0 })
        assertTrue(end.all { it.xp > 0 })
    }
}
