package com.jjas.labpomodoro.data.repository

import com.jjas.labpomodoro.data.local.dao.DiscoveryDao
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.Medals
import com.jjas.labpomodoro.domain.usecase.MedalCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** Medallas por logros, calculadas del historial, y cuáles faltan por celebrar. */
@Singleton
class MedalRepository @Inject constructor(
    sessions: SessionRepository,
    lab: LabRepository,
    discoveryDao: DiscoveryDao,
    private val settings: SettingsRepository,
) {

    val progress: Flow<List<MedalProgress>> = combine(
        sessions.allSessions,
        lab.obtainedCounts,
        discoveryDao.observeCountBySource(DiscoverySource.FUSION),
    ) { all, counts, fusions -> MedalCalculator.calculate(all, counts, fusions) }

    /** Medallas ganadas que el usuario aún no ha visto, para el aviso en la pantalla principal. */
    val unseen: Flow<List<Medal>> = combine(progress, settings.settings) { progress, prefs ->
        Medals.unseen(progress, prefs.medalsSeen)
    }

    suspend fun markSeen(medals: Collection<Medal>) = settings.addMedalsSeen(medals.map { it.name })
}
