package com.jjas.labpomodoro.ui.main

import android.content.res.Resources
import androidx.annotation.ArrayRes
import com.jjas.labpomodoro.R
import kotlin.random.Random

/** Saludos y mensajes de ánimo del laboratorio. Todos se eligen al azar para que no se repitan. */
object Encouragement {

    /** Lista de saludos según la hora del día (los textos están en strings_main.xml). */
    @ArrayRes
    fun greetingPool(hour: Int): Int = when (hour) {
        in 5..11 -> R.array.main_greetings_morning
        in 12..18 -> R.array.main_greetings_afternoon
        in 19..23 -> R.array.main_greetings_night
        else -> R.array.main_greetings_late
    }

    /** Saludo al azar según la hora del día, para la pantalla de inicio. */
    fun greeting(resources: Resources, hour: Int, random: Random = Random.Default): String =
        resources.getStringArray(greetingPool(hour)).random(random)

    /** Lista de mensajes al terminar el plan; [seed] la mantiene igual mientras se ve el resumen. */
    @ArrayRes
    fun finishedPool(workSessions: Int, seed: Long): Int = when {
        workSessions == 0 -> R.array.main_finished_none
        // Planes largos: a veces un mensaje especial
        workSessions >= 8 && Random(seed).nextBoolean() -> R.array.main_finished_big
        else -> R.array.main_finished_done
    }

    /** Mensaje al terminar el plan; [seed] lo mantiene igual mientras se ve el resumen. */
    fun finished(resources: Resources, workSessions: Int, seed: Long): String {
        val pool = resources.getStringArray(finishedPool(workSessions, seed))
        return pool[Random(seed + 1).nextInt(pool.size)]
    }
}
