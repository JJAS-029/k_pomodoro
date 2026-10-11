package com.jjas.labpomodoro.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.jjas.labpomodoro.timer.DeadlineScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarma exacta al final de cada sesión para despertar al teléfono en reposo (Doze).
 * Usa SCHEDULE_EXACT_ALARM (en Android 14+ el usuario lo concede en los ajustes, ver
 * [requestExactAlarms]); sin él cae a una alarma inexacta y el `delay` del motor sigue como respaldo.
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
        if (context.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(type, atElapsedRealtime, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(type, atElapsedRealtime, pendingIntent)
        }
    }

    override fun cancel() {
        alarmManager.cancel(pendingIntent)
    }
}

/** Si la app puede programar alarmas exactas; antes de Android 12 siempre. */
fun Context.canScheduleExactAlarms(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

/** Abre el ajuste del sistema donde el usuario permite las alarmas exactas a esta app. */
fun Context.requestExactAlarms() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", packageName, null)))
}
