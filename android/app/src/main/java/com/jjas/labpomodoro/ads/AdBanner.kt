package com.jjas.labpomodoro.ads

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AdBannerViewModel @Inject constructor(
    ads: AdsManager,
    settings: SettingsRepository,
) : ViewModel() {
    /** Solo en la versión gratis y cuando el SDK ya puede pedir anuncios. */
    val show: StateFlow<Boolean> = combine(ads.ready, settings.settings) { ready, prefs -> ready && !prefs.isPro }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}

/** Banner adaptable al ancho; no ocupa espacio si no hay anuncio que mostrar. */
@Composable
fun AdBanner(modifier: Modifier = Modifier, viewModel: AdBannerViewModel = hiltViewModel()) {
    val show by viewModel.show.collectAsStateWithLifecycle()
    if (!show) return
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
