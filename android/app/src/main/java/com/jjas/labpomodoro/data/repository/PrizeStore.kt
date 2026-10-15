package com.jjas.labpomodoro.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premios ya sorteados para recompensas que aún no se entregan: clave "BASIC_12" (la recompensa
 * básica número 12) → número atómico. Así el líquido puede mutar hacia el elemento que de verdad
 * se va a ganar.
 */
interface PrizeStore {
    fun get(key: String): Int?
    fun put(key: String, atomicNumber: Int)
    fun remove(key: String)
}

/** Para pruebas: sin guardar nada fuera de la memoria. */
class InMemoryPrizeStore : PrizeStore {
    private val map = mutableMapOf<String, Int>()
    override fun get(key: String) = map[key]
    override fun put(key: String, atomicNumber: Int) {
        map[key] = atomicNumber
    }
    override fun remove(key: String) {
        map.remove(key)
    }
}

@Singleton
class PrefsPrizeStore @Inject constructor(@ApplicationContext context: Context) : PrizeStore {
    private val prefs = context.getSharedPreferences("prize_reservations", Context.MODE_PRIVATE)
    override fun get(key: String): Int? = prefs.getInt(key, 0).takeIf { it > 0 }
    override fun put(key: String, atomicNumber: Int) = prefs.edit { putInt(key, atomicNumber) }
    override fun remove(key: String) = prefs.edit { remove(key) }
}
