package com.jjas.labpomodoro.timer

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Máquina de estados del plan de pomodoros. Vive fuera de la Activity (singleton), así que sobrevive a
 * rotaciones y a cerrar la pantalla; el [TimerServiceLauncher] mantiene el proceso vivo.
 *
 * El fin de cada sesión se detecta por dos vías que llaman a [onDeadline] (idempotente): un `delay`
 * interno cuando el proceso está despierto y una alarma del sistema cuando el teléfono duerme.
 */
@Singleton
class TimerEngine @Inject constructor(
    private val time: TimeSource,
    private val sessions: SessionRepository,
    private val scheduler: DeadlineScheduler,
    private val serviceLauncher: TimerServiceLauncher,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var deadlineJob: Job? = null

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    suspend fun start(config: SessionConfig) {
        mutex.withLock {
            if (_state.value is TimerState.Active) return
            val plan = SessionPlanGenerator.generate(config)
            _state.value = TimerState.Active(
                plan = plan,
                index = 0,
                isPaused = false,
                endsAtElapsed = time.elapsedRealtime() + plan.first().durationSeconds * 1000L,
                remainingWhenPausedMillis = 0,
                sessionStartedAt = time.now(),
                completedWorkSessions = 0,
                completedWorkSeconds = 0,
            )
            armDeadline()
        }
        serviceLauncher.start()
        _events.emit(TimerEvent.PlanStarted)
    }

    suspend fun pause() = mutex.withLock {
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.isPaused) return@withLock
        disarmDeadline()
        _state.value = s.copy(isPaused = true, remainingWhenPausedMillis = s.remainingMillis(time.elapsedRealtime()))
    }

    suspend fun resume() = mutex.withLock {
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (!s.isPaused) return@withLock
        _state.value = s.copy(isPaused = false, endsAtElapsed = time.elapsedRealtime() + s.remainingWhenPausedMillis)
        armDeadline()
    }

    /** Saltar ≠ completar: se guarda como no completada y no da racha ni recompensas. */
    suspend fun skip() = mutex.withLock {
        if (_state.value is TimerState.Active) finishCurrent(completed = false)
    }

    /** Abandona el plan sin guardar la sesión en curso. */
    suspend fun reset() = mutex.withLock {
        disarmDeadline()
        _state.value = TimerState.Idle
    }

    /** Llamado por el `delay` interno y por la alarma; no hace nada si la sesión aún no termina. */
    suspend fun onDeadline() = mutex.withLock {
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.isPaused) return@withLock
        if (s.remainingMillis(time.elapsedRealtime()) > 0) {
            armDeadline() // La alarma se adelantó: se reprograma
        } else {
            finishCurrent(completed = true)
        }
    }

    private suspend fun finishCurrent(completed: Boolean) {
        val s = _state.value as TimerState.Active
        val session = s.current
        val nowElapsed = time.elapsedRealtime()
        val actualSeconds = if (completed) {
            session.durationSeconds
        } else {
            session.durationSeconds - (s.remainingMillis(nowElapsed) / 1000).toInt()
        }
        val now = time.now()
        sessions.record(session.type, s.sessionStartedAt, now, session.durationSeconds, actualSeconds, completed)

        val countsAsWork = completed && session.type == SessionType.WORK
        val workSessions = s.completedWorkSessions + if (countsAsWork) 1 else 0
        val workSeconds = s.completedWorkSeconds + if (countsAsWork) session.durationSeconds else 0

        val next = s.next
        if (next == null) {
            disarmDeadline()
            _state.value = TimerState.Finished(workSessions, workSeconds)
        } else {
            _state.value = s.copy(
                index = s.index + 1,
                isPaused = false,
                endsAtElapsed = nowElapsed + next.durationSeconds * 1000L,
                remainingWhenPausedMillis = 0,
                sessionStartedAt = now,
                completedWorkSessions = workSessions,
                completedWorkSeconds = workSeconds,
            )
            armDeadline()
        }
        _events.emit(TimerEvent.SessionEnded(session, s.sessionStartedAt, completed, next))
    }

    /** Vuelve a Idle desde la pantalla de resumen. */
    suspend fun dismissFinished() = mutex.withLock {
        if (_state.value is TimerState.Finished) _state.value = TimerState.Idle
    }

    private fun armDeadline() {
        val s = _state.value as TimerState.Active
        deadlineJob?.cancel()
        scheduler.schedule(s.endsAtElapsed)
        deadlineJob = scope.launch {
            delay(s.endsAtElapsed - time.elapsedRealtime())
            // En otra corrutina: onDeadline vuelve a armar y cancelaría a esta misma
            scope.launch { onDeadline() }
        }
    }

    private fun disarmDeadline() {
        deadlineJob?.cancel()
        deadlineJob = null
        scheduler.cancel()
    }
}
