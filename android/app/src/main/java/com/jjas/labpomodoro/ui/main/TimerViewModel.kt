package com.jjas.labpomodoro.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TimerUi {
    /** Vista previa del plan que se generaría con la configuración actual. */
    data class Idle(
        val firstSessionMillis: Long,
        val pomodoros: Int,
        val workMinutes: Int,
        val totalMinutes: Int,
    ) : TimerUi

    data class Active(
        val type: SessionType,
        val remainingMillis: Long,
        val progress: Float,
        val sessionNumber: Int,
        val totalSessions: Int,
        val isPaused: Boolean,
        val next: SessionType?,
    ) : TimerUi

    data class Finished(val workSessions: Int, val workMinutes: Long) : TimerUi
}

data class TimerScreenState(
    val timer: TimerUi? = null,
    val keepScreenOn: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val engine: TimerEngine,
    private val settingsRepository: SettingsRepository,
    private val time: TimeSource,
) : ViewModel() {

    // Solo hace falta "tic" en pantalla mientras corre; el motor no depende de esto
    private val ticking = engine.state.flatMapLatest { state ->
        if (state is TimerState.Active && !state.isPaused) {
            flow {
                while (true) {
                    emit(state)
                    delay(TICK_MILLIS)
                }
            }
        } else {
            flowOf(state)
        }
    }

    val state: StateFlow<TimerScreenState> = combine(ticking, settingsRepository.settings) { timer, settings ->
        val ui = when (timer) {
            TimerState.Idle -> {
                val plan = SessionPlanGenerator.generate(settings.session)
                TimerUi.Idle(
                    firstSessionMillis = plan.first().durationSeconds * 1000L,
                    pomodoros = plan.count { it.type == SessionType.WORK },
                    workMinutes = plan.filter { it.type == SessionType.WORK }.sumOf { it.durationSeconds } / 60,
                    totalMinutes = plan.sumOf { it.durationSeconds } / 60,
                )
            }
            is TimerState.Active -> {
                val remaining = timer.remainingMillis(time.elapsedRealtime())
                val total = timer.current.durationSeconds * 1000f
                TimerUi.Active(
                    type = timer.current.type,
                    remainingMillis = remaining,
                    progress = (1f - remaining / total).coerceIn(0f, 1f),
                    sessionNumber = timer.index + 1,
                    totalSessions = timer.plan.size,
                    isPaused = timer.isPaused,
                    next = timer.next?.type,
                )
            }
            is TimerState.Finished -> TimerUi.Finished(timer.completedWorkSessions, timer.completedWorkSeconds / 60)
        }
        TimerScreenState(
            timer = ui,
            keepScreenOn = settings.keepScreenOn && ui is TimerUi.Active && !ui.isPaused,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerScreenState())

    fun start() {
        viewModelScope.launch { engine.start(settingsRepository.settings.first().session) }
    }

    fun pause() {
        viewModelScope.launch { engine.pause() }
    }

    fun resume() {
        viewModelScope.launch { engine.resume() }
    }

    fun skip() {
        viewModelScope.launch { engine.skip() }
    }

    fun stop() {
        viewModelScope.launch { engine.reset() }
    }

    fun dismissSummary() {
        viewModelScope.launch { engine.dismissFinished() }
    }

    private companion object {
        const val TICK_MILLIS = 200L
    }
}
