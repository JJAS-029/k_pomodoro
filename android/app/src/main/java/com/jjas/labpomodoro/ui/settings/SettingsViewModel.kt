package com.jjas.labpomodoro.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.SessionConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    /** null mientras DataStore carga, para no mostrar por un instante los valores por defecto. */
    val settings: StateFlow<AppSettings?> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateSession(transform: (SessionConfig) -> SessionConfig) {
        viewModelScope.launch { repository.updateSession(transform) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrationEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { repository.setKeepScreenOn(enabled) }
    }
}
