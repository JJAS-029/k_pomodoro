package com.jjas.labpomodoro.data.room

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.timer.RoomLink
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/** Qué está haciendo cada quien en la sala. */
enum class MemberStatus { WAITING, FOCUS, BREAK, PAUSED, AWAY, DONE, OFFLINE }

/** La sala: el plan, cuándo empezó y sus pausas (tiempos de reloj en ms; 0 = todavía no). */
data class StudyRoom(
    val code: String,
    val hostUid: String,
    val hostName: String,
    val config: SessionConfig,
    val createdAt: Long,
    val startedAt: Long = 0,
    val pausedAt: Long = 0,
    val pausedTotal: Long = 0,
) {
    val started: Boolean get() = startedAt > 0
    val paused: Boolean get() = pausedAt > 0

    /** Lo que lleva corriendo el plan sin contar pausas. */
    fun elapsed(nowMillis: Long): Long = ((if (paused) pausedAt else nowMillis) - startedAt - pausedTotal).coerceAtLeast(0)
}

data class RoomMember(val uid: String, val nickname: String, val avatar: Int, val status: MemberStatus, val updatedAt: Long)

data class RoomState(val room: StudyRoom, val members: List<RoomMember>, val myUid: String) {
    val isHost: Boolean get() = room.hostUid == myUid
}

enum class JoinResult { OK, SIGNED_OUT, NOT_FOUND, FULL, ERROR }

/**
 * Estudiar con amigos: salas en Firestore donde todos siguen el mismo plan.
 * - `studyRooms/{código}`: anfitrión, plan, inicio y pausas. Solo el anfitrión la cambia.
 * - `studyRooms/{código}/members/{uid}`: apodo, avatar y estado de cada quien; solo el dueño escribe.
 *
 * Cada teléfono calcula dónde va el plan con la hora de inicio y las pausas ([TimerEngine.follow]),
 * así que no hace falta un servidor que lleve el tiempo. El código de la sala se guarda en los
 * ajustes para volver a ella si Android cierra la app.
 */
@Singleton
class StudyRoomRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) : RoomLink {

    private val lock = Mutex()
    private val _state = MutableStateFlow<RoomState?>(null)
    val state: StateFlow<RoomState?> = _state.asStateFlow()

    /** Se pone en true cuando el anfitrión cierra la sala, para avisar. */
    private val _closed = MutableStateFlow(false)
    val closed: StateFlow<Boolean> = _closed.asStateFlow()

    /** La app está en segundo plano con la pantalla encendida (se fue a otra app). */
    private val away = MutableStateFlow(false)

    private var roomListener: ListenerRegistration? = null
    private var membersListener: ListenerRegistration? = null
    private var presenceJob: Job? = null
    private var room: StudyRoom? = null
    private var members: List<RoomMember> = emptyList()

    val signedIn: Boolean get() = auth.currentUser != null

    private fun roomDoc(code: String) = firestore.collection(ROOMS).document(code)
    private fun memberDoc(code: String, uid: String) = roomDoc(code).collection(MEMBERS).document(uid)

    /** Al abrir la app: vuelve a la sala en la que se estaba. */
    fun start() {
        scope.launch {
            val code = settings.settings.first().studyRoom
            if (code.isNotBlank() && auth.currentUser != null) runCatching { lock.withLock { attach(code) } }
        }
    }

    /** Nombre con el que te ven: el último que usaste, tu nombre de Google o un koala. */
    suspend fun defaultNickname(): String =
        settings.settings.first().roomNickname.ifBlank {
            auth.currentUser?.displayName?.substringBefore(' ')?.take(MAX_NICKNAME)?.ifBlank { null }
                ?: "Koala ${Random.nextInt(100, 1000)}"
        }

    /** Crea una sala con [config] y entra como anfitrión. Devuelve el código o null si falló. */
    suspend fun create(config: SessionConfig, nickname: String): String? = lock.withLock {
        val user = auth.currentUser ?: return@withLock null
        leaveLocked(stopTimer = false)
        val name = cleanNickname(nickname)
        repeat(CODE_ATTEMPTS) {
            val code = newCode()
            val ok = runCatching {
                roomDoc(code).set(
                    mapOf(
                        "host" to user.uid,
                        "hostName" to name,
                        "createdAt" to clock.millis(),
                        "totalHours" to config.totalHours,
                        "workMinutes" to config.workMinutes,
                        "shortBreakMinutes" to config.shortBreakMinutes,
                        "longBreakMinutes" to config.longBreakMinutes,
                        "pomodorosUntilLong" to config.pomodorosUntilLong,
                        "startedAt" to 0L,
                        "pausedAt" to 0L,
                        "pausedTotal" to 0L,
                    )
                ).await()
            }.isSuccess
            if (ok) {
                enter(code, name)
                return@withLock code
            }
        }
        null
    }

