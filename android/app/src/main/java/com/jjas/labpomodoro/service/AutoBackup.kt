package com.jjas.labpomodoro.service

import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.backup.BackupRepository
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Respalda solo, sin preguntar, cada vez que termina un plan (si hay sesión iniciada): es cuando
 * más progreso nuevo hay y la persona ya no está concentrada.
 */
@Singleton
class AutoBackup @Inject constructor(
    private val engine: TimerEngine,
    private val backup: BackupRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            engine.state
                .map { it is TimerState.Finished }
                .distinctUntilChanged()
                .filter { it && backup.isSignedIn }
                .collect {
                    backup.backup()
                    // El aviso de "respaldado" es para cuando lo pide el usuario, no para este
                    backup.clearStatus()
                }
        }
    }
}
