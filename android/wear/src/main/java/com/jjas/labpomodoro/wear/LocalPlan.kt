package com.jjas.labpomodoro.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Modo semiautónomo (Pro): el reloj corre el plan de siempre por su cuenta cuando el teléfono no
 * está cerca. Se guarda como la hora de inicio y las pausas (igual que las salas), así que no
 * hace falta que la app siga abierta: cada pantalla calcula dónde va.
 *
 * Los pomodoros completados se apuntan en [OFFLINE] y se publican en la Data Layer, que se los
 * entrega al teléfono al reconectarse; el teléfono los registra y confirma con [WearContract.ACK_PATH].
 */
object LocalPlan {
    private const val PREFS = "local_plan"
    private const val KEY_TYPES = "types"
    private const val KEY_SECONDS = "seconds"
    private const val KEY_STARTED = "startedAt"
    private const val KEY_PAUSED = "pausedAt"
    private const val KEY_PAUSED_TOTAL = "pausedTotal"
    private const val KEY_RECORDED = "recorded"
    private const val OFFLINE = "offline"

    /** El plan de siempre del teléfono (para correrlo sin él) y si hay Pro. */
    private const val KEY_USUAL_TYPES = "usualTypes"
    private const val KEY_USUAL_SECONDS = "usualSeconds"
    private const val KEY_PRO = "pro"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Guarda lo que hace falta del teléfono para correr el plan sin él. */
    fun rememberPhone(context: Context, types: List<String>, seconds: List<Int>, isPro: Boolean) {
        val edit = prefs(context).edit().putBoolean(KEY_PRO, isPro)
        if (types.isNotEmpty() && types.size == seconds.size) {
            edit.putString(KEY_USUAL_TYPES, types.joinToString(",")).putString(KEY_USUAL_SECONDS, seconds.joinToString(","))
        }
        edit.apply()
    }

    /** Se puede correr el plan en el reloj: hay Pro y ya llegó el plan de siempre alguna vez. */
    fun canRunAlone(context: Context): Boolean = prefs(context).let { it.getBoolean(KEY_PRO, false) && it.contains(KEY_USUAL_TYPES) }

    fun isActive(context: Context): Boolean = prefs(context).getLong(KEY_STARTED, 0) > 0

    fun start(context: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        p.edit()
            .putString(KEY_TYPES, p.getString(KEY_USUAL_TYPES, ""))
            .putString(KEY_SECONDS, p.getString(KEY_USUAL_SECONDS, ""))
            .putLong(KEY_STARTED, now)
            .putLong(KEY_PAUSED, 0)
            .putLong(KEY_PAUSED_TOTAL, 0)
            .putInt(KEY_RECORDED, 0)
            .apply()
        refresh(context, now)
    }

    fun pause(context: Context, now: Long = System.currentTimeMillis()) {
        if (!isActive(context) || prefs(context).getLong(KEY_PAUSED, 0) > 0) return
        prefs(context).edit().putLong(KEY_PAUSED, now).apply()
        refresh(context, now)
    }

    fun resume(context: Context, now: Long = System.currentTimeMillis()) {
        val p = prefs(context)
        val pausedAt = p.getLong(KEY_PAUSED, 0)
        if (pausedAt == 0L) return
        p.edit().putLong(KEY_PAUSED, 0).putLong(KEY_PAUSED_TOTAL, p.getLong(KEY_PAUSED_TOTAL, 0) + (now - pausedAt)).apply()
        refresh(context, now)
    }

    /** Termina el plan del reloj: apunta lo completado y lo manda al teléfono. */
    fun stop(context: Context, now: Long = System.currentTimeMillis()) {
        if (!isActive(context)) return
        collect(context, now)
        prefs(context).edit().remove(KEY_STARTED).remove(KEY_PAUSED).apply()
        cancelAlarm(context)
        WatchNotifier.update(context, WatchTimer(state = "IDLE"))
        TimerTileService.refresh(context)
        TimerComplicationService.refresh(context)
    }

    /** El estado del plan del reloj como lo dibujan la app, el tile y la complicación; null si no hay. */
    fun timer(context: Context, now: Long = System.currentTimeMillis()): WatchTimer? {
        val p = prefs(context)
        val started = p.getLong(KEY_STARTED, 0)
        if (started == 0L) return null
        val plan = plan(p.getString(KEY_TYPES, ""), p.getString(KEY_SECONDS, ""))
        val pausedAt = p.getLong(KEY_PAUSED, 0)
        val position = position(plan, elapsed(started, pausedAt, p.getLong(KEY_PAUSED_TOTAL, 0), now))
            ?: return WatchTimer(state = "FINISHED", isPro = true, local = true)
        val (index, offset) = position
        val (type, seconds) = plan[index]
        val remaining = seconds * 1000L - offset
        return WatchTimer(
            state = "ACTIVE",
            type = type,
            endsAt = now + remaining,
            paused = pausedAt > 0,
            remaining = remaining,
            duration = seconds * 1000L,
            pomodoro = plan.take(index + 1).count { it.first == "WORK" }.coerceAtLeast(1),
            pomodoros = plan.count { it.first == "WORK" },
            color = colorOf(type),
            shape = "BEAKER",
            isPro = true,
            local = true,
        )
    }

