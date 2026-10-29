package com.jjas.labpomodoro.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.time.Instant
import javax.inject.Inject

/**
 * Órdenes del reloj (empezar, pausar, continuar, saltar). Pasan por [TimerEngine], igual que los
 * botones de la notificación, así que respetan las salas de estudio. Saltar es Pro.
 *
 * También recibe los pomodoros que el reloj completó sin el teléfono: los registra (una sola vez
 * cada uno) y [com.jjas.labpomodoro.service.RewardSync] entrega los elementos que tocan.
 */
@AndroidEntryPoint
class WearCommandService : WearableListenerService() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var sessions: SessionRepository

    // Wear OS lo llama en un hilo aparte y espera a que termine
    override fun onMessageReceived(event: MessageEvent) = runBlocking {
        if (!event.path.startsWith(WearContract.COMMAND_PREFIX)) return@runBlocking
        engine.ensureRestored()
        when (event.path.removePrefix(WearContract.COMMAND_PREFIX)) {
            WearContract.START -> engine.start(settings.settings.first().session)
            WearContract.PAUSE -> engine.pause()
            WearContract.RESUME -> engine.resume()
            WearContract.SKIP -> if (settings.settings.first().isPro) engine.skip()
        }
    }

    override fun onDataChanged(events: DataEventBuffer) = runBlocking {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearContract.OFFLINE_PATH }.forEach { event ->
            val raw = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(WearContract.KEY_SESSIONS) ?: return@forEach
            val ids = recordOffline(raw)
            // Le confirma al reloj, que entonces las borra de su lista
            if (ids.isNotEmpty()) {
                runCatching {
                    Wearable.getMessageClient(this@WearCommandService)
                        .sendMessage(event.dataItem.uri.host.orEmpty(), WearContract.ACK_PATH, ids.joinToString(",").toByteArray())
                        .await()
                }
            }
        }
    }

    /** Registra las sesiones del reloj que no se habían registrado. Devuelve los ids recibidos. */
    private suspend fun recordOffline(raw: String): List<String> {
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val done = prefs.getStringSet(KEY_DONE, emptySet()).orEmpty().toMutableSet()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val ids = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            val id = row.getString("id")
            ids += id
            if (id in done) continue
            val type = runCatching { SessionType.valueOf(row.getString("type")) }.getOrNull() ?: continue
            val seconds = row.getInt("seconds")
            sessions.record(type, Instant.ofEpochMilli(row.getLong("start")), Instant.ofEpochMilli(row.getLong("end")), seconds, seconds, completed = true)
            done += id
        }
        // Solo hacen falta los recientes para no contar dos veces
        prefs.edit().putStringSet(KEY_DONE, done.sortedBy { it.substringBefore('-').toLongOrNull() ?: 0 }.takeLast(MAX_REMEMBERED).toSet()).apply()
        return ids
    }

    private companion object {
        const val PREFS = "wear_offline"
        const val KEY_DONE = "done"
        const val MAX_REMEMBERED = 500
    }
}
