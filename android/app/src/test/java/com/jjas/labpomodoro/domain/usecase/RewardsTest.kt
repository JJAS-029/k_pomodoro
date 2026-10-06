package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.Rarity
import com.jjas.labpomodoro.domain.model.ofRarity
import com.jjas.labpomodoro.domain.model.rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RewardsTest {

    @Test
    fun `las rarezas cubren los 118 sin huecos`() {
        assertEquals(30, PeriodicTable.ofRarity(Rarity.BASIC).size)
        assertEquals(60, PeriodicTable.ofRarity(Rarity.RARE).size)
        assertEquals(28, PeriodicTable.ofRarity(Rarity.SYNTHETIC).size)
        // Tecnecio y prometio no existen en la naturaleza aunque sean ligeros
        assertEquals(Rarity.SYNTHETIC, PeriodicTable[43].rarity)
        assertEquals(Rarity.SYNTHETIC, PeriodicTable[61].rarity)
        assertEquals(Rarity.RARE, PeriodicTable[92].rarity)
    }

    @Test
    fun `cada 25 min un basico y cada 60 min un raro`() {
        assertEquals(RewardSchedule.Pending(0, 0), RewardSchedule.pending(24 * 60L, 0, 0))
        assertEquals(RewardSchedule.Pending(1, 0), RewardSchedule.pending(25 * 60L, 0, 0))
        assertEquals(RewardSchedule.Pending(2, 1), RewardSchedule.pending(60 * 60L, 0, 0))
    }

    @Test
    fun `no entrega dos veces lo mismo`() {
        assertEquals(RewardSchedule.Pending(0, 0), RewardSchedule.pending(60 * 60L, 2, 1))
        assertEquals(RewardSchedule.Pending(1, 0), RewardSchedule.pending(75 * 60L, 2, 1))
        // Si se entregó de más (por ejemplo tras borrar historial) no da negativos
        assertTrue(RewardSchedule.pending(0, 5, 5).isEmpty)
    }

    @Test
    fun `el avance hacia la siguiente recompensa`() {
        assertEquals(0.5f, RewardSchedule.progress(30 * 60L, RewardSchedule.RARE_EVERY_SECONDS), 0.001f)
        assertEquals(0f, RewardSchedule.progress(50 * 60L, RewardSchedule.BASIC_EVERY_SECONDS), 0.001f)
    }

    @Test
    fun `el selector solo da elementos de la rareza pedida`() {
        val picker = ElementPicker(Random(7))
        repeat(200) {
            assertEquals(Rarity.BASIC, picker.pick(Rarity.BASIC, emptyMap()).rarity)
            assertEquals(Rarity.RARE, picker.pick(Rarity.RARE, emptyMap()).rarity)
        }
    }

    @Test
    fun `el selector prefiere los que faltan`() {
        val picker = ElementPicker(Random(1))
        // Falta solo el hidrógeno: debe salir la mayoría de las veces
        val owned = (2..30).associateWith { 3 }
        val hydrogen = (1..400).count { picker.pick(Rarity.BASIC, owned).atomicNumber == 1 }
        assertTrue("salió $hydrogen veces", hydrogen > 250)
    }

    @Test
    fun `con todo descubierto prefiere los mas atrasados en maestria`() {
        val picker = ElementPicker(Random(3))
        // Todos con 10 menos el helio, que va en 2
        val counts = (1..30).associateWith { if (it == 2) 2 else 10 }
        val helium = (1..400).count { picker.pick(Rarity.BASIC, counts).atomicNumber == 2 }
        assertTrue("salió $helium veces", helium > 250)
    }

    @Test
    fun `la fusion suma numeros atomicos`() {
        val recipes = Fusion.recipesFor(93, mapOf(1 to 1, 92 to 1))
        assertEquals(1, recipes.size)
        assertEquals("Np", recipes[0].result.symbol)
    }

    @Test
    fun `fusionar un elemento consigo mismo necesita dos unidades`() {
        assertTrue(Fusion.recipesFor(2, mapOf(1 to 1)).isEmpty())
        assertEquals(1, Fusion.recipesFor(2, mapOf(1 to 2)).size)
    }

    @Test
    fun `primero las recetas que gastan lo mas abundante`() {
        val recipes = Fusion.recipesFor(5, mapOf(1 to 1, 4 to 1, 2 to 5, 3 to 4))
        assertEquals(listOf(2 to 3, 1 to 4), recipes.map { it.a.atomicNumber to it.b.atomicNumber })
    }

    @Test
    fun `no hay recetas fuera de la tabla`() {
        assertTrue(Fusion.recipesFor(1, mapOf(1 to 10)).isEmpty())
        assertTrue(Fusion.recipesFor(119, mapOf(59 to 1, 60 to 1)).isEmpty())
    }
}
