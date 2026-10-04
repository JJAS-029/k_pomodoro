package com.jjas.labpomodoro.data.repository

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Perfil del usuario en `users/{uid}`; las reglas de Firestore solo dejan escribir al propio dueño. */
@Singleton
class UserProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun upsertProfile(user: FirebaseUser) {
        val profile = mapOf(
            "displayName" to user.displayName,
            "email" to user.email,
            "lastLoginAt" to FieldValue.serverTimestamp(),
        )
        firestore.collection("users")
            .document(user.uid)
            .set(profile, SetOptions.merge())
            .await()
    }
}
