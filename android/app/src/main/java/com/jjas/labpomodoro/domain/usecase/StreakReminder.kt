package com.jjas.labpomodoro.domain.usecase

import java.time.LocalDate

/** Qué decirle a la persona a la hora del recordatorio. */
object StreakReminder {

    data class Message(val title: String, val text: String)

    /**
     * null si hoy ya hizo al menos un pomodoro (no hay nada que recordar). Si la racha sigue viva
     * pero hoy no ha trabajado, avisa que está en riesgo; si no hay racha, invita sin presionar.
     */
    fun message(activeDays: Collection<LocalDate>, today: LocalDate): Message? {
        if (today in activeDays) return null
        val streak = StreakCalculator.calculate(activeDays, today).current
        return if (streak > 0) {
            Message(
                title = "Tu racha de $streak ${if (streak == 1) "día" else "días"} te espera",
                text = "Un pomodoro antes de dormir y la mantienes viva.",
            )
        } else {
            Message(
                title = "¿Un experimento hoy?",
                text = "Con un solo pomodoro empiezas una racha nueva.",
            )
        }
    }
}
