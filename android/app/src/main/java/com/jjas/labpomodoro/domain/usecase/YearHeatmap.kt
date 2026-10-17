package com.jjas.labpomodoro.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate

/** Un día del mapa de calor: minutos de enfoque completados. */
data class HeatDay(val date: LocalDate, val minutes: Int) {
    /** Intensidad 0..4, como en GitHub: nada, poco, un pomodoro largo, una hora, dos horas o más. */
    val level: Int
        get() = when {
            minutes <= 0 -> 0
            minutes < 25 -> 1
            minutes < 60 -> 2
            minutes < 120 -> 3
            else -> 4
        }
}

object YearHeatmap {

    const val WEEKS = 53

    /**
     * Semanas de lunes a domingo que terminan en la de [today]. Los días que aún no llegan son null.
     *
     * @param minutes día (epochDay) → minutos de enfoque.
     */
    fun weeks(minutes: Map<Long, Int>, today: LocalDate, weeks: Int = WEEKS): List<List<HeatDay?>> {
        val lastMonday = today.with(DayOfWeek.MONDAY)
        val firstMonday = lastMonday.minusWeeks((weeks - 1).toLong())
        return List(weeks) { w ->
            List(7) { d ->
                val date = firstMonday.plusDays(w * 7L + d)
                if (date.isAfter(today)) null else HeatDay(date, minutes[date.toEpochDay()] ?: 0)
            }
        }
    }
}
