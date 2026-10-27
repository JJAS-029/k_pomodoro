package com.jjas.labpomodoro.timer

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.domain.model.PlannedSession
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

    /** Sala de estudio en curso; mientras haya una, empezar, pausar y detener pasan por ella. */
    @Volatile
    var roomLink: RoomLink? = null

    /** Recupera el plan guardado (una sola vez por proceso). Lo llaman la app, el servicio y los receptores. */
    suspend fun ensureRestored() = mutex.withLock { restoreLocked() }

    suspend fun start(config: SessionConfig) {
        if (roomLink?.onStart() == true) return
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

    suspend fun pause() {
        if (roomLink?.onPause() == true) return
        pauseLocal()
    }

    private suspend fun pauseLocal() = mutex.withLock {
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.isPaused) return@withLock
        disarmDeadline()
        publish(s.copy(isPaused = true, remainingWhenPausedMillis = s.remainingMillis(time.elapsedRealtime())))
    }

    suspend fun resume() {
        if (roomLink?.onResume() == true) return
        resumeLocal()
    }

    private suspend fun resumeLocal() = mutex.withLock {
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (!s.isPaused) return@withLock
        publish(s.copy(isPaused = false, endsAtElapsed = time.elapsedRealtime() + s.remainingWhenPausedMillis))
        armDeadline()
    }

    /** Saltar ≠ completar: se guarda como no completada y no da racha ni recompensas. */
    suspend fun skip() = mutex.withLock {
        // En una sala todos van al mismo paso: no se salta
        if (roomLink != null) return@withLock
        restoreLocked()
        if (_state.value is TimerState.Active) finishCurrent(completed = false)
    }

    /** Abandona el plan sin guardar la sesión en curso (y, si hay sala, sale de ella). */
    suspend fun reset() {
        roomLink?.onStop()
        resetLocal()
    }

    private suspend fun resetLocal() = mutex.withLock {
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

    /** Suma [minutes] a la sesión actual (hasta [MAX_SESSION_SECONDS]), corra o esté en pausa. */
    suspend fun extendCurrent(minutes: Int = 5) = mutex.withLock {
        // El plan de una sala es el mismo para todos: no se ajusta por separado
        if (roomLink != null) return@withLock
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        val extra = minutes * 60
        if (s.current.durationSeconds + extra > MAX_SESSION_SECONDS) return@withLock
        val plan = s.plan.toMutableList().also { it[s.index] = s.current.copy(durationSeconds = s.current.durationSeconds + extra) }
        val extended = if (s.isPaused) {
            s.copy(plan = plan, remainingWhenPausedMillis = s.remainingWhenPausedMillis + extra * 1000L)
        } else {
            s.copy(plan = plan, endsAtElapsed = s.endsAtElapsed + extra * 1000L)
        }
        publish(extended)
        if (!extended.isPaused) armDeadline()
    }

    /** Agrega un pomodoro al final del plan, con un descanso corto antes (como los demás). */
    suspend fun addPomodoro() = mutex.withLock {
        // El plan de una sala es el mismo para todos: no se ajusta por separado
        if (roomLink != null) return@withLock
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.plan.count { it.type == SessionType.WORK } >= MAX_POMODOROS) return@withLock
        val work = s.plan.lastOrNull { it.type == SessionType.WORK }?.durationSeconds ?: return@withLock
        val rest = s.plan.firstOrNull { it.type == SessionType.SHORT_BREAK }?.durationSeconds ?: DEFAULT_BREAK_SECONDS
        val added = if (s.plan.last().type == SessionType.WORK) {
            listOf(PlannedSession(SessionType.SHORT_BREAK, rest), PlannedSession(SessionType.WORK, work))
        } else {
            listOf(PlannedSession(SessionType.WORK, work))
        }
        publish(s.copy(plan = s.plan + added))
    }

    /** Quita el último pomodoro que falta (y el descanso que lo precedía). */
    suspend fun removeLastPomodoro() = mutex.withLock {
        // El plan de una sala es el mismo para todos: no se ajusta por separado
        if (roomLink != null) return@withLock
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        val last = s.plan.indexOfLast { it.type == SessionType.WORK }
        if (last <= s.index) return@withLock
        val plan = s.plan.toMutableList()
        plan.removeAt(last)
        // Sin pomodoro después, los descansos del final ya no tienen sentido
        while (plan.size - 1 > s.index && plan.last().type != SessionType.WORK) plan.removeAt(plan.lastIndex)
        publish(s.copy(plan = plan, skippedIndices = s.skippedIndices.filter { it < plan.size }.toSet()))
    }

    /** El plan termina al acabar la sesión actual. */
    suspend fun finishAfterCurrent() = mutex.withLock {
        // El plan de una sala es el mismo para todos: no se ajusta por separado
        if (roomLink != null) return@withLock
        restoreLocked()
        val s = _state.value as? TimerState.Active ?: return@withLock
        if (s.next == null) return@withLock
        publish(s.copy(plan = s.plan.take(s.index + 1), skippedIndices = s.skippedIndices.filter { it <= s.index }.toSet()))
    }

    /**
     * Sigue el plan de una sala: [config] y [planSeed] son los de la sala (todos ven los mismos
     * recipientes y colores) y [elapsedMillis] lo que lleva corriendo sin contar pausas. Se coloca
     * en la sesión que toca; si ya iba ahí, solo corrige la pausa y la diferencia de reloj. Las
     * sesiones que terminan solas se registran como siempre; las de antes de entrar, no.
     */
    suspend fun follow(config: SessionConfig, planSeed: Long, elapsedMillis: Long, paused: Boolean) {
        var started = false
        mutex.withLock {
            restoreLocked()
            val plan = SessionPlanGenerator.generate(config)
            var index = -1
            var offset = 0L
            var before = 0L
            for ((i, session) in plan.withIndex()) {
                val length = session.durationSeconds * 1000L
                if (elapsedMillis < before + length) {
                    index = i
                    offset = (elapsedMillis - before).coerceAtLeast(0)
                    break
                }
                before += length
            }
            // El plan de la sala ya terminó: el propio se cierra solo con su alarma
            if (index < 0) return@withLock
            val s = _state.value
            val nowElapsed = time.elapsedRealtime()
            val remaining = plan[index].durationSeconds * 1000L - offset
            val same = s is TimerState.Active && s.planSeed == planSeed
            if (same && s.index == index) {
                val drift = abs(s.remainingMillis(nowElapsed) - remaining)
                if (s.isPaused == paused && drift <= ROOM_DRIFT_MILLIS) return@withLock
                if (paused) {
                    disarmDeadline()
                    publish(s.copy(isPaused = true, remainingWhenPausedMillis = remaining))
                } else {
                    publish(s.copy(isPaused = false, endsAtElapsed = nowElapsed + remaining))
                    armDeadline()
                }
                return@withLock
            }
            // Está por cerrar la sesión anterior por su cuenta: que lo haga (y la registre)
            if (same && s.index == index - 1 && !s.isPaused && s.remainingMillis(nowElapsed) <= ROOM_DRIFT_MILLIS) return@withLock
            started = s !is TimerState.Active
            publish(
                TimerState.Active(
                    plan = plan,
                    index = index,
                    isPaused = paused,
                    endsAtElapsed = nowElapsed + remaining,
                    remainingWhenPausedMillis = if (paused) remaining else 0,
                    sessionStartedAt = time.now().minusMillis(offset),
                    completedWorkSessions = (s as? TimerState.Active)?.takeIf { same }?.completedWorkSessions ?: 0,
                    completedWorkSeconds = (s as? TimerState.Active)?.takeIf { same }?.completedWorkSeconds ?: 0,
                    planSeed = planSeed,
                    // Lo que corrió antes de entrar queda vacío en la repisa
                    skippedIndices = if (same) (s as TimerState.Active).skippedIndices else (0 until index).toSet(),
                )
            )
            if (paused) disarmDeadline() else armDeadline()
        }
        if (started) {
            serviceLauncher.start()
            _events.emit(TimerEvent.PlanStarted)
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

    companion object {
        /** Un plan guardado hace más de un día ya no se recupera. */
        private const val MAX_RESTORE_AGE_MILLIS = 24 * 60 * 60 * 1000L

        /** Diferencia entre relojes que todavía cuenta como el mismo arranque (ajustes de hora). */
        private const val SAME_BOOT_TOLERANCE_MILLIS = 60_000L

        /** Una sesión no se alarga más de hora y media. */
        const val MAX_SESSION_SECONDS = 90 * 60

        private const val MAX_POMODOROS = 24
        private const val DEFAULT_BREAK_SECONDS = 5 * 60

        /** Diferencia tolerada con el plan de la sala antes de corregir (relojes de distintos teléfonos). */
        private const val ROOM_DRIFT_MILLIS = 2_000L
    }
}
