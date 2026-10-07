package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.domain.model.MasteryRules
import com.jjas.labpomodoro.domain.model.MedalFacts
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.Medals
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.SessionType
import java.time.LocalDate

object MedalCalculator {

    /**
     * [counts]: número atómico → veces obtenido (como en la maestría). [fusions]: fusiones hechas.
     * Solo cuentan los pomodoros de trabajo completados, igual que la racha.
     */
    fun calculate(sessions: List<SessionEntity>, counts: Map<Int, Int>, fusions: Int): List<MedalProgress> {
        val done = sessions.filter { it.type == SessionType.WORK && it.completed }
        // La racha más larga no depende de qué día es hoy
        val longest = StreakCalculator.calculate(done.map { LocalDate.ofEpochDay(it.epochDay) }.toSet(), LocalDate.ofEpochDay(0)).longest
        val facts = MedalFacts(
            longestStreak = longest,
            workSeconds = done.sumOf { it.actualSeconds.toLong() },
            completedPomodoros = done.size,
            bestDayPomodoros = done.groupingBy { it.epochDay }.eachCount().values.maxOrNull() ?: 0,
            earlyPomodoros = done.count { it.hourOfDay in Medals.EARLY_HOURS },
            latePomodoros = done.count { it.hourOfDay in Medals.LATE_HOURS },
            fusions = fusions,
            discovered = counts.count { it.value > 0 },
            goldElements = counts.count { (z, n) -> MasteryRules.level(PeriodicTable[z], n) == Mastery.GOLD },
        )
        return Medals.evaluate(facts)
    }
}
