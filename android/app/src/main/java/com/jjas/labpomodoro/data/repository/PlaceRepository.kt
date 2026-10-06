package com.jjas.labpomodoro.data.repository

import android.location.Location
import com.jjas.labpomodoro.data.local.dao.PlaceDao
import com.jjas.labpomodoro.data.local.entity.PlaceEntity
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import kotlinx.coroutines.flow.Flow
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Lugares donde el usuario se concentra (Pro, opcional). Todo se queda en el teléfono. */
@Singleton
class PlaceRepository @Inject constructor(
    private val placeDao: PlaceDao,
    private val clock: Clock,
) {

    /** Enfoque por lugar en los últimos [days] días (todo el historial si es null). */
    fun totals(days: Long?): Flow<List<PlaceTotal>> {
        val from = days?.let { LocalDate.now(clock).minusDays(it - 1).toEpochDay() } ?: 0L
        return placeDao.observeTotals(from)
    }

    suspend fun rename(id: Long, name: String) = placeDao.rename(id, name.trim().ifEmpty { "Sin nombre" })

    suspend fun tagSession(startedAtMillis: Long, placeId: Long) = placeDao.tagSession(startedAtMillis, placeId)

    /**
     * El lugar al que pertenece esta ubicación: el más cercano si está a menos de
     * [SAME_PLACE_METERS], o uno nuevo con [suggestedName] (o "Lugar N").
     */
    suspend fun resolve(latitude: Double, longitude: Double, suggestedName: String?): Long {
        val places = placeDao.all()
        val nearest = places.minByOrNull { distance(latitude, longitude, it.latitude, it.longitude) }
        if (nearest != null && distance(latitude, longitude, nearest.latitude, nearest.longitude) <= SAME_PLACE_METERS) {
            return nearest.id
        }
        return placeDao.insert(
            PlaceEntity(
                name = suggestedName?.takeIf { it.isNotBlank() } ?: "Lugar ${places.size + 1}",
                latitude = latitude,
                longitude = longitude,
                createdAtMillis = clock.millis(),
            )
        )
    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
        return result[0]
    }

    companion object {
        /** La ubicación aproximada puede variar unos cientos de metros: se juntan las cercanas. */
        const val SAME_PLACE_METERS = 150f
    }
}
