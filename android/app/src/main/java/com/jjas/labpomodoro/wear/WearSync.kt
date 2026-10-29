package com.jjas.labpomodoro.wear

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.data.repository.InventoryItem
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.room.StudyRoomRepository
import com.jjas.labpomodoro.domain.model.LabPalette
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.VesselReagents
import com.jjas.labpomodoro.ui.components.look
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publica el estado del timer para el reloj, solo cuando cambia (empezar, pausar, cambiar de
 * sesión): el reloj cuenta los segundos por su cuenta con la hora de fin.
 */
@Singleton
class WearSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val rooms: StudyRoomRepository,
    private val inventory: InventoryRepository,
    private val time: TimeSource,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private data class Snapshot(
        val timer: TimerState,
        val isPro: Boolean,
        val owned: Set<String>,
        val palette: LabPalette,
        val favorite: Int,
        val items: List<InventoryItem>,
        val usual: SessionConfig,
        val inRoom: Boolean,
        val isHost: Boolean,
    )

    fun start() {
        scope.launch {
            combine(engine.state, settings.settings, rooms.state, inventory.items) { timer, prefs, room, items ->
                Snapshot(timer, prefs.isPro, prefs.ownedGlassware, prefs.paletteOn(LocalDate.now()), prefs.vesselElement, items, prefs.session, room != null, room?.isHost == true)
            }.distinctUntilChanged().collect { snapshot ->
                runCatching { publish(snapshot) }
            }
        }
    }

    private suspend fun publish(s: Snapshot) {
        val request = PutDataMapRequest.create(WearContract.TIMER_PATH)
        val map = request.dataMap
        map.putBoolean(WearContract.KEY_PRO, s.isPro)
        map.putBoolean(WearContract.KEY_ROOM, s.inRoom)
        map.putBoolean(WearContract.KEY_HOST, s.isHost)
        // El plan de siempre, para que el reloj lo corra solo si el teléfono no está cerca
        val plan = SessionPlanGenerator.generate(s.usual)
        map.putStringArrayList(WearContract.KEY_PLAN_TYPES, ArrayList(plan.map { it.type.name }))
        map.putIntegerArrayList(WearContract.KEY_PLAN_SECONDS, ArrayList(plan.map { it.durationSeconds }))
        when (val timer = s.timer) {
            is TimerState.Active -> {
                val remaining = timer.remainingMillis(time.elapsedRealtime())
                map.putString(WearContract.KEY_STATE, "ACTIVE")
                map.putString(WearContract.KEY_TYPE, timer.current.type.name)
                map.putLong(WearContract.KEY_ENDS_AT, System.currentTimeMillis() + remaining)
                map.putBoolean(WearContract.KEY_PAUSED, timer.isPaused)
                map.putLong(WearContract.KEY_REMAINING, remaining)
                map.putLong(WearContract.KEY_DURATION, timer.current.durationSeconds * 1000L)
                val works = timer.plan.withIndex().filter { it.value.type == SessionType.WORK }
                map.putInt(WearContract.KEY_POMODOROS, works.size)
                map.putInt(WearContract.KEY_POMODORO, works.count { it.index <= timer.index }.coerceAtLeast(1))
                // El mismo color que en el teléfono: el del elemento que tiñe el trabajo, o el de la paleta
                val pool = VesselReagents.pool(s.items, timer.planSeed)
                val element = VesselReagents.reagent(timer.plan, timer.index, timer.planSeed, pool, s.favorite)
                val color = element?.look()?.color ?: LiquidPalette.liquid(timer.current.type, timer.planSeed, timer.index, s.palette)
                map.putInt(WearContract.KEY_COLOR, color.toArgb())
                map.putString(WearContract.KEY_SHAPE, LiquidPalette.vessel(timer.planSeed, timer.index, s.owned).name)
            }
            is TimerState.Finished -> map.putString(WearContract.KEY_STATE, "FINISHED")
            else -> map.putString(WearContract.KEY_STATE, "IDLE")
        }
        // Urgente: si no, Wear OS puede tardar hasta media hora en entregarlo
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent()).await()
    }
}
