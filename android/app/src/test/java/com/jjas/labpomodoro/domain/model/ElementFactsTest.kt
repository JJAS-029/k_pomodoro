package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test

class ElementFactsTest {

    private val allFacts = (1..PeriodicTable.SIZE).map { it to ElementFacts[it] }

    @Test
    fun hayUnDatoParaCadaElemento() {
        assertTrue(PeriodicTable.SIZE == 118)
        assertTrue(allFacts.size == PeriodicTable.SIZE)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun noHayDatosMasAllaDelUltimoElemento() {
        ElementFacts[PeriodicTable.SIZE + 1]
    }

    @Test
    fun ningunTextoEstaVacio() {
        allFacts.forEach { (z, fact) ->
            assertTrue("Descripción vacía en Z=$z", fact.description.isNotBlank())
            assertTrue("Usos vacíos en Z=$z", fact.uses.isNotBlank())
        }
    }

    @Test
    fun ningunTextoEsDemasiadoLargo() {
        allFacts.forEach { (z, fact) ->
            assertTrue("Descripción larga en Z=$z (${fact.description.length})", fact.description.length <= 180)
            assertTrue("Usos largos en Z=$z (${fact.uses.length})", fact.uses.length <= 180)
        }
    }
}
