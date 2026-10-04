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

/** Recibe la alarma de fin de sesión y le avisa al motor. */
@AndroidEntryPoint
class TimerAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var engine: TimerEngine

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        // goAsync mantiene despierto al teléfono hasta que el motor termine de procesar
        val pending = goAsync()
        scope.launch {
            try {
                engine.onDeadline()
            } finally {
                pending.finish()
            }
        }
    }
}
