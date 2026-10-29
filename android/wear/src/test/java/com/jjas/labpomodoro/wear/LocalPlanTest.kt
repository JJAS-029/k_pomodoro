package com.jjas.labpomodoro.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalPlanTest {

    private val plan = listOf("WORK" to 1500, "SHORT_BREAK" to 300, "WORK" to 1500)

    @Test
    fun `ubica la sesion en curso y lo que lleva`() {
        assertEquals(0 to 0L, LocalPlan.position(plan, 0))
        assertEquals(1 to 60_000L, LocalPlan.position(plan, 1_560_000))
        assertEquals(2 to 0L, LocalPlan.position(plan, 1_800_000))
    }

    @Test
    fun `al pasar el final el plan termino`() {
        assertNull(LocalPlan.position(plan, 3_300_000))
    }
}
