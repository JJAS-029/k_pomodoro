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

/**
 * El plan en curso tal como quedó guardado, para recuperarlo si Android cierra el proceso.
 * [savedAtWallMillis] y [savedAtElapsed] son el mismo instante en los dos relojes: sirven para saber
 * si el teléfono se reinició (el reloj de `elapsedRealtime` vuelve a cero) y traducir los tiempos.
 */
data class TimerSnapshot(
    val state: TimerState.Active,
    val savedAtWallMillis: Long,
    val savedAtElapsed: Long,
)

/** Guarda el plan en curso fuera de la memoria; null lo borra. */
interface TimerStore {
    suspend fun save(snapshot: TimerSnapshot?)
    suspend fun load(): TimerSnapshot?
}
