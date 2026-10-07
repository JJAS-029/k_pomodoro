package com.jjas.labpomodoro.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jjas.labpomodoro.data.backup.BackupRepository
import com.jjas.labpomodoro.data.league.LeagueRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Borrar la cuenta (lo exige Google Play a las apps con inicio de sesión): todo lo que hay en la
 * nube y la cuenta de Firebase. El progreso guardado en el teléfono se queda.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val league: LeagueRepository,
    private val backup: BackupRepository,
) {

    suspend fun deleteAccount(activityContext: Context) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        // Primero los datos (las reglas exigen la sesión del dueño) y al final la cuenta
        try {
            league.leave()
        } catch (e: FirebaseFirestoreException) {
            // Sin perfil en la nube no hay liga que dejar
            if (e.code != FirebaseFirestoreException.Code.NOT_FOUND) throw e
        }
        backup.deleteCloud()
        firestore.collection("users").document(uid).delete().await()
        auth.deleteUser(activityContext)
    }
}
