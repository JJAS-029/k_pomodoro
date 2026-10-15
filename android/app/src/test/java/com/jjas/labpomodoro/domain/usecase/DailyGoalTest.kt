package com.jjas.labpomodoro.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyGoalTest {

    @Test
    fun `cuenta hasta hoy si hoy ya se cumplio`() {
        assertEquals(3, DailyGoal.streak(mapOf(10L to 4, 9L to 5, 8L to 4, 7L to 1), today = 10, goal = 4))
    }

    @Test
    fun `hoy sin cumplir aun no rompe la racha`() {
        assertEquals(2, DailyGoal.streak(mapOf(10L to 1, 9L to 4, 8L to 4), today = 10, goal = 4))
    }

    @Test
    fun `un dia sin cumplir la corta`() {
        assertEquals(1, DailyGoal.streak(mapOf(10L to 4, 9L to 2, 8L to 4), today = 10, goal = 4))
    }

    @Test
    fun `sin meta no hay racha`() {
        assertEquals(0, DailyGoal.streak(mapOf(10L to 9), today = 10, goal = 0))
    }
}
