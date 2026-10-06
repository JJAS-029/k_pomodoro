package com.jjas.labpomodoro.service

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.league.LeagueRepository
import com.jjas.labpomodoro.data.local.dao.SessionDao
import com.jjas.labpomodoro.data.remote.AuthRepository
import com.jjas.labpomodoro.domain.model.LeagueRules
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mantiene la liga al día: al abrir la app cierra la semana anterior si hace falta y publica los
 * minutos de la semana cada vez que cambian (con una pausa corta para no escribir de más).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class LeagueSync @Inject constructor(
    private val leagues: LeagueRepository,
    private val auth: AuthRepository,
    private val sessionDao: SessionDao,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** Minutos de enfoque completados en la semana de la liga (desde el lunes, hora UTC). */
    fun weeklyXp(): Flow<Int> =
        sessionDao.observeWorkSecondsSince(LeagueRules.weekStart(clock.instant()).toEpochMilli())
            .map { (it / 60).toInt() }

    fun start() {
        scope.launch {
            auth.currentUser
                .map { it != null }
                .distinctUntilChanged()
                .flatMapLatest { signedIn -> if (signedIn) weeklyXp() else flowOf(null) }
                .debounce(PUBLISH_DELAY_MILLIS)
                .distinctUntilChanged()
                .collect { xp ->
                    if (xp == null) return@collect
                    runCatching {
                        leagues.rollOverIfNeeded(xp)
                        leagues.publishXp(xp)
                    }
                }
        }
    }

    /** Para la pantalla de la liga: los minutos actuales una sola vez. */
    suspend fun currentXp(): Int = weeklyXp().first()

    private companion object {
        const val PUBLISH_DELAY_MILLIS = 5_000L
    }
}
