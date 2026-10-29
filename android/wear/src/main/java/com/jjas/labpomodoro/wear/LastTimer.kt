package com.jjas.labpomodoro.wear

import android.content.Context
import org.json.JSONObject

/** Último estado del timer que llegó del teléfono, para el tile (que se dibuja con la app cerrada). */
object LastTimer {
    private const val PREFS = "watch_timer"
    private const val KEY = "last"

    fun save(context: Context, timer: WatchTimer) {
        val json = JSONObject()
            .put("state", timer.state).put("type", timer.type).put("endsAt", timer.endsAt).put("paused", timer.paused)
            .put("remaining", timer.remaining).put("duration", timer.duration).put("pomodoro", timer.pomodoro)
            .put("pomodoros", timer.pomodoros).put("color", timer.color).put("shape", timer.shape).put("pro", timer.isPro)
            .put("room", timer.inRoom).put("host", timer.isHost)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
        TimerTileService.refresh(context)
    }

    fun read(context: Context): WatchTimer? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return null
        return runCatching {
            val j = JSONObject(raw)
            WatchTimer(
                state = j.getString("state"), type = j.getString("type"), endsAt = j.getLong("endsAt"), paused = j.getBoolean("paused"),
                remaining = j.getLong("remaining"), duration = j.getLong("duration"), pomodoro = j.getInt("pomodoro"),
                pomodoros = j.getInt("pomodoros"), color = j.getInt("color"), shape = j.getString("shape"), isPro = j.getBoolean("pro"),
                inRoom = j.getBoolean("room"), isHost = j.getBoolean("host"),
            )
        }.getOrNull()
    }
}
