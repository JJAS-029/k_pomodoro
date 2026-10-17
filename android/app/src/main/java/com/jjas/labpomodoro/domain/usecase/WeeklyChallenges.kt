package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

/** Retos de la semana: cada uno con su meta. */
enum class ChallengeKind(val target: Int) {
    /** Pomodoros empezados antes de las 9 de la mañana. */
    EARLY_BIRD(3),
    /** Días distintos con al menos un pomodoro. */
    STEADY_DAYS(5),
    /** Minutos de enfoque en la semana. */
    FOCUS_MINUTES(300),
    /** Pomodoros en el mejor día de la semana. */
    BIG_DAY(6),
    /** Pomodoros con etiqueta. */
    TAGGED(5),
    /** Pomodoros en sábado o domingo. */
    WEEKEND(2),
}

data class ChallengeProgress(val kind: ChallengeKind, val current: Int) {
    val done: Boolean get() = current >= kind.target
    val fraction: Float get() = (current.toFloat() / kind.target).coerceIn(0f, 1f)
}

/**
 * Tres retos por semana (de lunes a domingo), iguales para todos esa semana. Cada uno cumplido da
 * un elemento raro, una sola vez.
 */
object WeeklyChallenges {

    const val PER_WEEK = 3

    fun weekStart(today: LocalDate): LocalDate = today.with(DayOfWeek.MONDAY)

    /** Los retos de la semana que empieza en [monday]: se sortean con la semana como semilla. */
    fun pick(monday: LocalDate): List<ChallengeKind> =
        ChallengeKind.entries.shuffled(Random(monday.toEpochDay())).take(PER_WEEK)

    /** Avance de cada reto de la semana de [today], con el historial completo. */
    fun progress(sessions: List<SessionEntity>, today: LocalDate): List<ChallengeProgress> =
        pick(weekStart(today)).map { kind -> ChallengeProgress(kind, measure(kind, sessions, today)) }

    /** Cuánto lleva un reto en la semana de [today] (solo pomodoros completados). */
    internal fun measure(kind: ChallengeKind, sessions: List<SessionEntity>, today: LocalDate): Int {
        val from = weekStart(today).toEpochDay()
        return current(kind, sessions.filter { it.type == SessionType.WORK && it.completed && it.epochDay in from until from + 7 })
    }

    /** Clave para no entregar dos veces el premio del mismo reto ("2026-10-12:EARLY_BIRD"). */
    fun claimKey(today: LocalDate, kind: ChallengeKind) = "${weekStart(today)}:${kind.name}"

    private fun current(kind: ChallengeKind, week: List<SessionEntity>): Int = when (kind) {
        ChallengeKind.EARLY_BIRD -> week.count { it.hourOfDay < 9 }
        ChallengeKind.STEADY_DAYS -> week.map { it.epochDay }.distinct().size
        ChallengeKind.FOCUS_MINUTES -> week.sumOf { it.actualSeconds } / 60
        ChallengeKind.BIG_DAY -> week.groupingBy { it.epochDay }.eachCount().values.maxOrNull() ?: 0
        ChallengeKind.TAGGED -> week.count { it.tagId != null }
        ChallengeKind.WEEKEND -> week.count { LocalDate.ofEpochDay(it.epochDay).dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY) }
    }
}
