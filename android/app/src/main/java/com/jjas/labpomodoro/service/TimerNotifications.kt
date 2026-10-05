package com.jjas.labpomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jjas.labpomodoro.MainActivity
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        // Silencioso: los sonidos los reproduce la app con SoundPool según la configuración
        val channel = NotificationChannel(CHANNEL_ID, "Temporizador", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Cuenta regresiva de la sesión en curso"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun notify(notification: Notification) = manager.notify(NOTIFICATION_ID, notification)

    /**
     * Mientras corre se usa el cronómetro en cuenta regresiva del sistema, así la notificación
     * se actualiza sola sin despertar a la app cada segundo.
     */
    fun build(state: TimerState, nowElapsed: Long, nowWallMillis: Long): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())

        if (state !is TimerState.Active) {
            return builder.setContentTitle("Lab Pomodoro").build()
        }

        val remaining = state.remainingMillis(nowElapsed)
        builder
            .setContentTitle(state.current.type.label())
            .setSubText("Sesión ${state.index + 1} de ${state.plan.size}")

        if (state.isPaused) {
            builder
                .setContentText("En pausa · quedan ${formatMinutesSeconds(remaining)}")
                .setShowWhen(false)
                .addAction(0, "Continuar", serviceIntent(TimerService.ACTION_RESUME))
        } else {
            builder
                .setContentText(state.next?.let { "Después: ${it.type.label().lowercase()}" } ?: "Última sesión")
                .setWhen(nowWallMillis + remaining)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, "Pausar", serviceIntent(TimerService.ACTION_PAUSE))
        }
        return builder
            .addAction(0, "Saltar", serviceIntent(TimerService.ACTION_SKIP))
            .addAction(0, "Detener", serviceIntent(TimerService.ACTION_STOP))
            .build()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceIntent(action: String): PendingIntent = TimerService.actionIntent(context, action)

    companion object {
        const val CHANNEL_ID = "timer"
        const val NOTIFICATION_ID = 1
    }
}

fun SessionType.label(): String = when (this) {
    SessionType.WORK -> "Trabajo"
    SessionType.SHORT_BREAK -> "Descanso corto"
    SessionType.LONG_BREAK -> "Descanso largo"
}

fun formatMinutesSeconds(millis: Long): String {
    // Redondeo hacia arriba: con 0.4 s restantes se muestra 00:01, no 00:00
    val totalSeconds = (millis + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
