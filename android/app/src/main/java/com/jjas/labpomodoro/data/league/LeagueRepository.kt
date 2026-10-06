package com.jjas.labpomodoro.data.league

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.jjas.labpomodoro.domain.model.League
import com.jjas.labpomodoro.domain.model.LeagueMember
import com.jjas.labpomodoro.domain.model.LeagueOutcome
import com.jjas.labpomodoro.domain.model.LeagueRules
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Lo que la persona eligió para las ligas y dónde está esta semana. */
data class LeagueProfile(
    val league: League,
    val nickname: String,
    val avatar: Int,
    val week: String?,
    val groupId: String?,
    val lastResult: LeagueResult?,
)

/** Cómo le fue la semana pasada; [seen] = ya se mostró el aviso. */
data class LeagueResult(
    val week: String,
    val rank: Int,
    val size: Int,
    val outcome: LeagueOutcome,
    val from: League,
    val to: League,
    val seen: Boolean,
)

/**
 * Ligas semanales en Firestore:
 * - `users/{uid}` campo `league`: perfil (liga, apodo, avatar, semana y grupo actuales, último resultado).
 * - `leagueGroups/{semana}_{liga}_{n}`: grupo con su número de integrantes (máximo 30).
 * - `leagueGroups/{id}/members/{uid}`: apodo, avatar y minutos de la semana; solo el dueño escribe.
 * Los grupos se llenan en orden (n = 0, 1, 2…) con transacciones, sin consultas ni índices.
 */