    /**
     * Apunta los pomodoros que ya terminaron y aún no se apuntaban, programa la alarma de la
     * siguiente sesión y actualiza la notificación, el tile y la complicación. Devuelve true si
     * se pasó a otra sesión (para vibrar).
     */
    fun refresh(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val changed = collect(context, now)
        val timer = timer(context, now)
        if (timer == null || timer.state == "FINISHED") {
            if (timer != null) stop(context, now)
            return changed
        }
        WatchNotifier.update(context, timer)
        TimerTileService.refresh(context)
        TimerComplicationService.refresh(context)
        if (timer.paused) cancelAlarm(context) else scheduleAlarm(context, now + timer.remaining)
        return changed
    }

    /** Lo que el teléfono confirmó que ya registró. */
    fun acknowledge(context: Context, ids: Set<String>) {
        val p = prefs(context)
        val pending = JSONArray(p.getString(OFFLINE, "[]"))
        val left = JSONArray()
        for (i in 0 until pending.length()) pending.getJSONObject(i).takeIf { it.getString("id") !in ids }?.let(left::put)
        p.edit().putString(OFFLINE, left.toString()).apply()
        publish(context, left)
    }

    /** Vuelve a publicar lo pendiente (al abrir la app), por si el teléfono no confirmó. */
    fun publishPending(context: Context) {
        val pending = JSONArray(prefs(context).getString(OFFLINE, "[]"))
        if (pending.length() > 0) publish(context, pending)
    }

    private fun collect(context: Context, now: Long): Boolean {
        val p = prefs(context)
        val started = p.getLong(KEY_STARTED, 0)
        if (started == 0L) return false
        val plan = plan(p.getString(KEY_TYPES, ""), p.getString(KEY_SECONDS, ""))
        val pausedTotal = p.getLong(KEY_PAUSED_TOTAL, 0)
        val elapsed = elapsed(started, p.getLong(KEY_PAUSED, 0), pausedTotal, now)
        val done = position(plan, elapsed)?.first ?: plan.size
        val recorded = p.getInt(KEY_RECORDED, 0)
        if (done <= recorded) return false
        val pending = JSONArray(p.getString(OFFLINE, "[]"))
        var end = 0L
        plan.forEachIndexed { i, (type, seconds) ->
            end += seconds * 1000L
            if (i < recorded || i >= done || type != "WORK") return@forEachIndexed
            // La hora exacta se pierde con las pausas: se suponen todas antes de esta sesión
            val endAt = started + pausedTotal + end
            pending.put(
                JSONObject().put("id", "$started-$i").put("type", type)
                    .put("start", endAt - seconds * 1000L).put("end", endAt).put("seconds", seconds)
            )
        }
        p.edit().putInt(KEY_RECORDED, done).putString(OFFLINE, pending.toString()).apply()
        publish(context, pending)
        return true
    }

    private fun publish(context: Context, pending: JSONArray) {
        val request = PutDataMapRequest.create(WearContract.OFFLINE_PATH).apply {
            dataMap.putString(WearContract.KEY_SESSIONS, pending.toString())
        }
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest().setUrgent())
    }

    private fun elapsed(started: Long, pausedAt: Long, pausedTotal: Long, now: Long) =
        ((if (pausedAt > 0) pausedAt else now) - started - pausedTotal).coerceAtLeast(0)

    /** Sesión en curso (índice y lo que lleva, en ms) o null si el plan ya terminó. */
    fun position(plan: List<Pair<String, Int>>, elapsed: Long): Pair<Int, Long>? {
        var before = 0L
        plan.forEachIndexed { i, (_, seconds) ->
            val length = seconds * 1000L
            if (elapsed < before + length) return i to (elapsed - before)
            before += length
        }
        return null
    }

    private fun plan(types: String?, seconds: String?): List<Pair<String, Int>> {
        val t = types.orEmpty().split(',').filter { it.isNotBlank() }
        val s = seconds.orEmpty().split(',').mapNotNull { it.toIntOrNull() }
        return t.zip(s)
    }

    /** Los colores de siempre: trabajo azul, descanso corto verde y largo rojo. */
    private fun colorOf(type: String): Int = when (type) {
        "WORK" -> 0xFF7FC8FF.toInt()
        "SHORT_BREAK" -> 0xFF2ECC71.toInt()
        else -> 0xFFE74C3C.toInt()
    }

    private fun alarmIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LocalPlanReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun scheduleAlarm(context: Context, at: Long) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        // Exacta si se puede; si no, Android la entrega con un poco de retraso
        // Antes de Android 12 no hace falta permiso para las exactas
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        }
    }

    private fun cancelAlarm(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }
}

/** Fin de una sesión del plan del reloj: vibra, apunta y programa la siguiente. */
class LocalPlanReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LocalPlan.refresh(context)
        val timer = LocalPlan.timer(context)
        WatchVibes.vibrate(context, timer ?: WatchTimer(state = "FINISHED"))
    }
}
