package com.jjas.labpomodoro.wear

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.data.room.StudyRoomRepository
import com.jjas.labpomodoro.domain.model.LabPalette
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimeSource
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.LiquidPalette
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
    private val time: TimeSource,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private data class Snapshot(
        val timer: TimerState,
        val isPro: Boolean,
        val owned: Set<String>,
        val palette: LabPalette,
        val inRoom: Boolean,
        val isHost: Boolean,
    )

    fun start() {
        scope.launch {
            combine(engine.state, settings.settings, rooms.state) { timer, prefs, room ->
                Snapshot(timer, prefs.isPro, prefs.ownedGlassware, prefs.paletteOn(LocalDate.now()), room != null, room?.isHost == true)
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
                val color = LiquidPalette.liquid(timer.current.type, timer.planSeed, timer.index, s.palette)
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
