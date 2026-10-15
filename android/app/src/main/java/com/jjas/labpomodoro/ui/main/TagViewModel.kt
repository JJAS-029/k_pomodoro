package com.jjas.labpomodoro.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.data.repository.TagRepository
import com.jjas.labpomodoro.domain.model.TagColors
import com.jjas.labpomodoro.service.TagTracker
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagUi(val id: Long, val name: String, val color: Int)

/**
 * Etiquetas para la pantalla principal. [selected] es la del próximo plan; [current], la del plan
 * en curso. Sin Pro caben [TagColors.FREE_LIMIT].
 */
data class TagsUi(
    val tags: List<TagUi> = emptyList(),
    val selected: Long? = null,
    val current: Long? = null,
    val isPro: Boolean = false,
) {
    val canAdd: Boolean get() = isPro || tags.size < TagColors.FREE_LIMIT
    fun find(id: Long?): TagUi? = id?.let { wanted -> tags.firstOrNull { it.id == wanted } }
}

@HiltViewModel
class TagViewModel @Inject constructor(
    private val repository: TagRepository,
    private val settings: SettingsRepository,
    private val tracker: TagTracker,
    private val engine: TimerEngine,
) : ViewModel() {

    val state: StateFlow<TagsUi> = combine(repository.tags, settings.settings, tracker.current) { tags, prefs, current ->
        val list = tags.map { TagUi(it.id, it.name, it.color) }
        TagsUi(
            tags = list,
            // Si la etiqueta elegida se borró, queda sin etiqueta
            selected = prefs.selectedTagId?.takeIf { id -> list.any { it.id == id } },
            current = current?.takeIf { id -> list.any { it.id == id } },
            isPro = prefs.isPro,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUi())

    /** Elige la etiqueta: con plan en curso cuenta desde la siguiente sesión que termine. */
    fun select(tagId: Long?) {
        if (engine.state.value is TimerState.Active) {
            tracker.setForCurrentPlan(tagId)
        } else {
            viewModelScope.launch { settings.setSelectedTag(tagId) }
        }
    }

    /** Crea la etiqueta y la deja elegida. */
    fun create(name: String, color: Int) {
        if (name.isBlank() || !state.value.canAdd) return
        viewModelScope.launch {
            val id = repository.create(name, color)
            select(id)
        }
    }

    fun update(id: Long, name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.update(id, name, color) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            if (settings.settings.first().selectedTagId == id) select(null)
        }
    }
}
