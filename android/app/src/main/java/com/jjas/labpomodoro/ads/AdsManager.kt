package com.jjas.labpomodoro.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.ui.promo.Podcast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anuncios de la versión gratis: pantalla completa al terminar un plan y banner en Config y
 * Logros. Nunca durante una sesión. Se alternan con anuncios propios de los podcasts del creador,
 * que también cubren los casos en que AdMob no tiene anuncio o no hay consentimiento. Antes de pedir anuncios se obtiene el consentimiento de
 * privacidad (UMP), que Google exige en la Unión Europea, Reino Unido y otras regiones.
 */
@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val consent: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val initialized = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)

    /** true cuando ya se pueden mostrar anuncios (hay consentimiento y el SDK arrancó). */
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /**
     * El plan terminado para el que ya se mostró el anuncio (por identidad). Vive aquí y no en la
     * Activity para que tampoco se repita al girar la pantalla.
     */
    var endAdShownFor: Any? = null

    private val _housePromo = MutableStateFlow<Podcast?>(null)

    /** Podcast a mostrar en el diálogo de fin de plan (anuncio propio); null si no hay. */
    val housePromo: StateFlow<Podcast?> = _housePromo.asStateFlow()

    private var endOfPlanCount = 0

    private var interstitial: InterstitialAd? = null
    private var loadingInterstitial = false

    /** Si hay que ofrecer en Config la opción de cambiar el consentimiento. */
    val privacyOptionsRequired: Boolean
        get() = consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Pide (o actualiza) el consentimiento y arranca los anuncios si se permiten. */
    fun gatherConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder().build()
        consent.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { startIfAllowed() }
            },
            { startIfAllowed() }, // Sin red: si ya había consentimiento de antes, sirve
        )
        // Si en una sesión anterior ya se dio, no hace falta esperar a la actualización
        startIfAllowed()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { startIfAllowed() }
    }

    private fun startIfAllowed() {
        if (!consent.canRequestAds() || !initialized.compareAndSet(false, true)) return
        // La inicialización tarda; fuera del hilo principal
        scope.launch(Dispatchers.IO) {
            MobileAds.initialize(context) {
                _ready.value = true
                scope.launch(Dispatchers.Main) { preloadInterstitial() }
            }
        }
    }

    /** Deja listo el anuncio del final del plan para que aparezca sin esperas. */
    fun preloadInterstitial() {
        if (!_ready.value || interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(
            context,
            BuildConfig.AD_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    loadingInterstitial = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingInterstitial = false
                }
            },
        )
    }

    /**
     * Anuncio de fin de plan: uno de AdMob y uno propio, por turnos. Si AdMob no tiene anuncio
     * listo, sale el propio.
     */
    fun showEndOfPlan(activity: Activity) {
        val turn = endOfPlanCount++
        if (turn % 2 == 1 || !showInterstitial(activity)) {
            _housePromo.value = Podcast.entries[(turn / 2) % Podcast.entries.size]
        }
    }

    fun dismissHousePromo() {
        _housePromo.value = null
    }

    /** Muestra el anuncio de pantalla completa si hay uno listo; devuelve si lo mostró. */
    private fun showInterstitial(activity: Activity): Boolean {
        val ad = interstitial ?: run {
            preloadInterstitial()
            return false
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = preloadInterstitial()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = preloadInterstitial()
        }
        ad.show(activity)
        return true
    }
}
