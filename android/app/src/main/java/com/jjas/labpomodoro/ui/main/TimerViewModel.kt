package com.jjas.labpomodoro.ui.main

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.FocusSound
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.service.label
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.ElementBehavior
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.PlanDot
import com.jjas.labpomodoro.ui.components.ShelfItemUi
import com.jjas.labpomodoro.ui.components.UpNext
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.look
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

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
        /** Pomodoros completados en este plan; cuando sube, hay confeti. */
        val completedWork: Int = 0,
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
    /** Elemento de tu colección que tiñe el líquido y define su comportamiento (solo en trabajo). */
    val element: Element? = null,
) {
    val behavior: ElementBehavior? get() = element?.look()?.behavior

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
    /** Pro con el sonido de lluvia elegido: en modo ambiente se ve llover detrás del recipiente. */
    val rainAmbience: Boolean = false,
    /** Si no es null, tras este tiempo sin tocar la pantalla se entra al modo ambiente. */
    val ambientDelayMillis: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val engine: TimerEngine,
    private val settingsRepository: SettingsRepository,
    private val time: TimeSource,
    inventory: InventoryRepository,
) : ViewModel() {

    private val discovered = inventory.items.map { items -> items.filter { it.firstObtainedAtMillis != null }.map { it.element } }

    // Los elementos se reparten al empezar el plan y no cambian mientras dura, aunque ganes nuevos
    private var reagentPool: Pair<Long, List<Element>>? = null

    private fun poolFor(planSeed: Long, current: List<Element>): List<Element> {
        reagentPool?.takeIf { it.first == planSeed }?.let { return it.second }
        return current.also { reagentPool = planSeed to it }
    }

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

    val state: StateFlow<TimerScreenState> = combine(ticking, settingsRepository.settings, discovered) { timer, settings, owned ->
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
                val pool = poolFor(timer.planSeed, owned)
                val favorite = pool.firstOrNull { it.atomicNumber == settings.vesselElement }
                // El favorito si lo elegiste; si no, uno distinto de tu colección en cada sesión
                fun reagent(i: Int): Element? = when {
                    timer.plan[i].type != SessionType.WORK -> null
                    favorite != null -> favorite
                    pool.isEmpty() -> null
                    else -> pool[Random(timer.planSeed * 17 + i).nextInt(pool.size)]
                }
                fun liquid(i: Int): Color =
                    reagent(i)?.look()?.color ?: LiquidPalette.liquid(timer.plan[i].type, timer.planSeed, i)
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
                        liquid = liquid(timer.index),
                        bubbles = LiquidPalette.bubble(type, timer.planSeed, timer.index),
                        effect = if (type == SessionType.WORK) LiquidEffect.VAPOR else LiquidEffect.BUBBLES,
                        animate = !timer.isPaused,
                        element = reagent(timer.index),
                    ),
                    upNext = timer.next?.let {
                        UpNext(it.type, liquid(timer.index + 1), it.durationSeconds / 60)
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
                                color = liquid(i),
                                cycleEnd = timer.plan.getOrNull(i + 1)?.type == SessionType.LONG_BREAK,
                            )
                        },
                    completedWork = timer.completedWorkSessions,
                    shelf = timer.plan.mapIndexed { i, session ->
                        ShelfItemUi(
                            shape = LiquidPalette.vessel(timer.planSeed, i),
                            color = liquid(i),
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
            rainAmbience = settings.isPro && settings.focusSound == FocusSound.RAIN,
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
