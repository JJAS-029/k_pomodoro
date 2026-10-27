package com.jjas.labpomodoro.ui.components

import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidPaletteVesselTest {

    private fun shapes(owned: Set<String>) = (0L until 200L).flatMap { seed -> (0 until 9).map { LiquidPalette.vessel(seed, it, owned) } }.toSet()

    @Test
    fun `sin compras solo salen los recipientes de siempre`() {
        assertTrue(shapes(emptySet()).all { it in VesselShape.BASE })
    }

    @Test
    fun `lo comprado entra en la rotacion y lo demas no`() {
        val seen = shapes(setOf(VesselShape.KITASATO.name, "HOT_PLATE"))
        assertTrue(VesselShape.KITASATO in seen)
        assertTrue(seen.all { it in VesselShape.BASE || it == VesselShape.KITASATO })
    }
}
