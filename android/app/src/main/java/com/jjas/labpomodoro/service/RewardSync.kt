package com.jjas.labpomodoro.service

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entrega elementos cada vez que crece el tiempo de enfoque guardado en el historial. Escucha la
 * base de datos y no los eventos del timer, así no se pierde nada si la app estaba cerrada.
 */
@Singleton
class RewardSync @Inject constructor(
    private val sessions: SessionRepository,
    private val lab: LabRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            sessions.totalWorkSeconds.distinctUntilChanged().collect { lab.syncRewards(it) }
        }
    }
}
