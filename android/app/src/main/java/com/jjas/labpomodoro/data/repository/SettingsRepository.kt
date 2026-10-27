package com.jjas.labpomodoro.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.FocusSound
import com.jjas.labpomodoro.domain.model.LabPalette
import com.jjas.labpomodoro.domain.model.LabBackground
import com.jjas.labpomodoro.domain.model.BurnerStyle
import com.jjas.labpomodoro.domain.model.PlanTemplate
import com.jjas.labpomodoro.domain.model.FlameColor
import com.jjas.labpomodoro.domain.model.GlassTint
import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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

    suspend fun setFocusDnd(enabled: Boolean) = dataStore.edit { it[Keys.FOCUS_DND] = enabled }

    suspend fun setLabSounds(enabled: Boolean) = dataStore.edit { it[Keys.LAB_SOUNDS] = enabled }

    suspend fun setAchievementNotes(enabled: Boolean) = dataStore.edit { it[Keys.ACHIEVEMENT_NOTES] = enabled }

    /**
     * Marca el reto [key] como reclamado. Devuelve false si ya lo estaba, así el premio se entrega
     * una sola vez. Solo se conservan los de la semana [week] (las claves empiezan con ella).
     */
    suspend fun claimChallenge(key: String, week: String): Boolean {
        var claimed = false
        dataStore.edit {
            val current = it[Keys.CLAIMED_CHALLENGES].orEmpty().filter { k -> k.startsWith(week) }.toSet()
            if (key !in current) {
                it[Keys.CLAIMED_CHALLENGES] = current + key
                claimed = true
            }
        }
        return claimed
    }

    /** [day] null quita la cuenta regresiva. */
    suspend fun setCountdown(name: String, day: Long?) = dataStore.edit {
        it[Keys.COUNTDOWN_NAME] = name
        if (day == null) it.remove(Keys.COUNTDOWN_DAY) else it[Keys.COUNTDOWN_DAY] = day
    }

    suspend fun setAmbientMode(enabled: Boolean) = dataStore.edit { it[Keys.AMBIENT_MODE] = enabled }

    suspend fun setAmbientDelayMinutes(minutes: Int) = dataStore.edit {
        it[Keys.AMBIENT_DELAY] = minutes.takeIf { m -> m in AppSettings.AMBIENT_DELAYS } ?: AppSettings.AMBIENT_DELAYS.first()
    }

    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setPureBlack(enabled: Boolean) = dataStore.edit { it[Keys.PURE_BLACK] = enabled }

    suspend fun setGuideSeen() = dataStore.edit { it[Keys.GUIDE_SEEN] = true }

    suspend fun setExactAlarmAsked() = dataStore.edit { it[Keys.EXACT_ALARM_ASKED] = true }

    suspend fun setLabPalette(palette: LabPalette) = dataStore.edit { it[Keys.LAB_PALETTE] = palette.name }

    suspend fun setGlassTint(tint: GlassTint) = dataStore.edit { it[Keys.GLASS_TINT] = tint.name }

    suspend fun setSeasonalThemes(enabled: Boolean) = dataStore.edit { it[Keys.SEASONAL] = enabled }

    suspend fun setDailyGoal(goal: Int) = dataStore.edit { it[Keys.DAILY_GOAL] = goal }

    suspend fun setWeeklyGoal(goal: Int) = dataStore.edit { it[Keys.WEEKLY_GOAL] = goal }

    suspend fun setGoalCelebratedDay(day: Long) = dataStore.edit { it[Keys.GOAL_CELEBRATED_DAY] = day }

    suspend fun setPlanTemplates(templates: List<PlanTemplate>) = dataStore.edit {
        it[Keys.PLAN_TEMPLATES] = PlanTemplate.encode(templates.take(PlanTemplate.MAX_TEMPLATES))
    }

    suspend fun setFlameColor(color: FlameColor) = dataStore.edit { it[Keys.FLAME_COLOR] = color.name }

    suspend fun setLabBackground(background: LabBackground) = dataStore.edit { it[Keys.LAB_BACKGROUND] = background.name }

    /** Suma [name] a la cristalería comprada (se guarda separada por comas para que viaje en el respaldo). */
    suspend fun addGlassware(name: String) = dataStore.edit { prefs ->
        val owned = prefs[Keys.GLASSWARE].orEmpty().split(',').filter { it.isNotBlank() }.toSet()
        prefs[Keys.GLASSWARE] = (owned + name).sorted().joinToString(",")
    }

    suspend fun setBurner(burner: BurnerStyle) = dataStore.edit { it[Keys.BURNER] = burner.name }

    suspend fun setVesselElement(atomicNumber: Int) = dataStore.edit { it[Keys.VESSEL_ELEMENT] = atomicNumber }

    suspend fun setFocusSound(sound: FocusSound) = dataStore.edit { it[Keys.FOCUS_SOUND] = sound.name }

    suspend fun setTableCelebrated() = dataStore.edit { it[Keys.TABLE_CELEBRATED] = true }

    suspend fun setMasteryCelebrated(level: Int) = dataStore.edit { it[Keys.MASTERY_CELEBRATED] = level }

    /** Suma [medals] a las ya celebradas (se guardan como texto para que viajen en el respaldo). */
    suspend fun addMedalsSeen(medals: Collection<String>) = dataStore.edit { prefs ->
        val seen = prefs[Keys.MEDALS_SEEN].toMedalSet() + medals
        prefs[Keys.MEDALS_SEEN] = seen.sorted().joinToString(",")
    }

    suspend fun setReminder(enabled: Boolean) = dataStore.edit { it[Keys.REMINDER] = enabled }

    suspend fun setReminderHour(hour: Int) = dataStore.edit { it[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23) }

    suspend fun setPlacesEnabled(enabled: Boolean) = dataStore.edit { it[Keys.PLACES] = enabled }

    suspend fun setSelectedTag(tagId: Long?) = dataStore.edit {
        if (tagId == null) it.remove(Keys.SELECTED_TAG) else it[Keys.SELECTED_TAG] = tagId
    }

    suspend fun setFocusVolume(volume: Float) = dataStore.edit { it[Keys.FOCUS_VOLUME] = volume.coerceIn(0f, 1f) }

    /** Interruptor "Activar Pro (solo pruebas)"; en release se ignora. */
    suspend fun setPro(enabled: Boolean) = dataStore.edit { it[Keys.PRO] = enabled }

    /** Lo llama la tienda al confirmar o revocar la compra. */
    suspend fun setProPurchased(purchased: Boolean) = dataStore.edit { it[Keys.PRO_PURCHASED] = purchased }

    /** Pro para los revisores de Google Play (con el código de revisión). */
    suspend fun setProReview() = dataStore.edit { it[Keys.PRO_REVIEW] = true }

    /** Ajustes para el respaldo en la nube, por nombre de clave. */
    suspend fun exportForBackup(): Map<String, Any> =
        settingsPrefs().asMap()
            .filterKeys { it.name !in NOT_BACKED_UP }
            .mapKeys { it.key.name }

    /** Aplica los ajustes de un respaldo; la compra de Pro y el permiso de lugares no viajan. */
    suspend fun importFromBackup(values: Map<String, Any>) = dataStore.edit { prefs ->
        values.filterKeys { it !in NOT_BACKED_UP }.forEach { (name, value) ->
            when (value) {
                is Boolean -> prefs[booleanPreferencesKey(name)] = value
                is Int -> prefs[intPreferencesKey(name)] = value
                is Long -> prefs[longPreferencesKey(name)] = value
                is Float -> prefs[floatPreferencesKey(name)] = value
                is String -> prefs[stringPreferencesKey(name)] = value
            }
        }
    }

    private suspend fun settingsPrefs(): Preferences = dataStore.data.first()

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
            focusDnd = this[Keys.FOCUS_DND] ?: defaults.focusDnd,
            labSounds = this[Keys.LAB_SOUNDS] ?: defaults.labSounds,
            achievementNotes = this[Keys.ACHIEVEMENT_NOTES] ?: defaults.achievementNotes,
            countdownName = this[Keys.COUNTDOWN_NAME] ?: defaults.countdownName,
            countdownDay = this[Keys.COUNTDOWN_DAY],
            ambientMode = this[Keys.AMBIENT_MODE] ?: defaults.ambientMode,
            ambientDelayMinutes = this[Keys.AMBIENT_DELAY]?.takeIf { it in AppSettings.AMBIENT_DELAYS }
                ?: defaults.ambientDelayMinutes,
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            pureBlack = this[Keys.PURE_BLACK] ?: defaults.pureBlack,
            proPurchased = this[Keys.PRO_PURCHASED] ?: defaults.proPurchased,
            proTesting = BuildConfig.DEBUG && (this[Keys.PRO] ?: defaults.proTesting),
            proReview = this[Keys.PRO_REVIEW] ?: defaults.proReview,
            guideSeen = this[Keys.GUIDE_SEEN] ?: defaults.guideSeen,
            exactAlarmAsked = this[Keys.EXACT_ALARM_ASKED] ?: defaults.exactAlarmAsked,
            labPalette = this[Keys.LAB_PALETTE]?.let { name -> LabPalette.entries.firstOrNull { it.name == name } } ?: defaults.labPalette,
            dailyGoal = this[Keys.DAILY_GOAL] ?: defaults.dailyGoal,
            weeklyGoal = this[Keys.WEEKLY_GOAL] ?: defaults.weeklyGoal,
            goalCelebratedDay = this[Keys.GOAL_CELEBRATED_DAY] ?: defaults.goalCelebratedDay,
            planTemplates = this[Keys.PLAN_TEMPLATES]?.let(PlanTemplate::decode),
            seasonalThemes = this[Keys.SEASONAL] ?: defaults.seasonalThemes,
            glassTint = this[Keys.GLASS_TINT]?.let { name -> GlassTint.entries.firstOrNull { it.name == name } } ?: defaults.glassTint,
            labBackground = this[Keys.LAB_BACKGROUND]?.let { name -> LabBackground.entries.firstOrNull { it.name == name } } ?: defaults.labBackground,
            ownedGlassware = this[Keys.GLASSWARE].orEmpty().split(',').filter { it.isNotBlank() }.toSet(),
            burner = this[Keys.BURNER]?.let { name -> BurnerStyle.entries.firstOrNull { it.name == name } } ?: defaults.burner,
            flameColor = this[Keys.FLAME_COLOR]?.let { name -> FlameColor.entries.firstOrNull { it.name == name } } ?: defaults.flameColor,
            vesselElement = this[Keys.VESSEL_ELEMENT] ?: defaults.vesselElement,
            focusSound = this[Keys.FOCUS_SOUND]?.let { name -> FocusSound.entries.firstOrNull { it.name == name } } ?: defaults.focusSound,
            focusVolume = this[Keys.FOCUS_VOLUME] ?: defaults.focusVolume,
            placesEnabled = this[Keys.PLACES] ?: defaults.placesEnabled,
            selectedTagId = this[Keys.SELECTED_TAG],
            tableCelebrated = this[Keys.TABLE_CELEBRATED] ?: defaults.tableCelebrated,
            masteryCelebrated = this[Keys.MASTERY_CELEBRATED] ?: defaults.masteryCelebrated,
            medalsSeen = this[Keys.MEDALS_SEEN].toMedalSet(),
            reminderEnabled = this[Keys.REMINDER] ?: defaults.reminderEnabled,
            reminderHour = this[Keys.REMINDER_HOUR] ?: defaults.reminderHour,
        )
    }

    private fun String?.toMedalSet(): Set<String> = this?.split(',')?.filter { it.isNotBlank() }?.toSet().orEmpty()

    private companion object {
        /** De cada teléfono: la compra la restaura Google Play y el permiso de ubicación se pide de nuevo. */
        val NOT_BACKED_UP = setOf("pro_unlocked", "pro_purchased", "pro_review", "places_enabled")
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
        val FOCUS_DND = booleanPreferencesKey("focus_dnd")
        val LAB_SOUNDS = booleanPreferencesKey("lab_sounds")
        val ACHIEVEMENT_NOTES = booleanPreferencesKey("achievement_notes")
        val CLAIMED_CHALLENGES = stringSetPreferencesKey("claimed_challenges")
        val COUNTDOWN_NAME = stringPreferencesKey("countdown_name")
        val COUNTDOWN_DAY = longPreferencesKey("countdown_day")
        val AMBIENT_MODE = booleanPreferencesKey("ambient_mode")
        val AMBIENT_DELAY = intPreferencesKey("ambient_delay_minutes")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val PRO = booleanPreferencesKey("pro_unlocked")
        val PRO_REVIEW = booleanPreferencesKey("pro_review")
        val GUIDE_SEEN = booleanPreferencesKey("guide_seen")
        val EXACT_ALARM_ASKED = booleanPreferencesKey("exact_alarm_asked")
        val LAB_PALETTE = stringPreferencesKey("lab_palette")
        val GLASS_TINT = stringPreferencesKey("glass_tint")
        val SEASONAL = booleanPreferencesKey("seasonal_themes")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val WEEKLY_GOAL = intPreferencesKey("weekly_goal")
        val GOAL_CELEBRATED_DAY = longPreferencesKey("goal_celebrated_day")
        val PLAN_TEMPLATES = stringPreferencesKey("plan_templates")
        val FLAME_COLOR = stringPreferencesKey("flame_color")
        val LAB_BACKGROUND = stringPreferencesKey("lab_background")
        val GLASSWARE = stringPreferencesKey("glassware")
        val BURNER = stringPreferencesKey("burner")
        val PRO_PURCHASED = booleanPreferencesKey("pro_purchased")
        val VESSEL_ELEMENT = intPreferencesKey("vessel_element")
        val FOCUS_SOUND = stringPreferencesKey("focus_sound")
        val FOCUS_VOLUME = floatPreferencesKey("focus_volume")
        val PLACES = booleanPreferencesKey("places_enabled")
        val SELECTED_TAG = longPreferencesKey("selected_tag")
        val TABLE_CELEBRATED = booleanPreferencesKey("table_celebrated")
        val MASTERY_CELEBRATED = intPreferencesKey("mastery_celebrated")
        val MEDALS_SEEN = stringPreferencesKey("medals_seen")
        val REMINDER = booleanPreferencesKey("streak_reminder")
        val REMINDER_HOUR = intPreferencesKey("streak_reminder_hour")
    }
}
