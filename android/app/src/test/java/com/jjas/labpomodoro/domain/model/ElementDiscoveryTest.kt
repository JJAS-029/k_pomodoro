package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementDiscoveryTest {

    @Test
    fun everyElementHasConsistentData() {
        for (data in listOf(ElementDataXml.spanish, ElementDataXml.english)) {
            val discoverers = data.getValue("element_discoverers")
            val eras = data.getValue("element_discovery_eras")
            val countries = data.getValue("element_discovery_countries")
            assertEquals(PeriodicTable.SIZE, discoverers.size)
            assertEquals(PeriodicTable.SIZE, eras.size)
            assertEquals(PeriodicTable.SIZE, countries.size)
            for (z in 1..PeriodicTable.SIZE) {
                val year = ElementDiscoveries.year(z)
                val d = discoverers[z - 1]
                assertTrue("Z=$z discoverers en blanco", d.isNotBlank())
                if (data === ElementDataXml.spanish) assertTrue("Z=$z discoverers demasiado largo", d.length <= 60)
                assertTrue("Z=$z debe tener exactamente uno de year/era", (year != null) xor eras[z - 1].isNotEmpty())
                year?.let { assertTrue("Z=$z año fuera de rango: $it", it in 1000..2025) }
                if (year == null) assertTrue("Z=$z antiguo con país", countries[z - 1].isEmpty())
            }
        }
    }

    @Test
    fun knownDiscoveries() {
        val es = ElementDataXml.spanish
        assertEquals(1766, ElementDiscoveries.year(1))
        assertTrue(es.getValue("element_discoverers")[0].contains("Cavendish"))
        assertEquals(1774, ElementDiscoveries.year(8))
        assertTrue(ElementDiscoveries.year(118)!! >= 2002)
        assertEquals("Japón", es.getValue("element_discovery_countries")[112])
        assertNull(ElementDiscoveries.year(79))
    }
}
