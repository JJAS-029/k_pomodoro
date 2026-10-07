package com.jjas.labpomodoro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
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
        val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.main_timer_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.main_timer_channel_description)
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
            // Android 16+: Live Update (chip en la barra de estado, pantalla de bloqueo y Now Bar de Samsung)
            .setRequestPromotedOngoing(true)

        if (state !is TimerState.Active) {
            return builder.setContentTitle(context.getString(R.string.app_name)).build()
        }

        val remaining = state.remainingMillis(nowElapsed)
        builder
            .setContentTitle(context.getString(state.current.type.labelRes()))
            .setSubText(context.getString(R.string.main_session_of, state.index + 1, state.plan.size))
            .setStyle(planProgress(state, remaining))

        if (state.isPaused) {
            builder
                .setContentText(context.getString(R.string.main_notification_paused, formatMinutesSeconds(remaining)))
                .setShowWhen(false)
                // El chip no puede mostrar un reloj detenido; avisa que está en pausa
                .setShortCriticalText(context.getString(R.string.main_notification_paused_chip))
                .addAction(0, context.getString(R.string.main_action_resume), serviceIntent(TimerService.ACTION_RESUME))
        } else {
            builder
                .setContentText(
                    state.next?.let { context.getString(R.string.main_next, context.getString(it.type.labelRes()).lowercase()) }
                        ?: context.getString(R.string.main_last_session)
                )
                // Igual que el widget: 1 s de más porque el cronómetro redondea hacia abajo (la
                // notificación se actualiza justo al cruzar el segundo, en TimerService)
                .setWhen(nowWallMillis + remaining + 1_000)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .addAction(0, context.getString(R.string.main_action_pause), serviceIntent(TimerService.ACTION_PAUSE))
        }
        return builder
            .addAction(0, context.getString(R.string.main_action_skip), serviceIntent(TimerService.ACTION_SKIP))
            .addAction(0, context.getString(R.string.main_action_stop), serviceIntent(TimerService.ACTION_STOP))
            .build()
    }

    /**
     * Barra de avance de todo el plan: un segmento por sesión con el color de su tipo. En versiones
     * anteriores a Android 16 se ve como una barra de progreso normal.
     */
    private fun planProgress(state: TimerState.Active, remaining: Long): NotificationCompat.ProgressStyle {
        val doneSeconds = state.plan.take(state.index).sumOf { it.durationSeconds } +
            (state.current.durationSeconds - remaining / 1000).coerceAtLeast(0)
        val segments = if (state.plan.size <= MAX_SEGMENTS) {
            state.plan.map { NotificationCompat.ProgressStyle.Segment(it.durationSeconds).setColor(it.type.color()) }
        } else {
            // Planes muy largos: un solo segmento para que la barra no se vuelva ilegible
            listOf(NotificationCompat.ProgressStyle.Segment(state.plan.sumOf { it.durationSeconds }).setColor(WORK_COLOR))
        }
        return NotificationCompat.ProgressStyle()
            .setProgressSegments(segments)
            .setProgress(doneSeconds.toInt())
            .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_timer))
    }

    private fun SessionType.color(): Int = when (this) {
        SessionType.WORK -> WORK_COLOR
        SessionType.SHORT_BREAK -> 0xFF4DB6AC.toInt()
        SessionType.LONG_BREAK -> 0xFF7986CB.toInt()
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
        private const val MAX_SEGMENTS = 15
        private val WORK_COLOR = 0xFFFFB74D.toInt()
    }
}

@StringRes
fun SessionType.labelRes(): Int = when (this) {
    SessionType.WORK -> R.string.main_session_work
    SessionType.SHORT_BREAK -> R.string.main_session_short_break
    SessionType.LONG_BREAK -> R.string.main_session_long_break
}

fun formatMinutesSeconds(millis: Long): String {
    // Redondeo hacia arriba: con 0.4 s restantes se muestra 00:01, no 00:00
    val totalSeconds = (millis + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
