package com.jjas.labpomodoro.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.ui.promo.Podcast
import com.jjas.labpomodoro.ui.promo.PodcastBanner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class AdBannerViewModel @Inject constructor(
    ads: AdsManager,
    settings: SettingsRepository,
) : ViewModel() {
    /** null mientras carga; si no es Pro, si AdMob ya puede pedir anuncios. */
    val state: StateFlow<BannerState?> = combine(ads.ready, settings.settings) { ready, prefs ->
        if (prefs.isPro) BannerState.Hidden else BannerState.Free(admobReady = ready)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

sealed interface BannerState {
    data object Hidden : BannerState
    data class Free(val admobReady: Boolean) : BannerState
}

/**
 * Banner de la versión gratis: la mitad de las veces AdMob y la otra mitad uno de los podcasts
 * del creador (siempre el podcast si AdMob no está disponible).
 */
@Composable
fun AdBanner(modifier: Modifier = Modifier, viewModel: AdBannerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val free = state as? BannerState.Free ?: return
    // Se decide al abrir la pantalla y no cambia mientras está abierta
    val roll = rememberSaveable { Random.nextInt(4) }
    if (!free.admobReady || roll >= 2) {
        PodcastBanner(Podcast.entries[roll % Podcast.entries.size], modifier)
        return
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val width = maxWidth.value.toInt()
        val adView = remember(width) {
            AdView(context).apply {
                adUnitId = BuildConfig.AD_BANNER_ID
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width))
                loadAd(AdRequest.Builder().build())
            }
        }
        DisposableEffect(adView) { onDispose { adView.destroy() } }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth())
    }
}
