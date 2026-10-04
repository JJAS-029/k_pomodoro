package com.jjas.labpomodoro.service

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.remote.AnalyticsTracker
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reacciona a los eventos del motor: sonido, vibración y analytics. Se arranca en
 * `LabPomodoroApp.onCreate` para no perder el evento de inicio aunque el servicio aún no exista.
 */
@Singleton
class TimerEffects @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val analytics: AnalyticsTracker,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // Los mismos sonidos del prototipo: in.mp3, des.mp3 y larg.mp3
    private val startSound = soundPool.load(context, R.raw.sound_start, 1)
    private val shortBreakSound = soundPool.load(context, R.raw.sound_short_break, 1)
    private val longBreakSound = soundPool.load(context, R.raw.sound_long_break, 1)

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
    }

    fun start() {
        scope.launch { engine.events.collect(::handle) }
    }

    private suspend fun handle(event: TimerEvent) {
        val prefs = settings.settings.first()
        when (event) {
            TimerEvent.PlanStarted -> if (prefs.soundEnabled) play(startSound)

            is TimerEvent.SessionEnded -> {
                if (event.completed) {
                    // El sonido anuncia lo que sigue; al acabar el plan suena el de descanso largo
                    if (prefs.soundEnabled) {
                        play(
                            when (event.next?.type) {
                                SessionType.WORK -> startSound
                                SessionType.SHORT_BREAK -> shortBreakSound
                                SessionType.LONG_BREAK, null -> longBreakSound
                            }
                        )
                    }
                    if (prefs.vibrationEnabled) vibrate()
                    analytics.logSessionCompleted(
                        type = event.session.type.name.lowercase(),
                        hourOfDay = event.startedAt.atZone(ZoneId.systemDefault()).hour,
                        durationMin = event.session.durationSeconds / 60,
                    )
                } else if (prefs.soundEnabled && event.next?.type == SessionType.WORK) {
                    play(startSound)
                }
            }
        }
    }

    private fun play(soundId: Int) {
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    private fun vibrate() {
        if (!vibrator.hasVibrator()) return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 200, 500, 200, 500), -1))
    }
}
