package com.jjas.labpomodoro.domain.usecase

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Cuenta regresiva a una fecha ("Faltan 12 días para el examen"). */
object Countdown {

    /** Días que faltan; 0 = es hoy; negativo = ya pasó. */
    fun daysLeft(target: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    const val MAX_NAME = 32
}
