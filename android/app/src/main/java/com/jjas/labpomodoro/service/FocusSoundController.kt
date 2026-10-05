package com.jjas.labpomodoro.service

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.FocusSound
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enciende el sonido de concentración (Pro) solo mientras corre una sesión de trabajo: en pausa,
 * en los descansos y al terminar se apaga con un fundido.
 */
@Singleton
class FocusSoundController @Inject constructor(
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val player: FocusSoundPlayer,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var previewJob: Job? = null

    fun start() {
        scope.launch {
            combine(engine.state, settings.settings, ::desired).distinctUntilChanged().collect { (sound, volume) ->
                // Una vista previa en curso no se corta por cambios de configuración
                if (previewJob?.isActive != true) player.play(sound, volume)
            }
        }
    }

    /** Para probar un sonido desde Configuración sin tener que iniciar el timer. */
    fun preview(sound: FocusSound, volume: Float) {
        previewJob?.cancel()
        previewJob = scope.launch {
            player.play(sound, volume)
            delay(PREVIEW_MILLIS)
            // Al terminar vuelve a lo que corresponda según el timer
            val (sound, current) = desired(engine.state.value, settings.settings.first())
            player.play(sound, current)
        }
    }

    private fun desired(state: TimerState, prefs: AppSettings): Pair<FocusSound, Float> {
        val working = state is TimerState.Active && !state.isPaused && state.current.type == SessionType.WORK
        return (if (working && prefs.isPro) prefs.focusSound else FocusSound.OFF) to prefs.focusVolume
    }

    private companion object {
        const val PREVIEW_MILLIS = 5_000L
    }
}
