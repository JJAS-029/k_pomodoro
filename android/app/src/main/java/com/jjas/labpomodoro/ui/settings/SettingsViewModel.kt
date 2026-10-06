package com.jjas.labpomodoro.ui.settings

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.ads.AdsManager
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.usecase.HistoryCsv
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val sessions: SessionRepository,
    private val ads: AdsManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _message = MutableStateFlow<String?>(null)

    /** Resultado de la exportación, para mostrarlo debajo del botón. */
    val message: StateFlow<String?> = _message.asStateFlow()

    /** Google pide ofrecer cambiar el consentimiento de anuncios cuando aplica (por ejemplo, en la UE). */
    val privacyOptionsRequired: Boolean get() = ads.privacyOptionsRequired

    fun showPrivacyOptions(activity: Activity) = ads.showPrivacyOptions(activity)

    /** Escribe el historial en el archivo que eligió el usuario. */
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            _message.value = runCatching {
                val all = sessions.all()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(HistoryCsv.build(all, ZoneId.systemDefault()).toByteArray(Charsets.UTF_8))
                    } ?: error("sin archivo")
                }
                "Listo: ${all.size} sesiones exportadas."
            }.getOrElse { "No se pudo exportar el historial." }
        }
    }

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

    fun setAmbientMode(enabled: Boolean) {
        viewModelScope.launch { repository.setAmbientMode(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    /** Vuelve a un elemento distinto en cada recipiente. */
    fun resetVesselElement() {
        viewModelScope.launch { repository.setVesselElement(0) }
    }

    fun setReminder(enabled: Boolean) {
        viewModelScope.launch { repository.setReminder(enabled) }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch { repository.setReminderHour(hour) }
    }

    fun setAmbientDelayMinutes(minutes: Int) {
        viewModelScope.launch { repository.setAmbientDelayMinutes(minutes) }
    }
}
