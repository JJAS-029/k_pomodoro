package com.jjas.labpomodoro.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LabSoundSynthTest {

    @Test
    fun `los sonidos suenan y terminan en silencio`() {
        listOf(LabSoundSynth.clink(), LabSoundSynth.bubbles(), LabSoundSynth.chime()).forEach { samples ->
            assertTrue(samples.maxOf { abs(it.toInt()) } > 2_000)
            assertEquals(0, samples.last().toInt())
        }
    }

    @Test
    fun `el wav lleva su cabecera`() {
        val samples = LabSoundSynth.clink()
        val wav = LabSoundSynth.wav(samples)
        assertEquals("RIFF", String(wav.copyOfRange(0, 4)))
        assertEquals("WAVE", String(wav.copyOfRange(8, 12)))
        assertEquals(44 + samples.size * 2, wav.size)
    }
}
