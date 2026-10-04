package com.jjas.labpomodoro.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jjas.labpomodoro.timer.DeadlineScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarma exacta al final de cada sesión para despertar al teléfono en reposo (Doze).
 * Usa USE_EXACT_ALARM (Android 13+, permitido para apps de temporizador); si el sistema no deja
 * programar alarmas exactas, cae a una inexacta y el `delay` del motor sigue como respaldo.
 */
@Singleton
class AlarmDeadlineScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeadlineScheduler {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    private val pendingIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, TimerAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun schedule(atElapsedRealtime: Long) {
        val type = AlarmManager.ELAPSED_REALTIME_WAKEUP
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(type, atElapsedRealtime, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(type, atElapsedRealtime, pendingIntent)
        }
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }
}
