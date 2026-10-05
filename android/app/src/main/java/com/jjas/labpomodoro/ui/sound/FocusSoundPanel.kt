package com.jjas.labpomodoro.ui.sound

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.FocusSound
import com.jjas.labpomodoro.service.FocusSoundController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusSoundViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val controller: FocusSoundController,
) : ViewModel() {

    val state: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun select(sound: FocusSound) {
        viewModelScope.launch { settings.setFocusSound(sound) }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch { settings.setFocusVolume(volume) }
    }

    fun preview(sound: FocusSound, volume: Float) = controller.preview(sound, volume)
}

/**
 * Elegir el sonido de concentración (Pro). Suena solo durante las sesiones de trabajo; "Probar"
 * lo reproduce unos segundos aunque el timer no esté corriendo.
 */
@Composable
fun FocusSoundPanel(
    onOpenPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusSoundViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val current = settings ?: return
    val enabled = current.isPro
    // El deslizador se mueve libre y solo se guarda al soltar
    var volume by remember(current.focusVolume) { mutableFloatStateOf(current.focusVolume) }

    Column(modifier) {
        Text(
            "Suena solo mientras trabajas; en pausa y en los descansos se apaga solo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.selectableGroup()) {
            FocusSound.entries.forEach { sound ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = current.focusSound == sound,
                            enabled = enabled,
                            role = Role.RadioButton,
                            onClick = { viewModel.select(sound) },
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = current.focusSound == sound, onClick = null, enabled = enabled)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(sound.label, style = MaterialTheme.typography.bodyLarge)
                        if (sound.description.isNotEmpty()) {
                            Text(
                                sound.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Volumen", style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = volume,
            onValueChange = { volume = it },
            onValueChangeFinished = { viewModel.setVolume(volume) },
            enabled = enabled && current.focusSound != FocusSound.OFF,
        )
        if (enabled) {
            OutlinedButton(
                onClick = { viewModel.preview(current.focusSound, volume) },
                enabled = current.focusSound != FocusSound.OFF,
            ) { Text("Probar 5 s") }
        } else {
            FilledTonalButton(onClick = onOpenPro) { Text("Disponible en Pro") }
        }
    }
}
