package com.jjas.labpomodoro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.timer.TimerEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tras reiniciar el teléfono o actualizar la app (que cierra el proceso) se pierden las alarmas:
 * se recupera el plan guardado para volver a programarlas y mostrar la notificación.
 */
@AndroidEntryPoint
class TimerRestoreReceiver : BroadcastReceiver() {

    @Inject lateinit var engine: TimerEngine

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        scope.launch {
            try {
                engine.ensureRestored()
            } finally {
                pending.finish()
            }
        }
    }
}
