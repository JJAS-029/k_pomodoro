package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementPropertiesTest {

    @Test
    fun `la configuracion lleva superindices`() {
        assertEquals("[He] 2s² 2p²", ElementPropertiesTable.of(6).prettyConfiguration())
        assertEquals("1s¹", ElementPropertiesTable.of(1).prettyConfiguration())
        assertEquals("[Ar] 4s¹ 3d¹⁰", ElementPropertiesTable.of(29).prettyConfiguration())
    }

    @Test
    fun `datos conocidos`() {
        val mercury = ElementPropertiesTable.of(80)
        assertEquals(ElementState.LIQUID, mercury.state)
        assertEquals(-38.8, kelvinToCelsius(mercury.meltingKelvin!!), 0.1)
        assertEquals(100.0, kelvinToCelsius(373.15), 1e-9)
        assertTrue(ElementPropertiesTable.of(1).state.isGas)
    }

    @Test
    fun `los superpesados no inventan datos`() {
        val oganesson = ElementPropertiesTable.of(118)
        assertTrue(oganesson.configurationPredicted)
        assertNull(oganesson.meltingKelvin)
        assertNull(oganesson.density)
    }
}
