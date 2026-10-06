package com.jjas.labpomodoro.ui.main

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.backup.BackupInfo
import com.jjas.labpomodoro.data.backup.BackupRepository
import com.jjas.labpomodoro.data.remote.AnalyticsTracker
import com.jjas.labpomodoro.data.remote.AuthRepository
import com.jjas.labpomodoro.data.remote.MissingWebClientIdException
import com.jjas.labpomodoro.data.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val userName: String? = null,
    val isSignedIn: Boolean = false,
    val isBusy: Boolean = false,
    val message: String? = null,
    /** Respaldo encontrado al iniciar sesión en un teléfono sin progreso: se ofrece restaurarlo. */
    val restoreOffer: BackupInfo? = null,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val analytics: AnalyticsTracker,
    private val backup: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(isSignedIn = user != null, userName = user?.displayName ?: user?.email)
                }
            }
        }
    }

    /** [activityContext] solo se usa durante la llamada; no se guarda en el ViewModel. */
    fun signIn(activityContext: Context) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, message = null) }
        viewModelScope.launch {
            val message = try {
                val user = authRepository.signInWithGoogle(activityContext)
                analytics.logLogin()
                userProfileRepository.upsertProfile(user)
                afterSignIn()
            } catch (e: CancellationException) {
                throw e
            } catch (_: GetCredentialCancellationException) {
                null
            } catch (_: NoCredentialException) {
                "No hay cuentas de Google disponibles en este dispositivo"
            } catch (e: MissingWebClientIdException) {
                e.message
            } catch (e: Exception) {
                "Error al iniciar sesión: ${e.message}"
            }
            _uiState.update { it.copy(isBusy = false, message = message) }
        }
    }

    /**
     * Con sesión nueva: si este teléfono está vacío y hay un respaldo, se ofrece restaurarlo; si
     * no hay respaldo, se hace el primero en ese momento.
     */
    private suspend fun afterSignIn(): String {
        val remote = backup.refreshInfo()
        return when {
            remote != null && backup.isLocalEmpty() -> {
                _uiState.update { it.copy(restoreOffer = remote) }
                "Sesión iniciada."
            }
            remote == null -> {
                backup.backup()
                backup.clearStatus()
                "Sesión iniciada. Tu progreso queda respaldado en la nube."
            }
            else -> "Sesión iniciada. Tu progreso se respalda al terminar cada plan."
        }
    }

    fun acceptRestore() {
        _uiState.update { it.copy(restoreOffer = null, isBusy = true) }
        viewModelScope.launch {
            val ok = backup.restore()
            backup.clearStatus()
            _uiState.update {
                it.copy(isBusy = false, message = if (ok) "¡Listo! Recuperaste tu progreso." else "No se pudo restaurar el respaldo.")
            }
        }
    }

    fun declineRestore() {
        _uiState.update { it.copy(restoreOffer = null) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(message = null) }
        }
    }
}
