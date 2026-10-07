package com.jjas.labpomodoro.data.backup

import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.data.local.entity.InventoryEntity
import com.jjas.labpomodoro.data.local.entity.PlaceEntity
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.SessionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val snapshot = BackupSnapshot(
        createdAtMillis = 1_791_259_000_000,
        sessions = listOf(
            SessionEntity(1, SessionType.WORK, 1000, 1_501_000, 1500, 1500, true, 20_000, 9, placeId = 2),
            SessionEntity(2, SessionType.SHORT_BREAK, 1_501_000, 1_801_000, 300, 120, false, 20_000, 9, placeId = null),
        ),
        inventory = listOf(InventoryEntity(1, 3, 5000), InventoryEntity(2, 0, null)),
        discoveries = listOf(DiscoveryEntity(7, 1, DiscoverySource.BASIC_REWARD, 5000, seen = true)),
        places = listOf(PlaceEntity(2, "Biblioteca", 19.4326, -99.1332, 4000)),
        settings = mapOf(
            "work_minutes" to 25,
            "sound_enabled" to false,
            "focus_volume" to 0.4f,
            "focus_sound" to "RAIN",
            "mastery_celebrated" to 2,
        ),
    )

    @Test
    fun `lo que se respalda vuelve igual`() {
        val restored = BackupCodec.decode(BackupCodec.encode(snapshot))
        // Todo menos las coordenadas de los lugares, que nunca salen del teléfono
        val withoutCoordinates = snapshot.places.map { it.copy(latitude = Double.NaN, longitude = Double.NaN) }
        assertEquals(snapshot.copy(places = withoutCoordinates), restored)
    }

    @Test
    fun `el respaldo va comprimido`() {
        // Mil sesiones: el JSON comprimido debe quedar muy por debajo del límite de un documento
        val big = snapshot.copy(sessions = List(1000) { i -> snapshot.sessions[0].copy(id = i.toLong(), startedAtMillis = i * 1_800_000L) })
        val bytes = BackupCodec.encode(big)
        assertTrue("ocupa ${bytes.size} bytes", bytes.size < 60_000)
        assertEquals(1000, BackupCodec.decode(bytes).sessions.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `un respaldo de una version mas nueva no se aplica`() {
        val future = BackupCodec.encode(snapshot).let { BackupCodec.decode(it) }
        // Se fuerza una versión futura reescribiendo el JSON
        val json = org.json.JSONObject(
            String(java.util.zip.GZIPInputStream(BackupCodec.encode(future).inputStream()).readBytes())
        ).put("version", BackupCodec.VERSION + 1)
        val bytes = java.io.ByteArrayOutputStream().also { out ->
            java.util.zip.GZIPOutputStream(out).use { it.write(json.toString().toByteArray()) }
        }.toByteArray()
        BackupCodec.decode(bytes)
    }
}
