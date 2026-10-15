package com.jjas.labpomodoro.service

import android.content.Context
import androidx.core.content.edit
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.data.repository.TagRepository
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerEvent
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Etiquetas: al iniciar un plan toma la etiqueta elegida y se la asigna a cada sesión de ese plan
 * conforme se guarda. Se puede cambiar a medio plan; cuenta desde la siguiente sesión que termine.
 */
@Singleton
class TagTracker @Inject constructor(
    @ApplicationContext context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val tags: TagRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _current = MutableStateFlow<Long?>(null)

    /** Etiqueta del plan en curso (null = sin etiqueta o sin plan). */
    val current: StateFlow<Long?> = _current

    /**
     * La etiqueta del plan, guardada con su semilla: si Android cierra la app y el motor recupera
     * ese mismo plan, las sesiones siguientes conservan la etiqueta.
     */
    private val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start() {
        scope.launch {
            engine.state.collect { state ->
                val active = state as? TimerState.Active
                if (active == null) {
                    _current.value = null
                    return@collect
                }
                if (_current.value == null && saved.getLong(KEY_SEED, 0L) == active.planSeed) {
                    _current.value = saved.getLong(KEY_TAG, 0L).takeIf { it > 0 }
                }
            }
        }
        scope.launch {
            engine.events.collect { event ->
                when (event) {
                    TimerEvent.PlanStarted -> {
                        val tag = settings.settings.first().selectedTagId
                        remember((engine.state.value as? TimerState.Active)?.planSeed, tag)
                    }
                    is TimerEvent.SessionEnded -> _current.value?.let { tags.tagSession(event.startedAt.toEpochMilli(), it) }
                }
            }
        }
    }

    /** Cambia la etiqueta del plan en curso (y la deja elegida para los siguientes). */
    fun setForCurrentPlan(tagId: Long?) {
        scope.launch {
            settings.setSelectedTag(tagId)
            remember((engine.state.value as? TimerState.Active)?.planSeed, tagId)
        }
    }

    private fun remember(seed: Long?, tagId: Long?) {
        _current.value = tagId
        saved.edit {
            clear()
            if (seed != null && tagId != null) putLong(KEY_SEED, seed).putLong(KEY_TAG, tagId)
        }
    }

    private companion object {
        const val PREFS = "tag_tracker"
        const val KEY_SEED = "plan_seed"
        const val KEY_TAG = "tag_id"
    }
}
