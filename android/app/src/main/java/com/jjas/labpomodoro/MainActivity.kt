package com.jjas.labpomodoro

import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.jjas.labpomodoro.ads.AdsManager
import com.jjas.labpomodoro.data.billing.BillingRepository
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.service.TimerService
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.main.TableCompleteDialog
import com.jjas.labpomodoro.ui.navigation.LabNavHost
import com.jjas.labpomodoro.ui.pip.PipScreen
import com.jjas.labpomodoro.ui.promo.PodcastPromoDialog
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var ads: AdsManager

    @Inject lateinit var billing: BillingRepository

    @Inject lateinit var inventory: InventoryRepository

    private var isInPip by mutableStateOf(false)

    private val supportsPip by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        isInPip = isInPictureInPictureMode
        addOnPictureInPictureModeChangedListener { isInPip = it.isInPictureInPictureMode }

        if (supportsPip) {
            // Los botones y la entrada automática a PiP dependen de si hay un plan en curso
            lifecycleScope.launch {
                engine.state.collect { setPictureInPictureParams(pipParams(it)) }
            }
        }

        // Versión gratis: consentimiento de privacidad y anuncios. En Pro no se carga nada de esto
        lifecycleScope.launch {
            if (!settingsRepository.settings.first().isPro) ads.gatherConsent(this@MainActivity)
        }
        // Anuncio de pantalla completa al terminar el plan, nunca durante una sesión
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                engine.state
                    .filterIsInstance<TimerState.Finished>()
                    // Una sola vez por plan: al cerrar el anuncio la app vuelve aquí con el mismo resumen
                    .filter { it !== ads.endAdShownFor }
                    .collect { finished ->
                        // Primero se ve el resumen del experimento
                        delay(END_AD_DELAY_MILLIS)
                        if (!settingsRepository.settings.first().isPro && !isInPip) {
                            ads.endAdShownFor = finished
                            ads.showEndOfPlan(this@MainActivity)
                        }
                    }
            }
        }

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(initialValue = null)
            LabPomodoroTheme(dynamicColor = settings?.dynamicColorActive == true) {
                // Fuera del if para conservar la pantalla en la que estaba al volver de PiP
                val navController = rememberNavController()
                if (isInPip) PipScreen() else LabNavHost(navController)
                // Anuncio propio de fin de plan (podcast)
                val promo by ads.housePromo.collectAsStateWithLifecycle()
                promo?.let { PodcastPromoDialog(it, onDismiss = ads::dismissHousePromo) }
                // Una sola vez en la vida: al descubrir los 118 elementos
                val discovered by inventory.discoveredCount.collectAsStateWithLifecycle(initialValue = 0)
                if (discovered >= PeriodicTable.SIZE && settings?.tableCelebrated == false) {
                    TableCompleteDialog(onDismiss = { lifecycleScope.launch { settingsRepository.setTableCelebrated() } })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Una suscripción pudo renovarse o cancelarse mientras la app estaba cerrada
        billing.refresh()
    }

    /** Antes de Android 12 no hay entrada automática: se entra a PiP al salir de la app con Home. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = engine.state.value
        if (supportsPip && Build.VERSION.SDK_INT < Build.VERSION_CODES.S && state is TimerState.Active) {
            enterPictureInPictureMode(pipParams(state))
        }
    }

    private fun pipParams(state: TimerState): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(3, 4))
        val active = state as? TimerState.Active
        val actions = if (active == null) {
            emptyList()
        } else {
            val (action, icon, title) = if (active.isPaused) {
                Triple(TimerService.ACTION_RESUME, R.drawable.ic_play, "Continuar")
            } else {
                Triple(TimerService.ACTION_PAUSE, R.drawable.ic_pause, "Pausar")
            }
            listOf(
                RemoteAction(
                    Icon.createWithResource(this, icon),
                    title,
                    title,
                    TimerService.actionIntent(this, action),
                )
            )
        }
        builder.setActions(actions)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(active != null)
        }
        return builder.build()
    }

    private companion object {
        const val END_AD_DELAY_MILLIS = 2_500L
    }
}
