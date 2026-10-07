package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodicTableTest {

    @Test
    fun `tiene los 118 elementos en orden`() {
        assertEquals(PeriodicTable.SIZE, PeriodicTable.elements.size)
        PeriodicTable.elements.forEachIndexed { index, element ->
            assertEquals(index + 1, element.atomicNumber)
        }
    }

    @Test
    fun `los simbolos no se repiten`() {
        assertEquals(PeriodicTable.SIZE, PeriodicTable.elements.map { it.symbol }.toSet().size)
    }

    @Test
    fun `cada grupo y periodo tiene una sola casilla`() {
        val positions = PeriodicTable.elements.filter { it.group != null }.map { it.group to it.period }
        assertEquals(positions.size, positions.toSet().size)
        assertTrue(positions.all { (group, period) -> group in 1..18 && period in 1..7 })
    }

    @Test
    fun `lantanidos y actinidos van en el bloque f`() {
        (57..71).forEach {
            assertEquals(ElementCategory.LANTHANIDE, PeriodicTable[it].category)
            assertNull(PeriodicTable[it].group)
            assertEquals(6, PeriodicTable[it].period)
        }
        (89..103).forEach {
            assertEquals(ElementCategory.ACTINIDE, PeriodicTable[it].category)
            assertNull(PeriodicTable[it].group)
            assertEquals(7, PeriodicTable[it].period)
        }
    }

    @Test
    fun `algunos elementos conocidos`() {
        assertEquals("Fe", PeriodicTable[26].symbol)
        assertEquals("Oro", ElementDataXml.spanish.getValue("element_names")[79 - 1])
        assertEquals(18, PeriodicTable[118].group)
    }
}
