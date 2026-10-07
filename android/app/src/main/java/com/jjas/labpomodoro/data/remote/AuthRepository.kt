package com.jjas.labpomodoro.data.remote

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** El google-services.json no trae un cliente OAuth web (falta SHA-1 o habilitar Google en Authentication). */
class MissingWebClientIdException : IllegalStateException(
    "Falta default_web_client_id: registra el SHA-1, habilita Google en Firebase Authentication " +
        "y vuelve a descargar google-services.json"
)

/**
 * Google Sign-In con Credential Manager + Firebase Auth.
 * El inicio de sesión es opcional: la app funciona offline y el login solo activa la sincronización.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val appContext: Context,
) {

    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Requiere el contexto de una Activity: Credential Manager muestra su hoja de selección de cuenta
     * sobre ella.
     */
    suspend fun signInWithGoogle(activityContext: Context): FirebaseUser =
        checkNotNull(auth.signInWithCredential(googleCredential(activityContext)).await().user)

    /**
     * Borra la cuenta de Firebase. Si el inicio de sesión ya es viejo, Firebase lo exige reciente:
     * se vuelve a elegir la cuenta de Google y se reintenta.
     */
    suspend fun deleteUser(activityContext: Context) {
        val user = auth.currentUser ?: return
        try {
            user.delete().await()
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            user.reauthenticate(googleCredential(activityContext)).await()
            user.delete().await()
        }
        CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
    }

    private suspend fun googleCredential(activityContext: Context): AuthCredential {
        val option = GetSignInWithGoogleOption.Builder(webClientId()).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val credential = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
            .credential

        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Tipo de credencial inesperado: ${credential.type}" }

        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        return GoogleAuthProvider.getCredential(idToken, null)
    }

    suspend fun signOut() {
        auth.signOut()
        // Olvida la cuenta elegida para que el próximo login vuelva a mostrar el selector
        CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
    }

    /**
     * El plugin google-services genera R.string.default_web_client_id solo si el json trae un cliente
     * OAuth web. Se busca en tiempo de ejecución para que el proyecto compile aunque aún no exista.
     */
    @SuppressLint("DiscouragedApi")
    private fun webClientId(): String {
        val resId = appContext.resources.getIdentifier(
            "default_web_client_id", "string", appContext.packageName
        )
        if (resId == 0) throw MissingWebClientIdException()
        return appContext.getString(resId)
    }
}
