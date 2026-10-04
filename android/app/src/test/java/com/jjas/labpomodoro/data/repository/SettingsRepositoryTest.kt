package com.jjas.labpomodoro.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.PlanRounding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsRepositoryTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.repository() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) {
            tmp.newFile("settings.preferences_pb").also { it.delete() }
        }
    )

    @Test
    fun `sin datos guardados devuelve los valores por defecto`() = runTest {
        assertEquals(AppSettings(), repository().settings.first())
    }

    @Test
    fun `guarda la configuracion de sesion y el modo de redondeo`() = runTest {
        val repo = repository()
        repo.updateSession { it.copy(totalHours = 3, workMinutes = 50, rounding = PlanRounding.WHOLE_POMODOROS) }

        val session = repo.settings.first().session
        assertEquals(3, session.totalHours)
        assertEquals(50, session.workMinutes)
        assertEquals(PlanRounding.WHOLE_POMODOROS, session.rounding)
    }

    @Test
    fun `los valores fuera de rango se guardan ya corregidos`() = runTest {
        val repo = repository()
        repo.updateSession { it.copy(workMinutes = 1000) }
        assertEquals(120, repo.settings.first().session.workMinutes)
    }

    @Test
    fun `guarda los avisos`() = runTest {
        val repo = repository()
        repo.setSoundEnabled(false)
        repo.setKeepScreenOn(true)

        val settings = repo.settings.first()
        assertFalse(settings.soundEnabled)
        assertEquals(true, settings.keepScreenOn)
    }
}
