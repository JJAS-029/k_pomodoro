package com.jjas.labpomodoro.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.PlaceRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Lugares (Pro, opcional): al iniciar un plan toma la ubicación aproximada una sola vez (la app
 * está abierta, así que no hace falta permiso en segundo plano) y la asigna a cada sesión de ese
 * plan conforme se guarda.
 */
@Singleton
class PlaceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val places: PlaceRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @Volatile private var currentPlaceId: Long? = null

    // Sesiones que terminaron antes de tener la ubicación: se les asigna en cuanto llegue
    private val pending = mutableListOf<Long>()
    private val lock = Mutex()

    fun start() {
        scope.launch {
            engine.events.collect { event ->
                when (event) {
                    TimerEvent.PlanStarted -> {
                        lock.withLock {
                            currentPlaceId = null
                            pending.clear()
                        }
                        val prefs = settings.settings.first()
                        if (prefs.isPro && prefs.placesEnabled && hasPermission()) {
                            // En paralelo: obtener la ubicación puede tardar unos segundos
                            launch {
                                val id = locate() ?: return@launch
                                lock.withLock {
                                    currentPlaceId = id
                                    pending.forEach { places.tagSession(it, id) }
                                    pending.clear()
                                }
                            }
                        }
                    }
                    is TimerEvent.SessionEnded -> lock.withLock {
                        val start = event.startedAt.toEpochMilli()
                        currentPlaceId?.let { places.tagSession(start, it) } ?: pending.add(start)
                    }
                }
            }
        }
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Se revisa en hasPermission()
    private suspend fun locate(): Long? = runCatching {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val token = CancellationTokenSource()
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            // Una sola vez por plan: vale la pena usar el GPS si hace falta. Con permiso aproximado
            // Android entrega igual una ubicación difuminada
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        } ?: run {
            token.cancel()
            client.lastLocation.await()
        } ?: return@runCatching null
        places.resolve(location.latitude, location.longitude, suggestName(location.latitude, location.longitude))
    }.onFailure { Log.w(TAG, "No se pudo obtener el lugar", it) }.getOrNull()

    /** Nombre sugerido para un lugar nuevo: la colonia o la calle, si el teléfono la conoce. */
    private suspend fun suggestName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? = withTimeoutOrNull(GEOCODER_TIMEOUT_MILLIS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses.firstOrNull())
                        override fun onError(errorMessage: String?) = cont.resume(null)
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    runCatching { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }.getOrNull()
                }
            }
        }
        return address?.let { it.subLocality ?: it.thoroughfare ?: it.locality }
    }

    private companion object {
        const val TAG = "PlaceTracker"
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
        const val GEOCODER_TIMEOUT_MILLIS = 5_000L
    }
}
