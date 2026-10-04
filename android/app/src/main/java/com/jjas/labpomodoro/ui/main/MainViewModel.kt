package com.jjas.labpomodoro.ui.main

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userProfileRepository: UserProfileRepository,
    private val analytics: AnalyticsTracker,
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
                // Prueba de punta a punta: Auth + escritura en Firestore
                userProfileRepository.upsertProfile(user)
                "Perfil guardado en users/${user.uid}"
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

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(message = null) }
        }
    }
}
