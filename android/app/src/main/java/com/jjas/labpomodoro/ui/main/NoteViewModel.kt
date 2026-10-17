package com.jjas.labpomodoro.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notas de logro (Pro): al completar un pomodoro se pregunta "¿Qué lograste?". [pending] es el
 * inicio de la sesión a la que irá la nota; null = no hay nada que preguntar.
 */
@HiltViewModel
class NoteViewModel @Inject constructor(
    engine: TimerEngine,
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _pending = MutableStateFlow<Long?>(null)
    val pending: StateFlow<Long?> = _pending.asStateFlow()

    init {
        viewModelScope.launch {
            engine.events.collect { event ->
                // Solo pomodoros completados con la app abierta (no los que se cierran al recuperar un plan)
                if (event is TimerEvent.SessionEnded && event.completed && !event.late && event.session.type == SessionType.WORK) {
                    val prefs = settings.settings.first()
                    if (prefs.isPro && prefs.achievementNotes) _pending.value = event.startedAt.toEpochMilli()
                }
            }
        }
    }

    fun save(text: String) {
        val startedAt = _pending.value ?: return
        _pending.value = null
        val note = text.trim().take(MAX_NOTE)
        if (note.isNotEmpty()) viewModelScope.launch { sessions.setNote(startedAt, note) }
    }

    fun skip() {
        _pending.value = null
    }

    companion object {
        const val MAX_NOTE = 140
    }
}
