package com.jjas.labpomodoro.ui.pro

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.data.billing.BillingRepository
import com.jjas.labpomodoro.data.billing.ProOffer
import com.jjas.labpomodoro.data.billing.ProPlan
import com.jjas.labpomodoro.data.billing.StoreState
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.SessionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class ProViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val sessions: SessionRepository,
    private val billing: BillingRepository,
    private val inventory: InventoryRepository,
    private val lab: LabRepository,
    private val clock: Clock,
) : ViewModel() {

    val prefs: StateFlow<AppSettings?> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val store: StateFlow<StoreState> = billing.state
    val message: StateFlow<String?> = billing.message

    init {
        billing.refresh()
    }

    fun buy(activity: Activity, offer: ProOffer) = billing.purchase(activity, offer)

    fun restore() = billing.refresh()

    fun clearMessage() = billing.clearMessage()

    fun setPro(enabled: Boolean) {
        viewModelScope.launch { settings.setPro(enabled) }
    }

    /** Solo pruebas: descubre los elementos que falten, para ver la celebración final. */
    fun completeTable() {
        viewModelScope.launch {
            val missing = inventory.items.first().filter { it.firstObtainedAtMillis == null }
            missing.forEach { inventory.add(it.element.atomicNumber) }
        }
    }

    /** Solo pruebas: obtiene cinco veces cada elemento, para ver la maestría y sus celebraciones. */
    fun addFiveOfEach() {
        viewModelScope.launch { lab.grantEachForTesting(5) }
    }

    /** Solo pruebas: guarda una hora de trabajo completada para ganar elementos sin esperar. */
    fun addTestFocusHour() {
        viewModelScope.launch {
            val end = clock.instant()
            sessions.record(SessionType.WORK, end.minusSeconds(3600), end, 3600, 3600, completed = true)
        }
    }
}

private val BENEFITS = listOf(
    "🌙" to "Modo ambiente: solo el recipiente y el reloj, atenuados, cuando no tocas la pantalla",
    "🎧" to "Sonidos de concentración: ruido blanco, rosa, café, lluvia y olas",
    "🎨" to "Colores de Material You tomados de tu fondo de pantalla",
    "✨" to "Pantalla limpia, sin título",
    "🔍" to "Vidrio realista que deforma el líquido como una lente (Android 13+)",
    "📄" to "Exportar tu historial a CSV",
    "🚫" to "Sin anuncios",
)

@Composable
fun ProScreen(onBack: () -> Unit, viewModel: ProViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val store by viewModel.store.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val purchased = prefs?.proPurchased == true

    LaunchedEffect(Unit) { viewModel.clearMessage() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (prefs?.isPro == true) "Lab Pomodoro Pro ✓" else "Lab Pomodoro Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text("Listo") }
            }
            Spacer(Modifier.height(12.dp))
            BENEFITS.forEach { (icon, text) ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text(icon, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(12.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(20.dp))

            message?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))
            }

            if (purchased) {
                Text("Ya tienes Pro. ¡Gracias por apoyar Lab Pomodoro!", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    // Las suscripciones se administran (cancelar, cambiar de plan) en Google Play
                    val uri = "https://play.google.com/store/account/subscriptions?package=${context.packageName}".toUri()
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }) { Text("Administrar suscripción en Google Play") }
            } else {
                when (val s = store) {
                    StoreState.Connecting -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text("Conectando con Google Play…")
                    }
                    StoreState.NoProducts -> Note("La compra estará disponible cuando Lab Pomodoro esté publicada en Google Play.")
                    is StoreState.Unavailable -> Note(s.message)
                    is StoreState.Ready -> Offers(s.offers, onBuy = { offer -> activity?.let { viewModel.buy(it, offer) } })
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::restore) { Text("Restaurar compras") }

            // Solo en compilaciones de prueba, para probar las funciones Pro sin comprar
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(24.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Activar Pro (solo pruebas)",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = prefs?.proTesting == true, onCheckedChange = viewModel::setPro)
                }
                TextButton(onClick = viewModel::addTestFocusHour) { Text("Sumar 1 h de enfoque (solo pruebas)") }
                TextButton(onClick = viewModel::completeTable) { Text("Completar la tabla (solo pruebas)") }
                TextButton(onClick = viewModel::addFiveOfEach) { Text("Sumar 5 de cada elemento (solo pruebas)") }
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Offers(offers: List<ProOffer>, onBuy: (ProOffer) -> Unit) {
    val monthly = offers.firstOrNull { it.plan == ProPlan.MONTHLY }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        offers.forEach { offer ->
            val (title, period) = when (offer.plan) {
                ProPlan.MONTHLY -> "Mensual" to "al mes"
                ProPlan.YEARLY -> "Anual" to "al año"
                ProPlan.LIFETIME -> "Para siempre" to "un solo pago"
            }
            // Cuánto ahorra el anual frente a pagar 12 meses
            val savings = if (offer.plan == ProPlan.YEARLY && monthly != null && monthly.priceMicros > 0) {
                (100 - offer.priceMicros * 100.0 / (monthly.priceMicros * 12)).roundToInt().takeIf { it > 0 }
            } else {
                null
            }
            OfferCard(
                title = title,
                price = offer.trialDays?.let { "${it} días gratis, luego ${offer.price}" } ?: offer.price,
                period = period,
                badge = savings?.let { "Ahorras $it %" } ?: if (offer.plan == ProPlan.LIFETIME) "Sin suscripción" else null,
                highlighted = offer.plan == ProPlan.YEARLY,
                action = if (offer.trialDays != null) "Probar" else "Elegir",
                onClick = { onBuy(offer) },
            )
        }
        Note(
            "Las suscripciones se renuevan solas y puedes cancelarlas cuando quieras en Google Play. " +
                "Si cancelas durante la prueba gratis, no se te cobra nada."
        )
    }
}

@Composable
private fun OfferCard(
    title: String,
    price: String,
    period: String,
    badge: String?,
    highlighted: Boolean,
    action: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("$price $period", style = MaterialTheme.typography.bodyLarge)
                badge?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            }
            Button(onClick = onClick) { Text(action) }
        }
    }
}
