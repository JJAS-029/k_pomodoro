package com.jjas.labpomodoro.ui.main

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.service.label
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.PlanDot
import com.jjas.labpomodoro.ui.components.ShelfItemUi
import com.jjas.labpomodoro.ui.components.UpNext
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.theme.NeonGreen
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
        val vessel: VesselUi,
        val shelf: List<ShelfItemUi>,
        val upNext: UpNext?,
        val dots: List<PlanDot>,
    ) : TimerUi {
        /** Para lectores de pantalla: lo que el indicador simbólico dice con íconos. */
        val description: String
            get() = "Sesión $sessionNumber de $totalSessions. " +
                (next?.let { "Después: ${it.label().lowercase()}" } ?: "Última sesión")
    }

    data class Finished(val workSessions: Int, val workMinutes: Long) : TimerUi
}

/** Lo que dibuja el recipiente: en trabajo el líquido se evapora, en descanso se llena (como el prototipo). */
data class VesselUi(
    val shape: VesselShape,
    val fill: Float,
    val liquid: Color,
    val bubbles: Color,
    val effect: LiquidEffect,
    val animate: Boolean,
) {
    companion object {
        /** Vaso con líquido y quieto, antes de empezar o al terminar. */
        val Resting = VesselUi(VesselShape.BEAKER, 0.8f, NeonGreen.copy(alpha = 0.8f), Color.Transparent, LiquidEffect.NONE, animate = false)
    }
}

data class TimerScreenState(
    val timer: TimerUi? = null,
    val keepScreenOn: Boolean = false,
    val isPro: Boolean = false,
    /** Con Material You activo el reloj toma el color del sistema. */
    val dynamicColor: Boolean = false,
    /** null mientras carga; false = hay que mostrar la guía la primera vez. */
    val guideSeen: Boolean? = null,
    /** Si no es null, tras este tiempo sin tocar la pantalla se entra al modo ambiente. */
    val ambientDelayMillis: Long? = null,
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
                val progress = (1f - remaining / total).coerceIn(0f, 1f)
                val type = timer.current.type
                TimerUi.Active(
                    type = timer.current.type,
                    remainingMillis = remaining,
                    progress = progress,
                    sessionNumber = timer.index + 1,
                    totalSessions = timer.plan.size,
                    isPaused = timer.isPaused,
                    next = timer.next?.type,
                    vessel = VesselUi(
                        shape = LiquidPalette.vessel(timer.planSeed, timer.index),
                        fill = if (type == SessionType.WORK) 1f - progress else progress,
                        liquid = LiquidPalette.liquid(type, timer.planSeed, timer.index),
                        bubbles = LiquidPalette.bubble(type, timer.planSeed, timer.index),
                        effect = if (type == SessionType.WORK) LiquidEffect.VAPOR else LiquidEffect.BUBBLES,
                        animate = !timer.isPaused,
                    ),
                    upNext = timer.next?.let {
                        UpNext(it.type, LiquidPalette.liquid(it.type, timer.planSeed, timer.index + 1), it.durationSeconds / 60)
                    },
                    dots = timer.plan.withIndex()
                        .filter { it.value.type == SessionType.WORK }
                        .map { (i, _) ->
                            PlanDot(
                                state = when {
                                    i in timer.skippedIndices -> DotState.SKIPPED
                                    i < timer.index -> DotState.DONE
                                    i == timer.index -> DotState.CURRENT
                                    else -> DotState.PENDING
                                },
                                color = LiquidPalette.liquid(SessionType.WORK, timer.planSeed, i),
                                cycleEnd = timer.plan.getOrNull(i + 1)?.type == SessionType.LONG_BREAK,
                            )
                        },
                    shelf = timer.plan.mapIndexed { i, session ->
                        ShelfItemUi(
                            shape = LiquidPalette.vessel(timer.planSeed, i),
                            color = LiquidPalette.liquid(session.type, timer.planSeed, i),
                            fill = when {
                                i < timer.index -> 1f
                                i == timer.index -> progress
                                else -> 0f
                            },
                            isCurrent = i == timer.index,
                            skipped = i in timer.skippedIndices,
                        )
                    },
                )
            }
            is TimerState.Finished -> TimerUi.Finished(timer.completedWorkSessions, timer.completedWorkSeconds / 60)
        }
        val running = ui is TimerUi.Active && !ui.isPaused
        TimerScreenState(
            timer = ui,
            // El modo ambiente necesita la pantalla encendida para poder mostrar el reloj
            keepScreenOn = running && (settings.keepScreenOn || settings.ambientActive),
            isPro = settings.isPro,
            dynamicColor = settings.dynamicColorActive,
            guideSeen = settings.guideSeen,
            ambientDelayMillis = if (running && settings.ambientActive) settings.ambientDelayMinutes * 60_000L else null,
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
