package com.jjas.labpomodoro.wear

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

/**
 * Recibe los cambios del timer aunque la app del reloj esté cerrada: vibra al cambiar de sesión
 * (el teléfono puede ir en la bolsa) y mantiene la actividad en curso, con el ícono en la carátula
 * y la cuenta regresiva.
 */
class WatchTimerService : WearableListenerService() {

    override fun onDataChanged(events: DataEventBuffer) {
        val timer = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WearContract.TIMER_PATH }
            .lastOrNull()
            ?.let { DataMapItem.fromDataItem(it.dataItem).dataMap.toWatchTimer() }
            ?: return
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val session = sessionKey(timer)
        val before = prefs.getString(KEY_SESSION, null)
        // Vibra al pasar a otra sesión o al terminar el plan; pausar o continuar no cuenta
        if (before != null && before != session && (timer.active || timer.state == "FINISHED") && before != IDLE) vibrate(timer)
        prefs.edit().putString(KEY_SESSION, session).apply()
        WatchNotifier.update(this, timer)
    }

    private fun vibrate(timer: WatchTimer) {
        val vibrator = getSystemService(Vibrator::class.java) ?: return
        // Trabajo: dos golpes firmes; descanso: tres suaves; fin del plan: uno largo
        val pattern = when {
            timer.state == "FINISHED" -> longArrayOf(0, 700)
            timer.type == "WORK" -> longArrayOf(0, 300, 150, 300)
            else -> longArrayOf(0, 150, 120, 150, 120, 150)
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    companion object {
        private const val PREFS = "watch_timer"
        private const val KEY_SESSION = "session"
        private const val IDLE = "IDLE"

        /** Qué sesión es: cambia al pasar de trabajo a descanso o de un pomodoro al siguiente. */
        fun sessionKey(timer: WatchTimer): String = if (timer.active) "${timer.type}-${timer.pomodoro}" else timer.state
    }
}

/** Notificación en curso del reloj, promovida a actividad en curso de Wear OS. */
object WatchNotifier {
    private const val CHANNEL = "timer"
    private const val ID = 1

    fun update(context: Context, timer: WatchTimer) {
        val manager = NotificationManagerCompat.from(context)
        if (!timer.active) {
            manager.cancel(ID)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_timer), NotificationManager.IMPORTANCE_LOW)
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, WearMainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val label = context.getString(sessionLabel(timer))
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(label)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
        if (!timer.paused) {
            // Cuenta regresiva hasta la hora de fin
            builder.setWhen(timer.endsAt).setUsesChronometer(true).setChronometerCountDown(true).setShowWhen(true)
        }
        val status = if (timer.paused) {
            Status.Builder().addTemplate(label).build()
        } else {
            Status.Builder()
                .addTemplate("#label# · #time#")
                .addPart("label", Status.TextPart(label))
                // TimerPart cuenta con elapsedRealtime, no con la hora de pared
                .addPart("time", Status.TimerPart(SystemClock.elapsedRealtime() + (timer.endsAt - System.currentTimeMillis())))
                .build()
        }
        OngoingActivity.Builder(context, ID, builder)
            .setStaticIcon(R.drawable.ic_stat_timer)
            .setTouchIntent(open)
            .setStatus(status)
            .build()
            .apply(context)
        manager.notify(ID, builder.build())
    }

    fun sessionLabel(timer: WatchTimer): Int = when {
        timer.paused -> R.string.paused
        timer.type == "WORK" -> R.string.work
        timer.type == "LONG_BREAK" -> R.string.long_break
        else -> R.string.short_break
    }
}
