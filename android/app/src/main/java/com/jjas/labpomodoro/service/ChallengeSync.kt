package com.jjas.labpomodoro.service

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.usecase.WeeklyChallenges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrega el premio (un elemento raro) de cada reto semanal cumplido, una sola vez. Escucha el
 * historial, así que funciona aunque el pomodoro haya terminado con la app cerrada.
 */
@Singleton
class ChallengeSync @Inject constructor(
    private val sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val lab: LabRepository,
    private val clock: Clock,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            sessions.allSessions.collect { all ->
                val today = LocalDate.now(clock)
                WeeklyChallenges.progress(all, today).filter { it.done }.forEach { challenge ->
                    // Primero se reclama (atómico): si ya estaba reclamado no se entrega de nuevo
                    if (settings.claimChallenge(WeeklyChallenges.claimKey(today, challenge.kind), WeeklyChallenges.weekStart(today).toString())) {
                        lab.grantChallengeReward()
                    }
                }
            }
        }
    }
}
