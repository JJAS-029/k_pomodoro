package com.jjas.labpomodoro.service

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.jjas.labpomodoro.MainActivity
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Botón de los ajustes rápidos: sin plan lo inicia; con un plan corriendo pausa o continúa. Se ve
 * encendido mientras hay un plan y muestra la sesión y los minutos que faltan.
 */
@AndroidEntryPoint
class TimerTileService : TileService() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var settings: SettingsRepository

    private val scope = MainScope()
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listening = scope.launch {
            engine.state.collectLatest { state ->
                // Mientras se ve la cortina, los minutos se actualizan solos
                while (true) {
                    render(state)
                    if (state !is TimerState.Active || state.isPaused) break
                    delay(REFRESH_MILLIS)
                }
            }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            when (val state = engine.state.value) {
                is TimerState.Active -> if (state.isPaused) engine.resume() else engine.pause()
                else -> {
                    // Si Android no deja iniciar el servicio desde aquí, se abre la app
                    val started = runCatching { engine.start(settings.settings.first().session) }.isSuccess
                    if (!started) openApp()
                }
            }
        }
    }

    private fun render(state: TimerState) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_timer)
        tile.label = "Lab Pomodoro"
        when (state) {
            is TimerState.Active -> {
                tile.state = Tile.STATE_ACTIVE
                val minutes = (state.remainingMillis(SystemClock.elapsedRealtime()) + 59_999) / 60_000
                tile.subtitleOrNull = if (state.isPaused) "En pausa · $minutes min" else "${state.current.type.label()} · $minutes min"
            }
            is TimerState.Finished -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitleOrNull = "Experimento completado"
            }
            TimerState.Idle -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitleOrNull = "Iniciar"
            }
        }
        tile.updateTile()
    }

    private var Tile.subtitleOrNull: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle?.toString() else null
        set(value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) subtitle = value
        }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val REFRESH_MILLIS = 20_000L
    }
}

/** Pide agregar el botón a los ajustes rápidos (Android 13+). Devuelve false si no se puede. */
fun Context.requestAddTimerTile(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
    val manager = getSystemService(StatusBarManager::class.java) ?: return false
    manager.requestAddTileService(
        ComponentName(this, TimerTileService::class.java),
        "Lab Pomodoro",
        Icon.createWithResource(this, R.drawable.ic_stat_timer),
        mainExecutor,
    ) { }
    return true
}
