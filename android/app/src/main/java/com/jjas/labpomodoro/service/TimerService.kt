package com.jjas.labpomodoro.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Servicio en primer plano mientras hay un plan en curso: mantiene vivo el proceso, muestra la
 * cuenta regresiva y atiende los botones de la notificación. Se detiene solo cuando el plan acaba.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var notifications: TimerNotifications

    private val scope = MainScope()

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        startInForeground()
        scope.launch {
            engine.state.collectLatest { state ->
                if (state is TimerState.Active) {
                    // El cronómetro avanza solo; la barra del plan se refresca cada 30 s mientras corre
                    do {
                        // Al cruzar el segundo, para que el cronómetro de la notificación vaya parejo con la app
                        if (!state.isPaused) delay(state.millisToNextSecond(SystemClock.elapsedRealtime()))
                        notifications.notify(
                            notifications.build(state, SystemClock.elapsedRealtime(), System.currentTimeMillis())
                        )
                        if (state.isPaused) break
                        delay(PROGRESS_REFRESH_MILLIS)
                    } while (true)
                } else {
                    ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService exige llamar a startForeground en cada arranque
        startInForeground()
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { engine.pause() }
            ACTION_RESUME -> scope.launch { engine.resume() }
            ACTION_SKIP -> scope.launch { engine.skip() }
            ACTION_STOP -> scope.launch { engine.reset() }
        }
        // Si el sistema mata el proceso el plan en memoria se pierde: no tiene caso recrear el servicio
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        val notification = notifications.build(engine.state.value, SystemClock.elapsedRealtime(), System.currentTimeMillis())
        ServiceCompat.startForeground(this, TimerNotifications.NOTIFICATION_ID, notification, type)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PAUSE = "com.jjas.labpomodoro.action.PAUSE"
        const val ACTION_RESUME = "com.jjas.labpomodoro.action.RESUME"
        const val ACTION_SKIP = "com.jjas.labpomodoro.action.SKIP"
        const val ACTION_STOP = "com.jjas.labpomodoro.action.STOP"
        private const val PROGRESS_REFRESH_MILLIS = 30_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        }

        /** Para los botones de la notificación y de la ventana PiP. */
        fun actionIntent(context: Context, action: String): PendingIntent = PendingIntent.getService(
            context,
            action.hashCode(),
            Intent(context, TimerService::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