    /** Entra a la sala [code]. */
    suspend fun join(code: String, nickname: String): JoinResult = lock.withLock {
        auth.currentUser ?: return@withLock JoinResult.SIGNED_OUT
        val clean = code.uppercase().filter { it in CODE_ALPHABET }
        if (clean.length != CODE_LENGTH) return@withLock JoinResult.NOT_FOUND
        try {
            val snapshot = roomDoc(clean).get().await()
            val found = snapshot.toRoom() ?: return@withLock JoinResult.NOT_FOUND
            if (clock.millis() - found.createdAt > ROOM_MAX_AGE_MILLIS) return@withLock JoinResult.NOT_FOUND
            val count = roomDoc(clean).collection(MEMBERS).get().await().size()
            val already = members.any { it.uid == auth.currentUser?.uid } && room?.code == clean
            if (count >= MAX_MEMBERS && !already) return@withLock JoinResult.FULL
            if (room?.code != clean) leaveLocked(stopTimer = false)
            enter(clean, cleanNickname(nickname))
            JoinResult.OK
        } catch (_: Exception) {
            JoinResult.ERROR
        }
    }

    /** El anfitrión empieza el plan para todos. */
    suspend fun startTogether() = lock.withLock {
        val current = room ?: return@withLock
        if (!isHost(current) || current.started) return@withLock
        runCatching { roomDoc(current.code).update(mapOf("startedAt" to clock.millis(), "pausedAt" to 0L, "pausedTotal" to 0L)).await() }
    }

    /** Sale de la sala; si es tuya, la cierra para todos. [stopTimer] detiene también el plan. */
    suspend fun leave(stopTimer: Boolean) {
        lock.withLock { leaveLocked(stopTimer = false) }
        if (stopTimer) engine.reset()
    }

    /** Se olvida del aviso de sala cerrada. */
    fun clearClosed() {
        _closed.value = false
    }

    /** La activity avisa si la app quedó detrás de otra (pantalla encendida) durante el plan. */
    fun setAway(value: Boolean) {
        away.value = value
    }

    // RoomLink: lo que el timer delega mientras hay sala

    override suspend fun onStart(): Boolean {
        val current = room ?: return false
        if (isHost(current) && !current.started) startTogether()
        // Los invitados esperan a que el anfitrión empiece
        return true
    }

    override suspend fun onPause(): Boolean {
        val current = room ?: return false
        if (!isHost(current) || !current.started || current.paused) return true
        runCatching { roomDoc(current.code).update("pausedAt", clock.millis()).await() }
        return true
    }

    override suspend fun onResume(): Boolean {
        val current = room ?: return false
        if (!isHost(current) || !current.paused) return true
        val total = current.pausedTotal + (clock.millis() - current.pausedAt).coerceAtLeast(0)
        runCatching { roomDoc(current.code).update(mapOf("pausedAt" to 0L, "pausedTotal" to total)).await() }
        return true
    }

    override suspend fun onStop() {
        lock.withLock { leaveLocked(stopTimer = false) }
    }

    private fun isHost(room: StudyRoom) = room.hostUid == auth.currentUser?.uid

    /** Escribe tu fila y empieza a escuchar la sala. */
    private suspend fun enter(code: String, nickname: String) {
        val uid = auth.currentUser?.uid ?: return
        settings.setStudyRoom(code)
        settings.setRoomNickname(nickname)
        memberDoc(code, uid).set(memberData(nickname, MemberStatus.WAITING)).await()
        attach(code)
    }

    private fun memberData(nickname: String, status: MemberStatus) = mapOf(
        "nickname" to nickname,
        "avatar" to avatarFor(auth.currentUser?.uid.orEmpty()),
        "status" to status.name,
        "updatedAt" to clock.millis(),
    )

