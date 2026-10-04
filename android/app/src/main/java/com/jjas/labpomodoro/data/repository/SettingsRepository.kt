package com.jjas.labpomodoro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Preferencias del usuario en DataStore. Lo que no esté guardado toma el valor por defecto de [AppSettings]. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toAppSettings() }

    suspend fun updateSession(transform: (SessionConfig) -> SessionConfig) {
        dataStore.edit { prefs ->
            val updated = transform(prefs.toAppSettings().session).normalized()
            prefs[Keys.TOTAL_HOURS] = updated.totalHours
            prefs[Keys.WORK_MINUTES] = updated.workMinutes
            prefs[Keys.SHORT_BREAK_MINUTES] = updated.shortBreakMinutes
            prefs[Keys.LONG_BREAK_MINUTES] = updated.longBreakMinutes
            prefs[Keys.POMODOROS_UNTIL_LONG] = updated.pomodorosUntilLong
            prefs[Keys.ROUNDING] = updated.rounding.name
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) = dataStore.edit { it[Keys.SOUND] = enabled }

    suspend fun setVibrationEnabled(enabled: Boolean) = dataStore.edit { it[Keys.VIBRATION] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) = dataStore.edit { it[Keys.KEEP_SCREEN_ON] = enabled }

    private fun Preferences.toAppSettings(): AppSettings {
        val defaults = AppSettings()
        val session = SessionConfig(
            totalHours = this[Keys.TOTAL_HOURS] ?: defaults.session.totalHours,
            workMinutes = this[Keys.WORK_MINUTES] ?: defaults.session.workMinutes,
            shortBreakMinutes = this[Keys.SHORT_BREAK_MINUTES] ?: defaults.session.shortBreakMinutes,
            longBreakMinutes = this[Keys.LONG_BREAK_MINUTES] ?: defaults.session.longBreakMinutes,
            pomodorosUntilLong = this[Keys.POMODOROS_UNTIL_LONG] ?: defaults.session.pomodorosUntilLong,
            rounding = this[Keys.ROUNDING]
                ?.let { name -> PlanRounding.entries.firstOrNull { it.name == name } }
                ?: defaults.session.rounding,
        ).normalized()
        return AppSettings(
            session = session,
            soundEnabled = this[Keys.SOUND] ?: defaults.soundEnabled,
            vibrationEnabled = this[Keys.VIBRATION] ?: defaults.vibrationEnabled,
            keepScreenOn = this[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
        )
    }

    private object Keys {
        val TOTAL_HOURS = intPreferencesKey("total_hours")
        val WORK_MINUTES = intPreferencesKey("work_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val POMODOROS_UNTIL_LONG = intPreferencesKey("pomodoros_until_long")
        val ROUNDING = stringPreferencesKey("plan_rounding")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    }
}
