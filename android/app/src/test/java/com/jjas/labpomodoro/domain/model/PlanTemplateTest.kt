package com.jjas.labpomodoro.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanTemplateTest {

    @Test
    fun `las plantillas vuelven iguales`() {
        val templates = listOf(
            PlanTemplate("Tesis · 4 h", SessionConfig(4, 50, 10, 20, 3, PlanRounding.WHOLE_POMODOROS), tagId = 7),
            PlanTemplate("Rápido", SessionConfig(totalHours = 1)),
        )
        assertEquals(templates, PlanTemplate.decode(PlanTemplate.encode(templates)))
    }

    @Test
    fun `un nombre con tabulador no rompe el formato`() {
        val decoded = PlanTemplate.decode(PlanTemplate.encode(listOf(PlanTemplate("a\tb\nc", SessionConfig()))))
        assertEquals("a b c", decoded.single().name)
    }

    @Test
    fun `lo que no se entiende se ignora`() {
        assertTrue(PlanTemplate.decode("basura\n\n1\t2").isEmpty())
        assertTrue(PlanTemplate.decode("").isEmpty())
    }
}