    private fun attach(code: String) {
        detach()
        val uid = auth.currentUser?.uid ?: return
        engine.roomLink = this
        roomListener = roomDoc(code).addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            val parsed = snapshot?.toRoom()
            if (parsed == null) {
                // El anfitrión cerró la sala: el plan sigue, pero ya por su cuenta
                if (snapshot?.metadata?.isFromCache == false) scope.launch { lock.withLock { closedByHost() } }
                return@addSnapshotListener
            }
            room = parsed
            publish(uid)
            if (parsed.started) {
                scope.launch { engine.follow(parsed.config, parsed.startedAt, parsed.elapsed(clock.millis()), parsed.paused) }
            }
        }
        membersListener = roomDoc(code).collection(MEMBERS).addSnapshotListener { snapshot, _ ->
            members = snapshot?.documents?.mapNotNull { it.toMember() }.orEmpty()
            publish(uid)
        }
        presenceJob = scope.launch { reportPresence(code, uid) }
    }

    /** Sube tu estado cuando cambia, y cada minuto para que los demás sepan que sigues ahí. */
    private suspend fun reportPresence(code: String, uid: String) = coroutineScope {
        // Latido: también vuelve a calcular quién lleva rato sin conexión
        launch {
            while (true) {
                delay(HEARTBEAT_MILLIS)
                runCatching { memberDoc(code, uid).update("updatedAt", clock.millis()).await() }
                publish(uid)
            }
        }
        combine(engine.state, away, _state) { timer, isAway, roomState -> statusOf(roomState?.room, timer, isAway) }
            .distinctUntilChanged()
            .collect { status ->
                runCatching { memberDoc(code, uid).update(mapOf("status" to status.name, "updatedAt" to clock.millis())).await() }
            }
    }

    private fun publish(uid: String) {
        val current = room ?: return
        val now = clock.millis()
        // Sin noticias en un rato = sin conexión
        val shown = members.map { if (now - it.updatedAt > OFFLINE_AFTER_MILLIS && it.uid != uid) it.copy(status = MemberStatus.OFFLINE) else it }
            .sortedWith(compareByDescending<RoomMember> { it.uid == current.hostUid }.thenBy { it.nickname.lowercase() })
        _state.value = RoomState(current, shown, uid)
    }

    private suspend fun leaveLocked(stopTimer: Boolean) {
        val current = room
        val uid = auth.currentUser?.uid
        detach()
        room = null
        members = emptyList()
        _state.value = null
        settings.setStudyRoom("")
        if (current != null && uid != null) {
            runCatching { memberDoc(current.code, uid).delete().await() }
            if (current.hostUid == uid) runCatching { roomDoc(current.code).delete().await() }
        }
        if (stopTimer) engine.reset()
    }

    private suspend fun closedByHost() {
        if (room == null) return
        detach()
        room = null
        members = emptyList()
        _state.value = null
        settings.setStudyRoom("")
        _closed.value = true
    }

    private fun detach() {
        roomListener?.remove()
        membersListener?.remove()
        presenceJob?.cancel()
        roomListener = null
        membersListener = null
        presenceJob = null
        if (engine.roomLink === this) engine.roomLink = null
    }

    private fun DocumentSnapshot.toRoom(): StudyRoom? {
        if (!exists()) return null
        val host = getString("host") ?: return null
        val defaults = SessionConfig()
        return StudyRoom(
            code = id,
            hostUid = host,
            hostName = getString("hostName").orEmpty(),
            config = SessionConfig(
                totalHours = getLong("totalHours")?.toInt() ?: defaults.totalHours,
                workMinutes = getLong("workMinutes")?.toInt() ?: defaults.workMinutes,
                shortBreakMinutes = getLong("shortBreakMinutes")?.toInt() ?: defaults.shortBreakMinutes,
                longBreakMinutes = getLong("longBreakMinutes")?.toInt() ?: defaults.longBreakMinutes,
                pomodorosUntilLong = getLong("pomodorosUntilLong")?.toInt() ?: defaults.pomodorosUntilLong,
            ).normalized(),
            createdAt = getLong("createdAt") ?: 0,
            startedAt = getLong("startedAt") ?: 0,
            pausedAt = getLong("pausedAt") ?: 0,
            pausedTotal = getLong("pausedTotal") ?: 0,
        )
    }

    private fun DocumentSnapshot.toMember(): RoomMember? {
        val nickname = getString("nickname") ?: return null
        return RoomMember(
            uid = id,
            nickname = nickname,
            avatar = getLong("avatar")?.toInt() ?: 1,
            status = getString("status")?.let { name -> MemberStatus.entries.firstOrNull { it.name == name } } ?: MemberStatus.WAITING,
            updatedAt = getLong("updatedAt") ?: 0,
        )
    }

    companion object {
        private const val ROOMS = "studyRooms"
        private const val MEMBERS = "members"
        const val CODE_LENGTH = 6

        /** Sin 0/O, 1/I/L: el código se dicta y se copia a mano. */
        const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        const val MAX_MEMBERS = 8
        const val MAX_NICKNAME = 20
        private const val CODE_ATTEMPTS = 5
        private const val HEARTBEAT_MILLIS = 60_000L
        private const val OFFLINE_AFTER_MILLIS = 3 * 60_000L

        /** Las salas duran un día; después el código ya no sirve para entrar. */
        private const val ROOM_MAX_AGE_MILLIS = 24 * 60 * 60 * 1000L

        fun newCode(random: Random = Random.Default): String = String(CharArray(CODE_LENGTH) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] })

        fun cleanNickname(nickname: String): String =
            nickname.trim().replace(Regex("\\s+"), " ").take(MAX_NICKNAME).ifEmpty { "Koala" }

        /** Un elemento fijo por cuenta, para que cada quien tenga su ficha en la sala. */
        fun avatarFor(uid: String): Int = (uid.hashCode().mod(118)) + 1

        /** Estado que se ve de ti: según el plan y si te fuiste a otra app durante el trabajo. */
        fun statusOf(room: StudyRoom?, timer: TimerState, away: Boolean): MemberStatus = when {
            room == null || !room.started -> MemberStatus.WAITING
            timer is TimerState.Active -> when {
                timer.isPaused -> MemberStatus.PAUSED
                timer.current.type == SessionType.WORK -> if (away) MemberStatus.AWAY else MemberStatus.FOCUS
                else -> MemberStatus.BREAK
            }
            else -> MemberStatus.DONE
        }
    }
}
