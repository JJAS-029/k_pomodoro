package com.jjas.labpomodoro.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.timer.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Órdenes del reloj (empezar, pausar, continuar, saltar). Pasan por [TimerEngine], igual que los
 * botones de la notificación, así que respetan las salas de estudio. Saltar es Pro.
 */
@AndroidEntryPoint
class WearCommandService : WearableListenerService() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var settings: SettingsRepository

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
}
