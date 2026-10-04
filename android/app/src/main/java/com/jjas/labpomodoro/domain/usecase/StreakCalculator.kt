package com.jjas.labpomodoro.domain.usecase

import java.time.LocalDate

data class Streak(val current: Int, val longest: Int)

/**
 * Racha = días consecutivos con al menos un pomodoro de trabajo completado.
 * La racha actual sigue viva si el último día con actividad fue hoy o ayer (aún hay tiempo de mantenerla).
 */
object StreakCalculator {

    fun calculate(activeDays: Collection<LocalDate>, today: LocalDate): Streak {
        if (activeDays.isEmpty()) return Streak(0, 0)
        val days = activeDays.toSortedSet().toList()

        var longest = 1
        var run = 1
        for (i in 1 until days.size) {
            run = if (days[i - 1].plusDays(1) == days[i]) run + 1 else 1
            longest = maxOf(longest, run)
        }

        // `run` termina siendo la racha que acaba en el último día con actividad
        val last = days.last()
        val current = if (last == today || last == today.minusDays(1)) run else 0
        return Streak(current = current, longest = longest)
    }
}
