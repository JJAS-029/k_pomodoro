package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MasteryTest {

    private val iron = PeriodicTable[26]
    private val neptunium = PeriodicTable[93]

    @Test
    fun `niveles de un elemento natural`() {
        assertEquals(Mastery.NONE, MasteryRules.level(iron, 0))
        assertEquals(Mastery.DISCOVERED, MasteryRules.level(iron, 4))
        assertEquals(Mastery.BRONZE, MasteryRules.level(iron, 5))
        assertEquals(Mastery.SILVER, MasteryRules.level(iron, 10))
        assertEquals(Mastery.GOLD, MasteryRules.level(iron, 15))
        assertEquals(Mastery.GOLD, MasteryRules.level(iron, 40))
    }

    @Test
    fun `los sinteticos tienen metas mas cortas`() {
        assertEquals(Mastery.BRONZE, MasteryRules.level(neptunium, 2))
        assertEquals(Mastery.SILVER, MasteryRules.level(neptunium, 4))
        assertEquals(Mastery.GOLD, MasteryRules.level(neptunium, 6))
    }

    @Test
    fun `lo que falta para el siguiente nivel`() {
        assertEquals(Mastery.BRONZE to 4, MasteryRules.next(iron, 1))
        assertEquals(Mastery.GOLD to 3, MasteryRules.next(iron, 12))
        assertNull(MasteryRules.next(iron, 15))
    }

    @Test
    fun `la tabla sube de nivel cuando todos lo alcanzan`() {
        val allBronze = PeriodicTable.elements.associate { it.atomicNumber to MasteryRules.thresholds(it)[0] }
        assertEquals(Mastery.BRONZE, MasteryRules.tableLevel(allBronze))
        // Basta uno atrasado para que la tabla no suba
        assertEquals(Mastery.DISCOVERED, MasteryRules.tableLevel(allBronze + (1 to 1)))
        assertEquals(Mastery.NONE, MasteryRules.tableLevel(allBronze - 118))
    }
}
