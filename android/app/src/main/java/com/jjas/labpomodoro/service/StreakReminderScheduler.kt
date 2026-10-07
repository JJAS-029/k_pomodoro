package com.jjas.labpomodoro.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jjas.labpomodoro.MainActivity
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.usecase.StreakReminder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recordatorio diario de racha (opcional): a la hora elegida, si ese día no hubo ningún pomodoro,
 * una notificación. Usa una alarma inexacta (ahorra batería; unos minutos de diferencia no
 * importan) y se reprograma cada día y al reiniciar el teléfono.
 */
@Singleton
class StreakReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    /** Sigue la configuración: programa o cancela cuando cambia. */
    fun start() {
        scope.launch {
            settings.settings
                .map { it.reminderEnabled to it.reminderHour }
                .distinctUntilChanged()
                .collect { (enabled, hour) -> if (enabled) schedule(hour) else cancel() }
        }
    }

    fun schedule(hour: Int) {
        val now = ZonedDateTime.now(clock)
        var next = now.with(LocalTime.of(hour, 0))
        if (!next.isAfter(now)) next = next.plusDays(1)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pendingIntent())
    }

    fun cancel() = alarms.cancel(pendingIntent())

    /** Lo llama el receptor a la hora del aviso. */
    suspend fun onAlarm(sessionDao: SessionDao) {
        val prefs = settings.settings.first()
        if (!prefs.reminderEnabled) return
        val activeDays = sessionDao.observeActiveDays().first().map(LocalDate::ofEpochDay)
        StreakReminder.message(activeDays, LocalDate.now(clock))?.let(::notify)
        schedule(prefs.reminderHour)
    }

    private fun notify(message: StreakReminder.Message) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.main_streak_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.main_streak_channel_description)
            }
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val (title, text) = when (message) {
            is StreakReminder.Message.StreakAtRisk -> context.resources.getQuantityString(
                R.plurals.main_streak_reminder_title, message.streak, message.streak,
            ) to context.getString(R.string.main_streak_reminder_text)
            StreakReminder.Message.StartNew ->
                context.getString(R.string.main_streak_new_title) to context.getString(R.string.main_streak_new_text)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // Sin permiso de notificaciones no se muestra; no hace falta pedirlo aquí
        runCatching { manager.notify(NOTIFICATION_ID, notification) }
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, StreakReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "streak_reminder"
        const val NOTIFICATION_ID = 7
        const val REQUEST_CODE = 70
    }
}

/** La alarma del recordatorio y el reinicio del teléfono (para volver a programarla). */
@AndroidEntryPoint
class StreakReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: StreakReminderScheduler

    @Inject lateinit var sessionDao: SessionDao

    @Inject lateinit var settings: SettingsRepository

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                    val prefs = settings.settings.first()
                    if (prefs.reminderEnabled) scheduler.schedule(prefs.reminderHour)
                } else {
                    scheduler.onAlarm(sessionDao)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