@Singleton
class LeagueRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val clock: Clock,
) {
    private val lock = Mutex()

    val uid: String? get() = auth.currentUser?.uid

    private fun userDoc(uid: String) = firestore.collection("users").document(uid)
    private fun group(id: String) = firestore.collection("leagueGroups").document(id)

    suspend fun profile(): LeagueProfile? {
        val uid = uid ?: return null
        @Suppress("UNCHECKED_CAST")
        val data = userDoc(uid).get().await().get("league") as? Map<String, Any?> ?: return null
        @Suppress("UNCHECKED_CAST")
        val result = (data["lastResult"] as? Map<String, Any?>)?.let { r ->
            LeagueResult(
                week = r["week"] as String,
                rank = (r["rank"] as Number).toInt(),
                size = (r["size"] as Number).toInt(),
                outcome = LeagueOutcome.valueOf(r["outcome"] as String),
                from = League.valueOf(r["from"] as String),
                to = League.valueOf(r["to"] as String),
                seen = r["seen"] as? Boolean ?: false,
            )
        }
        return LeagueProfile(
            league = (data["tier"] as? String)?.let { runCatching { League.valueOf(it) }.getOrNull() } ?: League.HYDROGEN,
            nickname = data["nickname"] as? String ?: "",
            avatar = (data["avatar"] as? Number)?.toInt() ?: 1,
            week = data["week"] as? String,
            groupId = data["groupId"] as? String,
            lastResult = result,
        )
    }

    /** Se une a las ligas (empieza en Hidrógeno) y entra a un grupo de esta semana. */
    suspend fun join(nickname: String, avatar: Int, weeklyXp: Int) = lock.withLock {
        val uid = uid ?: return@withLock
        val current = profile()
        val profile = LeagueProfile(
            league = current?.league ?: League.HYDROGEN,
            nickname = nickname.trim().take(MAX_NICKNAME).ifEmpty { "Científico anónimo" },
            avatar = avatar,
            week = null,
            groupId = null,
            lastResult = current?.lastResult,
        )
        enterWeek(uid, profile, weeklyXp)
    }

    /**
     * Al empezar una semana nueva: cierra la anterior con las posiciones finales (incluidos los
     * bots, que son iguales en todos los teléfonos), sube o baja de liga y entra a un grupo nuevo.
     * No hace nada si la persona no está en las ligas o ya está en la semana actual.
     */
    suspend fun rollOverIfNeeded(weeklyXp: Int) = lock.withLock {
        val uid = uid ?: return@withLock
        val profile = profile() ?: return@withLock
        val week = LeagueRules.weekId(clock.instant())
        if (profile.week == week) return@withLock
        var updated = profile
        if (profile.groupId != null && profile.week != null) {
            val real = members(profile.groupId)
            val all = real + LeagueRules.bots(profile.groupId, profile.league, real.size, weekProgress = 1f)
            val ranking = LeagueRules.ranking(all)
            val rank = ranking.indexOfFirst { it.id == uid } + 1
            if (rank > 0) {
                val outcome = LeagueRules.outcome(profile.league, rank, ranking.size)
                val to = LeagueRules.nextLeague(profile.league, outcome)
                updated = profile.copy(
                    league = to,
                    lastResult = LeagueResult(profile.week, rank, ranking.size, outcome, profile.league, to, seen = false),
                )
            }
        }
        enterWeek(uid, updated, weeklyXp)
    }

    private suspend fun enterWeek(uid: String, profile: LeagueProfile, weeklyXp: Int) {
        val week = LeagueRules.weekId(clock.instant())
        val groupId = claimSeat(week, profile.league)
        group(groupId).collection("members").document(uid).set(
            mapOf(
                "nickname" to profile.nickname,
                "avatar" to profile.avatar,
                "xp" to weeklyXp.coerceIn(0, MAX_XP),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
        ).await()
        saveProfile(uid, profile.copy(week = week, groupId = groupId))
    }

    /** Busca lugar en el primer grupo con espacio; si todos están llenos, crea el siguiente. */
    private suspend fun claimSeat(week: String, league: League): String {
        var n = 0
        while (true) {
            val id = "${week}_${league.ordinal}_$n"
            val ref = group(id)
            val claimed = firestore.runTransaction { tx ->
                val snapshot = tx.get(ref)
                val count = snapshot.getLong("count") ?: 0
                when {
                    !snapshot.exists() -> {
                        tx.set(ref, mapOf("week" to week, "tier" to league.ordinal, "count" to 1))
                        true
                    }
                    count < LeagueRules.GROUP_SIZE -> {
                        tx.update(ref, "count", count + 1)
                        true
                    }
                    else -> false
                }
            }.await()
            if (claimed) return id
            n++
        }
    }

    /** Publica los minutos de la semana en el grupo actual. */
    suspend fun publishXp(weeklyXp: Int) {
        val uid = uid ?: return
        val profile = profile() ?: return
        val groupId = profile.groupId ?: return
        if (profile.week != LeagueRules.weekId(clock.instant())) return
        group(groupId).collection("members").document(uid)
            .update(mapOf("xp" to weeklyXp.coerceIn(0, MAX_XP), "updatedAt" to FieldValue.serverTimestamp()))
            .await()
    }

    suspend fun markResultSeen() {
        val uid = uid ?: return
        userDoc(uid).set(mapOf("league" to mapOf("lastResult" to mapOf("seen" to true))), SetOptions.merge()).await()
    }

    /** Deja las ligas: se borra del grupo y del perfil. */
    suspend fun leave() = lock.withLock {
        val uid = uid ?: return@withLock
        profile()?.groupId?.let { group(it).collection("members").document(uid).delete().await() }
        userDoc(uid).update("league", FieldValue.delete()).await()
    }

    /** Integrantes reales del grupo, en vivo. */
    fun observeMembers(groupId: String): Flow<List<LeagueMember>> = callbackFlow {
        val registration = group(groupId).collection("members").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) trySend(snapshot.documents.map { it.toMember() })
        }
        awaitClose { registration.remove() }
    }

    private suspend fun members(groupId: String): List<LeagueMember> =
        group(groupId).collection("members").get().await().documents.map { it.toMember() }

    private fun com.google.firebase.firestore.DocumentSnapshot.toMember() = LeagueMember(
        id = id,
        nickname = getString("nickname").orEmpty(),
        avatar = (getLong("avatar") ?: 1).toInt(),
        xp = (getLong("xp") ?: 0).toInt(),
    )

    private suspend fun saveProfile(uid: String, profile: LeagueProfile) {
        val data = mutableMapOf<String, Any?>(
            "tier" to profile.league.name,
            "nickname" to profile.nickname,
            "avatar" to profile.avatar,
            "week" to profile.week,
            "groupId" to profile.groupId,
        )
        profile.lastResult?.let { r ->
            data["lastResult"] = mapOf(
                "week" to r.week, "rank" to r.rank, "size" to r.size, "outcome" to r.outcome.name,
                "from" to r.from.name, "to" to r.to.name, "seen" to r.seen,
            )
        }
        userDoc(uid).set(mapOf("league" to data), SetOptions.merge()).await()
    }

    companion object {
        const val MAX_NICKNAME = 20

        /** Una semana entera de minutos: más que eso no puede ser real. */
        const val MAX_XP = 7 * 24 * 60
    }
}
