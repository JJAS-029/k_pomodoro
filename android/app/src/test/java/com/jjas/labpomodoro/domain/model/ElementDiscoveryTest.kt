package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementDiscoveryTest {

    @Test
    fun everyElementHasConsistentData() {
        for (z in 1..PeriodicTable.SIZE) {
            val d = ElementDiscoveries[z]
            assertTrue("Z=$z discoverers en blanco", d.discoverers.isNotBlank())
            assertTrue("Z=$z discoverers demasiado largo", d.discoverers.length <= 60)
            assertTrue("Z=$z debe tener exactamente uno de year/era", (d.year != null) xor (d.era != null))
            d.year?.let { assertTrue("Z=$z año fuera de rango: $it", it in 1000..2025) }
            if (d.year == null) assertNull("Z=$z antiguo con país", d.country)
        }
    }

    @Test
    fun knownDiscoveries() {
        val h = ElementDiscoveries[1]
        assertEquals(1766, h.year)
        assertTrue(h.discoverers.contains("Cavendish"))
        assertEquals(1774, ElementDiscoveries[8].year)
        assertTrue(ElementDiscoveries[118].year!! >= 2002)
        assertEquals("Japón", ElementDiscoveries[113].country)
        assertNull(ElementDiscoveries[79].year)
    }
}
