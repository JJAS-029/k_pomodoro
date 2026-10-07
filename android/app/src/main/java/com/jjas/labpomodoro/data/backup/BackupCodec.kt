package com.jjas.labpomodoro.data.backup

import com.jjas.labpomodoro.data.local.entity.DiscoveryEntity
import com.jjas.labpomodoro.data.local.entity.InventoryEntity
import com.jjas.labpomodoro.data.local.entity.PlaceEntity
import com.jjas.labpomodoro.data.local.entity.SessionEntity
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.SessionType
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Todo lo que se respalda. Los ajustes van por nombre de clave con su tipo. */
data class BackupSnapshot(
    val createdAtMillis: Long,
    val sessions: List<SessionEntity>,
    val inventory: List<InventoryEntity>,
    val discoveries: List<DiscoveryEntity>,
    val places: List<PlaceEntity>,
    val settings: Map<String, Any>,
) {
    val discoveredCount: Int get() = inventory.count { it.firstObtainedAtMillis != null }
}

/**
 * Convierte el respaldo a JSON comprimido con gzip y de vuelta. Formato compacto (arreglos en vez
 * de objetos por fila) para que miles de sesiones quepan en pocos cientos de KB.
 */
object BackupCodec {

    const val VERSION = 1

    fun encode(snapshot: BackupSnapshot): ByteArray {
        val json = JSONObject()
            .put("version", VERSION)
            .put("createdAt", snapshot.createdAtMillis)
            .put("sessions", JSONArray().apply {
                snapshot.sessions.forEach { s ->
                    put(JSONArray(listOf(s.id, s.type.name, s.startedAtMillis, s.endedAtMillis, s.plannedSeconds, s.actualSeconds, s.completed, s.epochDay, s.hourOfDay, s.placeId ?: JSONObject.NULL)))
                }
            })
            .put("inventory", JSONArray().apply {
                snapshot.inventory.forEach { i -> put(JSONArray(listOf(i.atomicNumber, i.quantity, i.firstObtainedAtMillis ?: JSONObject.NULL))) }
            })
            .put("discoveries", JSONArray().apply {
                snapshot.discoveries.forEach { d -> put(JSONArray(listOf(d.id, d.atomicNumber, d.source.name, d.obtainedAtMillis, d.seen))) }
            })
            .put("places", JSONArray().apply {
                // Sin coordenadas: la ubicación nunca sale del teléfono, solo el nombre del lugar
                snapshot.places.forEach { p -> put(JSONArray(listOf(p.id, p.name, JSONObject.NULL, JSONObject.NULL, p.createdAtMillis))) }
            })
            .put("settings", JSONObject().apply {
                snapshot.settings.forEach { (key, value) ->
                    val type = when (value) {
                        is Boolean -> "b"
                        is Int -> "i"
                        is Long -> "l"
                        is Float -> "f"
                        is String -> "s"
                        else -> return@forEach
                    }
                    put(key, JSONObject().put("t", type).put("v", value))
                }
            })
        return gzip(json.toString().toByteArray(Charsets.UTF_8))
    }

    fun decode(bytes: ByteArray): BackupSnapshot {
        val json = JSONObject(gunzip(bytes).toString(Charsets.UTF_8))
        require(json.getInt("version") <= VERSION) { "Respaldo de una versión más nueva de la app" }
        return BackupSnapshot(
            createdAtMillis = json.getLong("createdAt"),
            sessions = json.getJSONArray("sessions").rows { r ->
                SessionEntity(
                    id = r.getLong(0),
                    type = SessionType.valueOf(r.getString(1)),
                    startedAtMillis = r.getLong(2),
                    endedAtMillis = r.getLong(3),
                    plannedSeconds = r.getInt(4),
                    actualSeconds = r.getInt(5),
                    completed = r.getBoolean(6),
                    epochDay = r.getLong(7),
                    hourOfDay = r.getInt(8),
                    placeId = if (r.isNull(9)) null else r.getLong(9),
                )
            },
            inventory = json.getJSONArray("inventory").rows { r ->
                InventoryEntity(r.getInt(0), r.getInt(1), if (r.isNull(2)) null else r.getLong(2))
            },
            discoveries = json.getJSONArray("discoveries").rows { r ->
                DiscoveryEntity(r.getLong(0), r.getInt(1), DiscoverySource.valueOf(r.getString(2)), r.getLong(3), r.getBoolean(4))
            },
            places = json.getJSONArray("places").rows { r ->
                // Llegan sin coordenadas (NaN): las recupera la primera ubicación con el mismo nombre
                PlaceEntity(r.getLong(0), r.getString(1), r.optCoordinate(2), r.optCoordinate(3), r.getLong(4))
            },
            settings = json.getJSONObject("settings").let { s ->
                s.keys().asSequence().associateWith { key ->
                    val entry = s.getJSONObject(key)
                    when (entry.getString("t")) {
                        "b" -> entry.getBoolean("v")
                        "i" -> entry.getInt("v")
                        "l" -> entry.getLong("v")
                        "f" -> entry.getDouble("v").toFloat()
                        else -> entry.getString("v")
                    }
                }
            },
        )
    }

    private inline fun <T> JSONArray.rows(map: (JSONArray) -> T): List<T> = List(length()) { map(getJSONArray(it)) }

    private fun gzip(data: ByteArray): ByteArray =
        ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()

    private fun gunzip(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }
}

/** Coordenada de un lugar; los respaldos nuevos no las traen. */
private fun JSONArray.optCoordinate(i: Int): Double = if (isNull(i)) Double.NaN else getDouble(i)
