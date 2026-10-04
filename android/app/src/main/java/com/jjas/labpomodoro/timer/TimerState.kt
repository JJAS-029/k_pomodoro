package com.jjas.labpomodoro.timer

import com.jjas.labpomodoro.domain.model.PlannedSession
import java.time.Instant

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
    ) : TimerState {
        val current: PlannedSession get() = plan[index]
        val next: PlannedSession? get() = plan.getOrNull(index + 1)

        fun remainingMillis(nowElapsed: Long): Long =
            if (isPaused) remainingWhenPausedMillis else (endsAtElapsed - nowElapsed).coerceAtLeast(0)
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
    ) : TimerEvent
}
