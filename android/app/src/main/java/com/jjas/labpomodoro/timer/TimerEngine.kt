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
import kotlin.math.abs

/**
 * Máquina de estados del plan de pomodoros. Vive fuera de la Activity (singleton), así que sobrevive a
 * rotaciones y a cerrar la pantalla; el [TimerServiceLauncher] mantiene el proceso vivo.
 *
 * El fin de cada sesión se detecta por dos vías que llaman a [onDeadline] (idempotente): un `delay`
 * interno cuando el proceso está despierto y una alarma del sistema cuando el teléfono duerme.
 *
 * Cada cambio del plan se guarda en [store]; si Android cierra el proceso (o el teléfono se
 * reinicia), [ensureRestored] lo recupera la próxima vez que algo despierta a la app.
 */
@Singleton
class TimerEngine @Inject constructor(
    private val time: TimeSource,
    private val sessions: SessionRepository,
    private val scheduler: DeadlineScheduler,
    private val serviceLauncher: TimerServiceLauncher,
    private val store: TimerStore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private var deadlineJob: Job? = null
    private var restored = false

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TimerEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()

    /** Recupera el plan guardado (una sola vez por proceso). Lo llaman la app, el servicio y los receptores. */
    suspend fun ensureRestored() = mutex.withLock { restoreLocked() }

    suspend fun start(config: SessionConfig) {
        mutex.withLock {
            restoreLocked()
            if (_state.value is TimerState.Active) return
            val plan = SessionPlanGenerator.generate(config)
            val now = time.now()
            publish(
                TimerState.Active(
                    plan = plan,
                    index = 0,
                    isPaused = false,
                    endsAtElapsed = time.elapsedRealtime() + plan.first().durationSeconds * 1000L,
                    remainingWhenPausedMillis = 0,
                    sessionStartedAt = now,
                    completedWorkSessions = 0,
                    completedWorkSeconds = 0,
                    planSeed = now.toEpochMilli(),
                )
            )
            armDeadline()
        }
        serviceLauncher.start()
        _events.emit(TimerEvent.PlanStarted)
    }

    suspend fun pause() = mutex.withLock {
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.isPaused) return@withLock
        disarmDeadline()
        publish(s.copy(isPaused = true, remainingWhenPausedMillis = s.remainingMillis(time.elapsedRealtime())))
    }

    suspend fun resume() = mutex.withLock {
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (!s.isPaused) return@withLock
        publish(s.copy(isPaused = false, endsAtElapsed = time.elapsedRealtime() + s.remainingWhenPausedMillis))
        armDeadline()
    }

    /** Saltar ≠ completar: se guarda como no completada y no da racha ni recompensas. */
    suspend fun skip() = mutex.withLock {
        restoreLocked()
        if (_state.value is TimerState.Active) finishCurrent(completed = false)
    }

    /** Abandona el plan sin guardar la sesión en curso. */
    suspend fun reset() = mutex.withLock {
        restoreLocked()
        disarmDeadline()
        publish(TimerState.Idle)
    }

    /** Llamado por el `delay` interno y por la alarma; no hace nada si la sesión aún no termina. */
    suspend fun onDeadline() = mutex.withLock {
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.isPaused) return@withLock
        if (s.remainingMillis(time.elapsedRealtime()) > 0) {
            armDeadline() // La alarma se adelantó: se reprograma
        } else {
            finishCurrent(completed = true)
        }
    }

    /** Vuelve a Idle desde la pantalla de resumen. */
    suspend fun dismissFinished() = mutex.withLock {
        if (_state.value is TimerState.Finished) publish(TimerState.Idle)
    }

    private suspend fun restoreLocked() {
        if (restored) return
        restored = true
        // Si en este proceso ya se empezó algo, eso manda
        if (_state.value !is TimerState.Idle) return
        val snapshot = store.load() ?: return
        val nowElapsed = time.elapsedRealtime()
        val nowWall = time.now().toEpochMilli()
        if (nowWall - snapshot.savedAtWallMillis !in 0..MAX_RESTORE_AGE_MILLIS) {
            store.save(null)
            return
        }
        var s = snapshot.state
        // Mismo arranque del teléfono: elapsedRealtime sigue valiendo. Si se reinició, se traduce
        // con la hora de pared
        val bootShift = (nowWall - nowElapsed) - (snapshot.savedAtWallMillis - snapshot.savedAtElapsed)
        if (abs(bootShift) > SAME_BOOT_TOLERANCE_MILLIS) {
            s = s.copy(endsAtElapsed = s.endsAtElapsed - bootShift)
        }
        _state.value = s
        // Las sesiones que terminaron con la app cerrada se cierran a la hora en que acabaron
        while (true) {
            val active = _state.value as? TimerState.Active ?: break
            if (active.isPaused || active.remainingMillis(nowElapsed) > 0) break
            finishCurrent(completed = true, atElapsed = active.endsAtElapsed, late = true)
        }
        val current = _state.value
        if (current is TimerState.Active) {
            if (!current.isPaused) armDeadline()
            store.save(snapshotOf(current))
            // Desde segundo plano Android puede no dejar iniciarlo; la notificación vuelve al abrir la app
            runCatching { serviceLauncher.start() }
        } else {
            store.save(null)
        }
    }

    /**
     * Cierra la sesión actual. [atElapsed] es cuándo terminó: ahora, o antes si se está recuperando
     * un plan que siguió corriendo con la app cerrada.
     */
    private suspend fun finishCurrent(completed: Boolean, atElapsed: Long = time.elapsedRealtime(), late: Boolean = false) {
        val s = _state.value as TimerState.Active
        val session = s.current
        val actualSeconds = if (completed) {
            session.durationSeconds
        } else {
            session.durationSeconds - (s.remainingMillis(atElapsed) / 1000).toInt()
        }
        val endedAt = time.now().minusMillis(time.elapsedRealtime() - atElapsed)
        sessions.record(session.type, s.sessionStartedAt, endedAt, session.durationSeconds, actualSeconds, completed)

        val countsAsWork = completed && session.type == SessionType.WORK
        val workSessions = s.completedWorkSessions + if (countsAsWork) 1 else 0
        val workSeconds = s.completedWorkSeconds + if (countsAsWork) session.durationSeconds else 0

        val next = s.next
        if (next == null) {
            disarmDeadline()
            publish(TimerState.Finished(workSessions, workSeconds))
        } else {
            publish(
                s.copy(
                    index = s.index + 1,
                    isPaused = false,
                    endsAtElapsed = atElapsed + next.durationSeconds * 1000L,
                    remainingWhenPausedMillis = 0,
                    sessionStartedAt = endedAt,
                    completedWorkSessions = workSessions,
                    completedWorkSeconds = workSeconds,
                    skippedIndices = if (completed) s.skippedIndices else s.skippedIndices + s.index,
                )
            )
            if (!late) armDeadline()
        }
        _events.emit(TimerEvent.SessionEnded(session, s.sessionStartedAt, completed, next, late))
    }

    /** Cambia el estado y lo guarda; el resumen final no se guarda (solo importa el plan en curso). */
    private suspend fun publish(state: TimerState) {
        _state.value = state
        store.save((state as? TimerState.Active)?.let(::snapshotOf))
    }

    private fun snapshotOf(state: TimerState.Active) =
        TimerSnapshot(state, savedAtWallMillis = time.now().toEpochMilli(), savedAtElapsed = time.elapsedRealtime())

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

    private companion object {
        /** Un plan guardado hace más de un día ya no se recupera. */
        const val MAX_RESTORE_AGE_MILLIS = 24 * 60 * 60 * 1000L

        /** Diferencia entre relojes que todavía cuenta como el mismo arranque (ajustes de hora). */
        const val SAME_BOOT_TOLERANCE_MILLIS = 60_000L
    }
}
