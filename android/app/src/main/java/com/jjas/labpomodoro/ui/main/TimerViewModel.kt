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
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.ElementBehavior
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.ElementSignatures
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.LiquidTones
import com.jjas.labpomodoro.ui.components.PlanDot
import com.jjas.labpomodoro.ui.components.ShelfItemUi
import com.jjas.labpomodoro.ui.components.UpNext
import com.jjas.labpomodoro.ui.components.VesselReagents
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
import com.jjas.labpomodoro.domain.model.SessionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.LabRepository
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
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
        /** Con lo que arrancará el próximo plan. */
        val config: SessionConfig = SessionConfig(),
        /** true si se ajustó solo para este plan (distinto de lo de Configuración). */
        val customized: Boolean = false,
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
        /** Para ajustar el plan: pomodoros que faltan después de la sesión actual. */
        val pomodorosLeft: Int = 0,
        /** Lo que falta de todo el plan (esta sesión y las siguientes). */
        val planRemainingMillis: Long = 0,
        /** Se le pueden sumar 5 min a esta sesión (no pasa del tope). */
        val canExtend: Boolean = true,
    ) : TimerUi

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
    /** Elementos que gana este pomodoro al completarse: en el último tramo el líquido muta hacia ellos. */
    val morphTo: List<Element> = emptyList(),
    /** Cuánto ha mutado (0..1). */
    val morph: Float = 0f,
) {
    val behavior: ElementBehavior? get() = element?.look()?.behavior

    /** Tonos de firma si el elemento tiene una (oro, mercurio…); si no, se calculan del color. */
    val tones: LiquidTones? get() = element?.let { ElementSignatures.tones(it.atomicNumber) }

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
    /** null mientras carga; false = aún no se pide permitir las alarmas exactas. */
    val exactAlarmAsked: Boolean? = null,
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
    private val lab: LabRepository,
    private val sessions: SessionRepository,
) : ViewModel() {

    private val inventoryItems = inventory.items

    /** Ajuste del próximo plan hecho desde la pantalla de inicio; null = lo de Configuración. */
    private val planOverride = MutableStateFlow<SessionConfig?>(null)

    /**
     * Premios del pomodoro en curso, reservados al empezarlo (clave = plan, sesión y duración, por si
     * se alarga). Así el líquido muta hacia el elemento que de verdad se va a ganar.
     */
    private val prizes: StateFlow<Pair<String, List<Element>>?> = engine.state
        .map { state ->
            (state as? TimerState.Active)?.takeIf { it.current.type == SessionType.WORK }?.let {
                "${it.planSeed}_${it.index}_${it.current.durationSeconds}" to it.current.durationSeconds.toLong()
            }
        }
        .distinctUntilChanged()
        .mapLatest { key ->
            key?.let { (id, seconds) ->
                id to runCatching { lab.reservePrizes(sessions.totalWorkSeconds.first(), seconds) }.getOrDefault(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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

    val state: StateFlow<TimerScreenState> = combine(ticking, settingsRepository.settings, inventoryItems, prizes, planOverride) { timer, settings, items, prizes, override ->
        val ui = when (timer) {
            TimerState.Idle -> {
                val config = override ?: settings.session
                val plan = SessionPlanGenerator.generate(config)
                TimerUi.Idle(
                    config = config,
                    customized = override != null && override != settings.session,
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
                // La semilla del plan es su hora de inicio: se usan los elementos que ya se tenían
                val pool = VesselReagents.pool(items, timer.planSeed)
                fun reagent(i: Int): Element? =
                    VesselReagents.reagent(timer.plan, i, timer.planSeed, pool, settings.vesselElement)
                fun liquid(i: Int): Color =
                    reagent(i)?.look()?.color ?: LiquidPalette.liquid(timer.plan[i].type, timer.planSeed, i, settings.activePalette)
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
                        bubbles = LiquidPalette.bubble(type, timer.planSeed, timer.index, settings.activePalette),
                        effect = if (type == SessionType.WORK) LiquidEffect.VAPOR else LiquidEffect.BUBBLES,
                        animate = !timer.isPaused,
                        element = reagent(timer.index),
                        morphTo = prizes?.takeIf { it.first == "${timer.planSeed}_${timer.index}_${timer.current.durationSeconds}" }?.second.orEmpty(),
                        // En el último tramo del pomodoro (solo trabajo)
                        morph = if (type == SessionType.WORK) ((progress - MORPH_START) / (1f - MORPH_START)).coerceIn(0f, 1f) else 0f,
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
                    pomodorosLeft = timer.plan.drop(timer.index + 1).count { it.type == SessionType.WORK },
                    planRemainingMillis = remaining + timer.plan.drop(timer.index + 1).sumOf { it.durationSeconds } * 1000L,
                    canExtend = timer.current.durationSeconds + 5 * 60 <= TimerEngine.MAX_SESSION_SECONDS,
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
                            tones = reagent(i)?.let { ElementSignatures.tones(it.atomicNumber) },
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
            exactAlarmAsked = settings.exactAlarmAsked,
            rainAmbience = settings.isPro && settings.focusSound == FocusSound.RAIN,
            ambientDelayMillis = if (running && settings.ambientActive) settings.ambientDelayMinutes * 60_000L else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerScreenState())

    fun markExactAlarmAsked() {
        viewModelScope.launch { settingsRepository.setExactAlarmAsked() }
    }

    fun start() {
        viewModelScope.launch {
            engine.start(planOverride.value ?: settingsRepository.settings.first().session)
            // El ajuste era solo para este plan
            planOverride.value = null
        }
    }

    /** Ajusta solo el próximo plan, sin tocar Configuración. */
    fun editPlan(transform: (SessionConfig) -> SessionConfig) {
        viewModelScope.launch {
            val base = planOverride.value ?: settingsRepository.settings.first().session
            planOverride.value = transform(base).normalized()
        }
    }

    /** Vuelve a lo de Configuración. */
    fun resetPlan() {
        planOverride.value = null
    }

    /** Guarda el ajuste como el plan de siempre. */
    fun savePlanAsDefault() {
        val config = planOverride.value ?: return
        viewModelScope.launch {
            settingsRepository.updateSession { config }
            planOverride.value = null
        }
    }

    fun extend() {
        viewModelScope.launch { engine.extendCurrent(5) }
    }

    fun addPomodoro() {
        viewModelScope.launch { engine.addPomodoro() }
    }

    fun removeLastPomodoro() {
        viewModelScope.launch { engine.removeLastPomodoro() }
    }

    fun finishAfterCurrent() {
        viewModelScope.launch { engine.finishAfterCurrent() }
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

/** Desde qué avance del pomodoro el líquido empieza a mutar hacia el premio (último 20 %). */
private const val MORPH_START = 0.8f
