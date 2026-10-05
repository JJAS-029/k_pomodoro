package com.jjas.labpomodoro.ui.pro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ProViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val sessions: SessionRepository,
    private val clock: Clock,
) : ViewModel() {

    val isPro: StateFlow<Boolean> = settings.settings.map { it.isPro }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPro(enabled: Boolean) {
        viewModelScope.launch { settings.setPro(enabled) }
    }

    /** Solo pruebas: guarda una hora de trabajo completada para ganar elementos sin esperar. */
    fun addTestFocusHour() {
        viewModelScope.launch {
            val end = clock.instant()
            sessions.record(SessionType.WORK, end.minusSeconds(3600), end, 3600, 3600, completed = true)
        }
    }
}

/** Provisional: la compra con Play Billing llega en la Fase 5. */
@Composable
fun ProScreen(onBack: () -> Unit, viewModel: ProViewModel = hiltViewModel()) {
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isPro) "Lab Pomodoro Pro ✓" else "Lab Pomodoro Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text("Listo") }
            }
            Spacer(Modifier.height(16.dp))
            listOf(
                "Modo ambiente: solo el reloj, atenuado, cuando no tocas la pantalla",
                "Pantalla limpia, sin título",
                "Colores dinámicos de Material You",
                "Exportar tu historial a CSV",
                "Sin anuncios",
            ).forEach {
                Text("•  $it", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 6.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "La compra estará disponible en una próxima versión.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Solo en compilaciones de prueba, para probar las funciones Pro antes de Play Billing
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(32.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Activar Pro (solo pruebas)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = isPro, onCheckedChange = viewModel::setPro)
                }
                TextButton(onClick = viewModel::addTestFocusHour) { Text("Sumar 1 h de enfoque (solo pruebas)") }
            }
        }
    }
}
