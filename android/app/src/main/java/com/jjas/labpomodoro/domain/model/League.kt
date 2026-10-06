package com.jjas.labpomodoro.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import kotlin.random.Random

/** Ligas semanales, de la más baja a la más alta. Todos empiezan en Hidrógeno. */
enum class League(val label: String, val symbol: String, val color: Long) {
    HYDROGEN("Hidrógeno", "H", 0xFFB3E5FC),
    HELIUM("Helio", "He", 0xFFFFB3C1),
    CARBON("Carbono", "C", 0xFF9E9E9E),
    NITROGEN("Nitrógeno", "N", 0xFF90CAF9),
    OXYGEN("Oxígeno", "O", 0xFF4FC3F7),
    NEON("Neón", "Ne", 0xFFFF5722),
    IRON("Hierro", "Fe", 0xFFD87C2A),
    SILVER("Plata", "Ag", 0xFFE0E0E0),
    GOLD("Oro", "Au", 0xFFFFC94A),
    PLATINUM("Platino", "Pt", 0xFFB9F2FF),
    ;

    val next: League? get() = entries.getOrNull(ordinal + 1)
    val previous: League? get() = entries.getOrNull(ordinal - 1)
}

/** Un participante del grupo. Los bots se generan en el teléfono y siempre se marcan como tales. */
data class LeagueMember(
    val id: String,
    val nickname: String,
    /** Número atómico del elemento que usa como avatar. */
    val avatar: Int,
    /** Minutos de enfoque completados en la semana. */
    val xp: Int,
    val isBot: Boolean = false,
)

enum class LeagueOutcome { PROMOTED, STAYED, DEMOTED }

object LeagueRules {

    const val GROUP_SIZE = 30
    const val PROMOTE = 7
    const val DEMOTE = 5

    /** Con menos participantes reales que esto, el grupo se completa con bots. */
    const val MIN_PLAYERS = 10

    /** Semanas ISO en UTC, para que todos los grupos cierren al mismo tiempo: "2026-W41". */
    fun weekId(instant: Instant): String {
        val date = instant.atZone(ZoneOffset.UTC).toLocalDate()
        return "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
    }

    /** Inicio (lunes 00:00 UTC) de la semana de [instant]. */
    fun weekStart(instant: Instant): Instant =
        instant.atZone(ZoneOffset.UTC).toLocalDate()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .atStartOfDay(ZoneOffset.UTC).toInstant()

    fun weekEnd(instant: Instant): Instant = weekStart(instant).plusSeconds(7 * 24 * 3600)

    /** Ordena por puntos; a igualdad, por apodo para que el orden sea estable. */
    fun ranking(members: List<LeagueMember>): List<LeagueMember> =
        members.sortedWith(compareByDescending<LeagueMember> { it.xp }.thenBy { it.nickname })

    /** Cuántos suben: 7 de 30, proporcional en grupos más chicos (al menos uno). */
    fun promoteCount(size: Int): Int = maxOf(1, (size * PROMOTE + GROUP_SIZE / 2) / GROUP_SIZE)

    /** Cuántos bajan: 5 de 30, proporcional; en grupos de menos de [MIN_PLAYERS] nadie baja. */
    fun demoteCount(size: Int): Int = if (size < MIN_PLAYERS) 0 else (size * DEMOTE + GROUP_SIZE / 2) / GROUP_SIZE

    /**
     * Qué le toca a quien quedó en [rank] (1 = primero) de [size] participantes. En la liga más
     * alta no se sube y en la más baja no se baja.
     */
    fun outcome(league: League, rank: Int, size: Int): LeagueOutcome = when {
        rank <= promoteCount(size) && league.next != null -> LeagueOutcome.PROMOTED
        rank > size - demoteCount(size) && league.previous != null -> LeagueOutcome.DEMOTED
        else -> LeagueOutcome.STAYED
    }

    fun nextLeague(league: League, outcome: LeagueOutcome): League = when (outcome) {
        LeagueOutcome.PROMOTED -> league.next ?: league
        LeagueOutcome.DEMOTED -> league.previous ?: league
        LeagueOutcome.STAYED -> league
    }

    private val BOT_NAMES = listOf(
        "Curie", "Newton", "Mendeléyev", "Lavoisier", "Faraday", "Franklin", "Bohr", "Hopper",
        "Lovelace", "Meitner", "Tesla", "Darwin", "Kepler", "Noether", "Galileo",
    )

    /**
     * Completa el grupo con asistentes de laboratorio (bots) hasta [MIN_PLAYERS]. Son deterministas
     * para el grupo y la semana: todos los teléfonos ven los mismos y con los mismos puntos.
     * Acumulan minutos a su ritmo según cuánto ha avanzado la semana ([weekProgress], 0..1).
     */
    fun bots(groupId: String, league: League, realPlayers: Int, weekProgress: Float): List<LeagueMember> {
        val missing = (MIN_PLAYERS - realPlayers).coerceAtLeast(0)
        val random = Random(groupId.hashCode())
        // En ligas más altas los bots rinden más: la competencia sube con la liga
        val base = 60 + league.ordinal * 45
        return List(missing) { i ->
            val weeklyGoal = base + random.nextInt(base * 2)
            LeagueMember(
                id = "bot_$i",
                nickname = "Asistente ${BOT_NAMES[(random.nextInt(BOT_NAMES.size) + i) % BOT_NAMES.size]}",
                avatar = 1 + random.nextInt(PeriodicTable.SIZE),
                xp = (weeklyGoal * weekProgress.coerceIn(0f, 1f)).toInt(),
                isBot = true,
            )
        }
    }

    /** Fracción de la semana transcurrida, para el avance de los bots. */
    fun weekProgress(now: Instant): Float {
        val start = weekStart(now).epochSecond
        return ((now.epochSecond - start).toFloat() / (7 * 24 * 3600)).coerceIn(0f, 1f)
    }

    /** La semana anterior a la de [now] (para cerrar resultados). */
    fun previousWeekId(now: Instant): String = weekId(weekStart(now).minusSeconds(1))
}
