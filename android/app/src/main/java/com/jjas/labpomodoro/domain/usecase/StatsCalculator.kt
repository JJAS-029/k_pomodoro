package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.SessionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class StatsRange(val label: String, val days: Long?) {
    WEEK("7 días", 7),
    MONTH("30 días", 30),
    ALL("Todo", null),
}

/** Una barra de la línea de tiempo: un día (7 y 30 días) o un mes (Todo). */
data class TimeBucket(val start: LocalDate, val seconds: Long)

data class FocusStats(
    val range: StatsRange,
    /** Enfoque completado (solo pomodoros de trabajo completados). */
    val workSeconds: Long,
    val completed: Int,
    val skipped: Int,
    /** Días distintos con al menos un pomodoro completado. */
    val activeDays: Int,
    val timeline: List<TimeBucket>,
    /** Segundos por día de la semana, lunes primero. */
    val byWeekday: List<Long>,
    /** Segundos por hora del día, 0..23. */
    val byHour: List<Long>,
) {
    /** Porcentaje de pomodoros de trabajo que se terminaron sin saltarlos; null si no hay. */
    val completionRate: Int? get() = (completed + skipped).takeIf { it > 0 }?.let { completed * 100 / it }
    val averagePerActiveDaySeconds: Long get() = if (activeDays == 0) 0 else workSeconds / activeDays
    val bestWeekday: DayOfWeek? get() = byWeekday.indexOfMax()?.let { DayOfWeek.of(it + 1) }
    val bestHour: Int? get() = byHour.indexOfMax()
    val isEmpty: Boolean get() = completed == 0 && skipped == 0
}

private fun List<Long>.indexOfMax(): Int? = withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index

/**
 * Estadísticas del historial. Usa el día y la hora locales guardados con cada sesión, así que un
 * viaje no reacomoda el pasado.
 */
object StatsCalculator {

    fun calculate(sessions: List<SessionEntity>, today: LocalDate, range: StatsRange): FocusStats {
        val from = range.days?.let { today.minusDays(it - 1) }
        val work = sessions.filter { it.type == SessionType.WORK }
            .filter { from == null || LocalDate.ofEpochDay(it.epochDay) >= from }
        val done = work.filter { it.completed }

        val byWeekday = LongArray(7)
        val byHour = LongArray(24)
        done.forEach { s ->
            byWeekday[LocalDate.ofEpochDay(s.epochDay).dayOfWeek.value - 1] += s.actualSeconds.toLong()
            byHour[s.hourOfDay.coerceIn(0, 23)] += s.actualSeconds.toLong()
        }

        return FocusStats(
            range = range,
            workSeconds = done.sumOf { it.actualSeconds.toLong() },
            completed = done.size,
            skipped = work.size - done.size,
            activeDays = done.map { it.epochDay }.distinct().size,
            timeline = if (from != null) daily(done, from, today) else monthly(done, today),
            byWeekday = byWeekday.toList(),
            byHour = byHour.toList(),
        )
    }

    private fun daily(done: List<SessionEntity>, from: LocalDate, today: LocalDate): List<TimeBucket> {
        val perDay = done.groupBy { it.epochDay }.mapValues { (_, v) -> v.sumOf { it.actualSeconds.toLong() } }
        return generateSequence(from) { it.plusDays(1) }
            .takeWhile { it <= today }
            .map { TimeBucket(it, perDay[it.toEpochDay()] ?: 0) }
            .toList()
    }

    /** Últimos 12 meses (o desde el primer mes con datos, si es más reciente). */
    private fun monthly(done: List<SessionEntity>, today: LocalDate): List<TimeBucket> {
        val current = YearMonth.from(today)
        val first = done.minOfOrNull { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) } ?: current
        val start = maxOf(first, current.minusMonths(11))
        val perMonth = done.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.epochDay)) }
            .mapValues { (_, v) -> v.sumOf { it.actualSeconds.toLong() } }
        return generateSequence(start) { it.plusMonths(1) }
            .takeWhile { it <= current }
            .map { TimeBucket(it.atDay(1), perMonth[it] ?: 0) }
            .toList()
    }
}
