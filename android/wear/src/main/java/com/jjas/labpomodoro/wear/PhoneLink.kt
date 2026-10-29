package com.jjas.labpomodoro.wear

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** Estado del timer del teléfono, como lo publica `WearSync`. */
data class WatchTimer(
    val state: String,
    val type: String = "WORK",
    val endsAt: Long = 0,
    val paused: Boolean = false,
    val remaining: Long = 0,
    val duration: Long = 1,
    val pomodoro: Int = 0,
    val pomodoros: Int = 0,
    val color: Int = 0xFF39FF14.toInt(),
    val shape: String = "BEAKER",
    val isPro: Boolean = false,
    val inRoom: Boolean = false,
    val isHost: Boolean = false,
    /** El plan de siempre del teléfono (tipo y segundos), para correrlo sin él. */
    val planTypes: List<String> = emptyList(),
    val planSeconds: List<Int> = emptyList(),
    /** Es el plan que corre el reloj solo (modo semiautónomo). */
    val local: Boolean = false,
) {
    val active: Boolean get() = state == "ACTIVE"

    /** Lo que falta ahora: en pausa, lo congelado; si no, hasta la hora de fin. */
    fun remainingAt(nowMillis: Long): Long = if (paused) remaining else (endsAt - nowMillis).coerceAtLeast(0)

    /** En una sala solo el anfitrión pausa y nadie salta. */
    val canPause: Boolean get() = local || !inRoom || isHost
    val canSkip: Boolean get() = isPro && !inRoom && !local
}

/** Lee el estado que publica el teléfono y le manda órdenes. */
class PhoneLink(context: Context) {
    private val data = Wearable.getDataClient(context)
    private val messages = Wearable.getMessageClient(context)
    private val nodes = Wearable.getNodeClient(context)

    private val _timer = MutableStateFlow<WatchTimer?>(null)

    /** null = todavía no llega nada del teléfono. */
    val timer: StateFlow<WatchTimer?> = _timer.asStateFlow()

    private val listener = DataClient.OnDataChangedListener { events ->
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearContract.TIMER_PATH }
            .forEach { _timer.value = DataMapItem.fromDataItem(it.dataItem).dataMap.toWatchTimer() }
    }

    suspend fun start() {
        data.addListener(listener)
        // Lo último que publicó el teléfono, aunque haya sido antes de abrir la app
        runCatching {
            val items = data.getDataItems(Uri.Builder().scheme("wear").path(WearContract.TIMER_PATH).build()).await()
            items.forEach { _timer.value = DataMapItem.fromDataItem(it).dataMap.toWatchTimer() }
            items.release()
        }
    }

    fun stop() {
        data.removeListener(listener)
    }

    /** Igual que [send], sin esperar (para el tile, que no es una corrutina). */
    fun sendAsync(command: String) {
        nodes.connectedNodes.addOnSuccessListener { connected ->
            connected.filter { it.isNearby }.forEach { messages.sendMessage(it.id, WearContract.COMMAND_PREFIX + command, null) }
        }
    }

    /** Manda [command] a los teléfonos conectados. Devuelve false si no hay ninguno cerca. */
    suspend fun send(command: String): Boolean = runCatching {
        // Solo el teléfono cerca: Wear OS también cuenta "la nube" como conectada
        withTimeoutOrNull(SEND_TIMEOUT_MILLIS) {
            val nearby = nodes.connectedNodes.await().filter { it.isNearby }
            nearby.forEach { messages.sendMessage(it.id, WearContract.COMMAND_PREFIX + command, null).await() }
            nearby.isNotEmpty()
        } ?: false
    }.getOrDefault(false)

    private companion object {
        const val SEND_TIMEOUT_MILLIS = 5_000L
    }
}

/** Lee el DataItem que publica `WearSync` en el teléfono. */
fun DataMap.toWatchTimer() = WatchTimer(
    state = getString(WearContract.KEY_STATE, "IDLE"),
    type = getString(WearContract.KEY_TYPE, "WORK"),
    endsAt = getLong(WearContract.KEY_ENDS_AT),
    paused = getBoolean(WearContract.KEY_PAUSED),
    remaining = getLong(WearContract.KEY_REMAINING),
    duration = getLong(WearContract.KEY_DURATION).coerceAtLeast(1),
    pomodoro = getInt(WearContract.KEY_POMODORO),
    pomodoros = getInt(WearContract.KEY_POMODOROS),
    color = getInt(WearContract.KEY_COLOR, 0xFF39FF14.toInt()),
    shape = getString(WearContract.KEY_SHAPE, "BEAKER"),
    isPro = getBoolean(WearContract.KEY_PRO),
    inRoom = getBoolean(WearContract.KEY_ROOM),
    isHost = getBoolean(WearContract.KEY_HOST),
    planTypes = getStringArrayList(WearContract.KEY_PLAN_TYPES).orEmpty(),
    planSeconds = getIntegerArrayList(WearContract.KEY_PLAN_SECONDS).orEmpty(),
)
