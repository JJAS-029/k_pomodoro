package com.jjas.labpomodoro.domain.usecase

/** Meta diaria de pomodoros: cuántos lleva hoy y la racha de días que la cumplieron. */
object DailyGoal {

    /**
     * Días seguidos que cumplieron la meta. Termina hoy si hoy ya se cumplió; si no, ayer (hoy
     * todavía hay tiempo y no rompe la racha).
     *
     * @param counts día (epochDay) → pomodoros completados.
     */
    fun streak(counts: Map<Long, Int>, today: Long, goal: Int): Int {
        if (goal <= 0) return 0
        var day = if ((counts[today] ?: 0) >= goal) today else today - 1
        var streak = 0
        while ((counts[day] ?: 0) >= goal) {
            streak++
            day--
        }
        return streak
    }

    val GOAL_RANGE = 0..16
}
