package com.jjas.labpomodoro.domain.usecase

import java.time.LocalDate

/** Qué decirle a la persona a la hora del recordatorio (los textos los pone quien notifica). */
object StreakReminder {

    sealed interface Message {
        /** La racha de [streak] días sigue viva pero hoy no ha trabajado. */
        data class StreakAtRisk(val streak: Int) : Message

        /** No hay racha: se invita a empezar una sin presionar. */
        data object StartNew : Message
    }

    /**
     * null si hoy ya hizo al menos un pomodoro (no hay nada que recordar). Si la racha sigue viva
     * pero hoy no ha trabajado, avisa que está en riesgo; si no hay racha, invita sin presionar.
     */
    fun message(activeDays: Collection<LocalDate>, today: LocalDate): Message? {
        if (today in activeDays) return null
        val streak = StreakCalculator.calculate(activeDays, today).current
        return if (streak > 0) Message.StreakAtRisk(streak) else Message.StartNew
    }
}
