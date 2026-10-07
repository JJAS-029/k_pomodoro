package com.jjas.labpomodoro.data.backup

import android.os.Build
import androidx.annotation.StringRes
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.local.LabDatabase
import com.jjas.labpomodoro.data.local.dao.DiscoveryDao
import com.jjas.labpomodoro.data.local.dao.InventoryDao
import com.jjas.labpomodoro.data.local.dao.PlaceDao
import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que se sabe del último respaldo en la nube, para mostrarlo en Config. */
data class BackupInfo(
    val createdAtMillis: Long,
    val sessions: Int,
    val elements: Int,
    val device: String,
)

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data class Done(@StringRes val messageRes: Int) : BackupStatus
    data class Failed(@StringRes val messageRes: Int) : BackupStatus
}

/**
 * Respaldo del progreso en Firestore, en `users/{uid}/backup/`. Un documento `meta` con el
 * resumen y el contenido comprimido en uno o más documentos `part_N` (Firestore admite ~1 MB por
 * documento). Las reglas de Firestore solo dejan leer y escribir al dueño.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val db: LabDatabase,
    private val sessionDao: SessionDao,
    private val inventoryDao: InventoryDao,
    private val discoveryDao: DiscoveryDao,
    private val placeDao: PlaceDao,
    private val settings: SettingsRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: Clock,
) {
    private val lock = Mutex()

    private val _status = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val status: StateFlow<BackupStatus> = _status.asStateFlow()

    private val _info = MutableStateFlow<BackupInfo?>(null)

    /** Último respaldo conocido; null si no hay o aún no se consulta. */
    val info: StateFlow<BackupInfo?> = _info.asStateFlow()

    val isSignedIn: Boolean get() = auth.currentUser != null

    private fun folder(uid: String) = firestore.collection("users").document(uid).collection("backup")

    /** Consulta el resumen del último respaldo. */
    suspend fun refreshInfo(): BackupInfo? {
        val uid = auth.currentUser?.uid ?: return null.also { _info.value = null }
        return runCatching {
            val meta = folder(uid).document(META).get().await()
            if (!meta.exists()) null else BackupInfo(
                createdAtMillis = meta.getLong("createdAt") ?: 0,
                sessions = (meta.getLong("sessions") ?: 0).toInt(),
                elements = (meta.getLong("elements") ?: 0).toInt(),
                device = meta.getString("device").orEmpty(),
            )
        }.getOrNull().also { _info.value = it }
    }

    /** Sube todo el progreso. Devuelve false si no hay sesión o algo falló. */
    suspend fun backup(): Boolean = lock.withLock {
        val uid = auth.currentUser?.uid ?: return false
        _status.value = BackupStatus.Working
        runCatching {
            val snapshot = snapshot()
            val bytes = withContext(Dispatchers.Default) { BackupCodec.encode(snapshot) }
            val parts = (bytes.indices step PART_BYTES).map { bytes.copyOfRange(it, minOf(it + PART_BYTES, bytes.size)) }
            val folder = folder(uid)
            val batch = firestore.batch()
            parts.forEachIndexed { i, part -> batch.set(folder.document("part_$i"), mapOf("data" to Blob.fromBytes(part))) }
            batch.set(
                folder.document(META),
                mapOf(
                    "version" to BackupCodec.VERSION,
                    "createdAt" to snapshot.createdAtMillis,
                    "parts" to parts.size,
                    "sessions" to snapshot.sessions.size,
                    "elements" to snapshot.discoveredCount,
                    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                ),
            )
            batch.commit().await()
            refreshInfo()
        }.fold(
            onSuccess = {
                _status.value = BackupStatus.Done(R.string.set_backup_done)
                true
            },
            onFailure = {
                _status.value = BackupStatus.Failed(R.string.set_backup_failed)
                false
            },
        )
    }

    /** Borra el respaldo de la nube (al borrar la cuenta); el progreso del teléfono no se toca. */
    suspend fun deleteCloud() = lock.withLock {
        val uid = auth.currentUser?.uid ?: return@withLock
        folder(uid).get().await().documents.forEach { it.reference.delete().await() }
        _info.value = null
        _status.value = BackupStatus.Idle
    }

    /** Reemplaza el progreso de este teléfono por el del respaldo. */
    suspend fun restore(): Boolean = lock.withLock {
        val uid = auth.currentUser?.uid ?: return false
        _status.value = BackupStatus.Working
        runCatching {
            val folder = folder(uid)
            val meta = folder.document(META).get().await()
            require(meta.exists()) { "No hay respaldo" }
            val parts = (meta.getLong("parts") ?: 0).toInt()
            val bytes = (0 until parts).map { i ->
                folder.document("part_$i").get().await().getBlob("data")?.toBytes() ?: error("Respaldo incompleto")
            }.reduce { acc, part -> acc + part }
            val snapshot = withContext(Dispatchers.Default) { BackupCodec.decode(bytes) }
            apply(snapshot)
        }.fold(
            onSuccess = {
                _status.value = BackupStatus.Done(R.string.set_restore_done)
                true
            },
            onFailure = {
                _status.value = BackupStatus.Failed(R.string.set_restore_failed)
                false
            },
        )
    }

    /** Si este teléfono está vacío (instalación nueva), conviene ofrecer restaurar. */
    suspend fun isLocalEmpty(): Boolean = sessionDao.all().isEmpty() && inventoryDao.discoveredAtomicNumbers().isEmpty()

    fun clearStatus() {
        _status.value = BackupStatus.Idle
    }

    private suspend fun snapshot() = BackupSnapshot(
        createdAtMillis = clock.millis(),
        sessions = sessionDao.all(),
        inventory = inventoryDao.all(),
        discoveries = discoveryDao.all(),
        places = placeDao.all(),
        settings = settings.exportForBackup(),
    )

    private suspend fun apply(snapshot: BackupSnapshot) {
        // Todo o nada: si algo falla, la base de datos queda como estaba
        db.withTransaction {
            sessionDao.deleteAll()
            discoveryDao.deleteAll()
            placeDao.deleteAll()
            placeDao.insertAll(snapshot.places)
            sessionDao.insertAll(snapshot.sessions)
            inventoryDao.insertAll(snapshot.inventory)
            discoveryDao.insertAll(snapshot.discoveries)
        }
        settings.importFromBackup(snapshot.settings)
    }

    private companion object {
        const val META = "meta"

        /** Por debajo del límite de ~1 MB por documento de Firestore. */
        const val PART_BYTES = 900_000
    }
}
