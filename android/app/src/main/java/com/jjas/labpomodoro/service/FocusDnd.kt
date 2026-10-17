package com.jjas.labpomodoro.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * No molestar automático (Pro): durante el trabajo pone No molestar en modo prioridad y lo quita en
 * los descansos, al pausar o al terminar. Solo deshace lo que la app hizo: si el usuario ya tenía
 * No molestar puesto, no lo toca. Lo que hizo se guarda para deshacerlo aunque Android cierre la app.
 */
@Singleton
class FocusDnd @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start() {
        scope.launch {
            combine(engine.state, settings.settings) { state, prefs ->
                val working = state is TimerState.Active && state.current.type == SessionType.WORK && !state.isPaused
                prefs.isPro && prefs.focusDnd && working
            }
                .distinctUntilChanged()
                .collect { quiet -> if (quiet) enable() else restore() }
        }
    }

    private fun enable() {
        if (!hasAccess()) return
        // Si ya estaba en silencio por el usuario, no se toca ni se deshace después
        if (notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        runCatching {
            notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            saved.edit { putBoolean(KEY_ENABLED_BY_US, true) }
        }.onFailure { Log.w(TAG, "No se pudo activar No molestar", it) }
    }

    private fun restore() {
        if (!saved.getBoolean(KEY_ENABLED_BY_US, false)) return
        saved.edit { putBoolean(KEY_ENABLED_BY_US, false) }
        if (!hasAccess()) return
        runCatching { notifications.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) }
            .onFailure { Log.w(TAG, "No se pudo quitar No molestar", it) }
    }

    fun hasAccess(): Boolean = notifications.isNotificationPolicyAccessGranted

    private companion object {
        const val TAG = "FocusDnd"
        const val PREFS = "focus_dnd"
        const val KEY_ENABLED_BY_US = "enabled_by_us"
    }
}
