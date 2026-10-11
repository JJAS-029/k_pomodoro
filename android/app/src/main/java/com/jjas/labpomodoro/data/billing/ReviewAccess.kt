package com.jjas.labpomodoro.data.billing

import java.security.MessageDigest

/**
 * Código que activa Pro para los revisores de Google Play (no pueden comprar). Aquí solo está su
 * SHA-256; el código va únicamente en Play Console › Acceso a la app.
 */
object ReviewAccess {

    private const val CODE_SHA256 = "7ecc9d27d712df5e9302eaacda96e8e08d8799b8a4b28a2893e281b30952ad4e"

    fun matches(code: String): Boolean {
        val digest = MessageDigest.getInstance("SHA-256").digest(code.trim().uppercase().toByteArray())
        return digest.joinToString("") { "%02x".format(it) } == CODE_SHA256
    }
}
