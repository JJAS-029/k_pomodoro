package com.jjas.labpomodoro.service

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.jjas.labpomodoro.domain.model.FocusSound
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.random.Random

/**
 * Genera los sonidos de fondo en tiempo real y los reproduce en un hilo propio. Cambiar de sonido
 * o de volumen se hace con un fundido corto para que no haya chasquidos.
 */
@Singleton
class FocusSoundPlayer @Inject constructor() {

    @Volatile private var target: FocusSound = FocusSound.OFF
    @Volatile private var targetVolume = 0f
    private var thread: Thread? = null

    /** Reproduce [sound] a [volume] (0..1); con [FocusSound.OFF] se apaga con un fundido. */
    @Synchronized
    fun play(sound: FocusSound, volume: Float) {
        target = sound
        targetVolume = if (sound == FocusSound.OFF) 0f else volume.coerceIn(0f, 1f)
        if (sound != FocusSound.OFF && thread?.isAlive != true) {
            thread = Thread(::run, "focus-sound").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    fun stop() = play(FocusSound.OFF, 0f)

    private fun run() {
        val bufferBytes = maxOf(
            AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
            CHUNK * 2 * 4,
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val generator = NoiseGenerator()
        val buffer = ShortArray(CHUNK)
        var playing = FocusSound.OFF
        var gain = 0f
        track.play()
        try {
            while (true) {
                val wanted = target
                // Para cambiar de sonido primero se baja a cero y luego se sube con el nuevo
                val goal = if (wanted != playing) 0f else targetVolume
                for (i in 0 until CHUNK) {
                    gain += (goal - gain).coerceIn(-FADE_STEP, FADE_STEP)
                    val sample = if (playing == FocusSound.OFF) 0f else generator.next(playing)
                    // Curva de volumen perceptual (al cuadrado) y límite para no saturar
                    buffer[i] = ((sample * gain * gain).coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buffer, 0, CHUNK)
                if (gain == 0f && wanted != playing) {
                    if (wanted == FocusSound.OFF) break
                    playing = wanted
                    generator.reset()
                }
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val CHUNK = 1_024

        /** Fundido de ~1.5 s de silencio a volumen máximo. */
        const val FADE_STEP = 1f / (SAMPLE_RATE * 1.5f)
    }
}

/** Generadores de ruido por muestra. Cada sonido conserva su propio estado de filtro. */
private class NoiseGenerator {
    private val random = Random(System.nanoTime())

    // Ruido rosa (filtro de Paul Kellet)
    private var b0 = 0f; private var b1 = 0f; private var b2 = 0f; private var b3 = 0f
    private var b4 = 0f; private var b5 = 0f; private var b6 = 0f

    // Ruido café (ruido blanco integrado)
    private var brown = 0f

    // Lluvia: gotas que suenan y se apagan rápido
    private var dropEnergy = 0f
    private var dropHigh = 0f
    private var lastWhite = 0f

    // Olas: tiempo para la envolvente lenta
    private var t = 0L

    fun reset() {
        b0 = 0f; b1 = 0f; b2 = 0f; b3 = 0f; b4 = 0f; b5 = 0f; b6 = 0f
        brown = 0f; dropEnergy = 0f; dropHigh = 0f; t = 0L
    }

    private fun white() = random.nextFloat() * 2f - 1f

    private fun pink(): Float {
        val w = white()
        b0 = 0.99886f * b0 + w * 0.0555179f
        b1 = 0.99332f * b1 + w * 0.0750759f
        b2 = 0.96900f * b2 + w * 0.1538520f
        b3 = 0.86650f * b3 + w * 0.3104856f
        b4 = 0.55000f * b4 + w * 0.5329522f
        b5 = -0.7616f * b5 - w * 0.0168980f
        val out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
        b6 = w * 0.115926f
        return out * 0.11f
    }

    private fun brown(): Float {
        brown = (brown + 0.02f * white()) / 1.02f
        return brown * 3.5f
    }

    fun next(sound: FocusSound): Float = when (sound) {
        FocusSound.OFF -> 0f
        FocusSound.WHITE -> white() * 0.35f
        FocusSound.PINK -> pink()
        FocusSound.BROWN -> brown()
        FocusSound.RAIN -> rain()
        FocusSound.WAVES -> waves()
    }

    private fun rain(): Float {
        // Unas 40 gotas por segundo, de intensidad al azar
        if (random.nextFloat() < 40f / 22_050f) dropEnergy = 0.3f + random.nextFloat() * 0.7f
        dropEnergy *= DROP_DECAY
        // Las gotas son ruido agudo (diferencia entre muestras = filtro pasa altas)
        val w = white()
        dropHigh = w - lastWhite
        lastWhite = w
        return pink() * 0.7f + dropHigh * dropEnergy * 0.35f
    }

    private fun waves(): Float {
        t++
        // Una ola cada ~9 s: sube despacio y rompe
        val phase = (t % WAVE_SAMPLES).toFloat() / WAVE_SAMPLES
        val swell = 0.5f - 0.5f * cos(2f * PI.toFloat() * phase)
        val envelope = 0.15f + 0.85f * swell * swell
        return brown() * envelope * 0.9f + pink() * envelope * 0.25f
    }

    private companion object {
        val DROP_DECAY = exp(-1f / (22_050f * 0.006f))
        const val WAVE_SAMPLES = 22_050L * 9
    }
}
