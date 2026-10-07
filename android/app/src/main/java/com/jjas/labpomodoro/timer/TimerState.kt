package com.jjas.labpomodoro.timer

import com.jjas.labpomodoro.domain.model.PlannedSession
import java.time.Instant

/** Un poco después del cruce, para no caer justo antes por imprecisión del reloj. */
private const val SECOND_ALIGN_MARGIN_MILLIS = 20L

sealed interface TimerState {

    data object Idle : TimerState

    /**
     * Hay un plan en curso. Mientras corre, el tiempo se deriva de [endsAtElapsed] (marca de
     * `elapsedRealtime`, que sigue avanzando aunque el teléfono duerma); en pausa se congela en
     * [remainingWhenPausedMillis].
     */
    data class Active(
        val plan: List<PlannedSession>,
        val index: Int,
        val isPaused: Boolean,
        val endsAtElapsed: Long,
        val remainingWhenPausedMillis: Long,
        val sessionStartedAt: Instant,
        val completedWorkSessions: Int,
        val completedWorkSeconds: Long,
        /** Semilla del plan: da a cada pomodoro un color al azar pero estable (vaso y tubo). */
        val planSeed: Long,
        /** Sesiones saltadas: su tubo en la repisa queda vacío. */
        val skippedIndices: Set<Int> = emptySet(),
    ) : TimerState {
        val current: PlannedSession get() = plan[index]
        val next: PlannedSession? get() = plan.getOrNull(index + 1)

        fun remainingMillis(nowElapsed: Long): Long =
            if (isPaused) remainingWhenPausedMillis else (endsAtElapsed - nowElapsed).coerceAtLeast(0)

        /**
         * Cuánto falta para que el tiempo restante cruce el siguiente segundo entero. Los
         * cronómetros del sistema (widget, notificación) avanzan cada segundo desde que se dibujan:
         * si se actualizan justo después del cruce, marcan exactamente lo mismo que la app.
         */
        fun millisToNextSecond(nowElapsed: Long): Long = remainingMillis(nowElapsed) % 1000 + SECOND_ALIGN_MARGIN_MILLIS
    }

    data class Finished(
        val completedWorkSessions: Int,
        val completedWorkSeconds: Long,
    ) : TimerState
}

sealed interface TimerEvent {
    data object PlanStarted : TimerEvent

    /** Una sesión terminó, completada o saltada. [next] es null si era la última del plan. */
    data class SessionEnded(
        val session: PlannedSession,
        val startedAt: Instant,
        val completed: Boolean,
        val next: PlannedSession?,
        /** Terminó mientras la app estaba cerrada y se registra al recuperar el plan: sin sonido. */
        val late: Boolean = false,
    ) : TimerEvent
}
