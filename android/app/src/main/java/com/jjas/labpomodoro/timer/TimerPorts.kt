package com.jjas.labpomodoro.timer

import java.time.Instant

/** Fuente de tiempo del motor; en tests se reemplaza por una controlada. */
interface TimeSource {
    /** Milisegundos monotónicos que incluyen el tiempo dormido (`SystemClock.elapsedRealtime`). */
    fun elapsedRealtime(): Long

    /** Hora de pared, solo para el historial. */
    fun now(): Instant
}

/** Despierta al teléfono cuando acaba la sesión aunque esté en reposo (AlarmManager). */
interface DeadlineScheduler {
    fun schedule(atElapsedRealtime: Long)
    fun cancel()
}

/** Arranca el servicio en primer plano que mantiene viva la app mientras hay un plan en curso. */
fun interface TimerServiceLauncher {
    fun start()
}
