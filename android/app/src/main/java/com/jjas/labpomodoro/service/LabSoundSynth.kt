package com.jjas.labpomodoro.service

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sonidos de laboratorio (Pro), sintetizados: no hacen falta archivos de audio. Cada uno es un
 * WAV mono de 16 bits listo para el SoundPool.
 */
object LabSoundSynth {

    const val SAMPLE_RATE = 44_100

    /** Dos tintineos de vidrio (ting-ting): empieza el trabajo. */
    fun clink(): ShortArray = render(0.9f) { t ->
        glass(t, 0f) + glass(t, 0.22f) * 0.8f
    }

    /** Burbujeo: cinco burbujas que suben de tono, como un matraz al hervir suave. Descanso corto. */
    fun bubbles(): ShortArray = render(1.3f) { t ->
        (0 until 5).sumOf { i ->
            val start = i * 0.22f + (i % 2) * 0.05f
            bubble(t - start, 320f + i * 70f).toDouble()
        }.toFloat()
    }

    /** Acorde de copas de cristal (do, mi, sol) que se apaga despacio. Descanso largo o fin del plan. */
    fun chime(): ShortArray = render(2.4f) { t ->
        listOf(1046.5f, 1318.5f, 1568f).withIndex().sumOf { (i, f) ->
            val local = t - i * 0.08f
            if (local < 0f) 0.0 else (sin(2 * PI * f * local) * exp(-local * 1.6) * attack(local)).toDouble()
        }.toFloat() / 3f
    }

    /** WAV (cabecera RIFF + PCM de 16 bits) para guardarlo y cargarlo en el SoundPool. */
    fun wav(samples: ShortArray): ByteArray {
        val data = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { data.putShort(it) }
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt(36 + samples.size * 2); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(1)
            putInt(SAMPLE_RATE); putInt(SAMPLE_RATE * 2); putShort(2); putShort(16)
            put("data".toByteArray()); putInt(samples.size * 2)
        }
        return ByteArrayOutputStream().apply { write(header.array()); write(data.array()) }.toByteArray()
    }

    /** Un golpe de vidrio: parciales inarmónicos altos que se apagan rápido. */
    private fun glass(t: Float, start: Float): Float {
        val local = t - start
        if (local < 0f) return 0f
        val partials = listOf(2637f to 1f, 3951f to 0.5f, 5274f to 0.3f, 6620f to 0.15f)
        return partials.sumOf { (f, a) -> (a * sin(2 * PI * f * local) * exp(-local * (6.0 + f / 1500.0))).toDouble() }.toFloat() *
            attack(local) * 0.5f
    }

    /** Una burbuja: tono que sube rápido y se apaga, como al reventar en la superficie. */
    private fun bubble(local: Float, base: Float): Float {
        if (local < 0f || local > 0.18f) return 0f
        val freq = base * (1f + local * 9f)
        return (sin(2 * PI * freq * local) * exp(-local * 22.0)).toFloat() * attack(local) * 0.6f
    }

    /** Entrada suave de 3 ms para que no truene. */
    private fun attack(local: Float) = (local / 0.003f).coerceAtMost(1f)

    private fun render(seconds: Float, wave: (Float) -> Float): ShortArray =
        ShortArray((seconds * SAMPLE_RATE).toInt()) { i ->
            val t = i.toFloat() / SAMPLE_RATE
            // Se apaga al final para no cortar en seco
            val tail = ((seconds - t) / 0.05f).coerceIn(0f, 1f)
            (wave(t).coerceIn(-1f, 1f) * tail * Short.MAX_VALUE * 0.8f).toInt().toShort()
        }
}
