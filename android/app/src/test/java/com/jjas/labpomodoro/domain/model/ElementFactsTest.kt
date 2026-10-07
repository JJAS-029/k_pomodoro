package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElementFactsTest {

    private fun facts(data: Map<String, List<String>>): List<Pair<Int, ElementFact>> {
        val descriptions = data.getValue("element_fact_descriptions")
        val uses = data.getValue("element_fact_uses")
        return descriptions.indices.map { i -> (i + 1) to ElementFact(descriptions[i], uses.getOrElse(i) { "" }) }
    }

    private val allFacts = facts(ElementDataXml.spanish) + facts(ElementDataXml.english)

    @Test
    fun hayUnDatoParaCadaElemento() {
        assertTrue(PeriodicTable.SIZE == 118)
        for (data in listOf(ElementDataXml.spanish, ElementDataXml.english)) {
            assertEquals(PeriodicTable.SIZE, data.getValue("element_names").size)
            assertEquals(PeriodicTable.SIZE, data.getValue("element_fact_descriptions").size)
            assertEquals(PeriodicTable.SIZE, data.getValue("element_fact_uses").size)
            assertEquals(PeriodicTable.SIZE, data.getValue("element_look_origins").size)
        }
    }

    @Test
    fun ningunTextoEstaVacio() {
        allFacts.forEach { (z, fact) ->
            assertTrue("Descripción vacía en Z=$z", fact.description.isNotBlank())
            assertTrue("Usos vacíos en Z=$z", fact.uses.isNotBlank())
        }
    }

    // El límite es para los textos originales; las traducciones pueden quedar un poco más largas
    @Test
    fun ningunTextoEsDemasiadoLargo() {
        facts(ElementDataXml.spanish).forEach { (z, fact) ->
            assertTrue("Descripción larga en Z=$z (${fact.description.length})", fact.description.length <= 180)
            assertTrue("Usos largos en Z=$z (${fact.uses.length})", fact.uses.length <= 180)
        }
    }
}
