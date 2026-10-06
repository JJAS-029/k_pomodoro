package com.jjas.labpomodoro.ui.main

import kotlin.random.Random

/** Saludos y mensajes de ánimo del laboratorio. */
object Encouragement {

    /** Saludo según la hora del día, para la pantalla de inicio. */
    fun greeting(hour: Int): String = when (hour) {
        in 5..11 -> "Buenos días. ¿Empezamos el experimento?"
        in 12..18 -> "Buenas tardes. El laboratorio está listo."
        in 19..23 -> "Buenas noches. Un último experimento y a descansar."
        else -> "Trabajando de madrugada. Que no se te olvide dormir."
    }

    private val DONE = listOf(
        "¡Reacción completa! Tu concentración fue el mejor catalizador.",
        "Energía de activación superada: lo difícil era empezar, y lo hiciste.",
        "Cada pomodoro es un átomo; hoy armaste una molécula entera.",
        "Como el oro, tu constancia no se oxida.",
        "Experimento exitoso. Marie Curie también empezó con una sola muestra.",
        "Tu cerebro acaba de sintetizar algo valioso: tiempo bien invertido.",
        "Hasta los gases nobles necesitan estabilidad. Descansa, te lo ganaste.",
        "La ciencia avanza paso a paso, y hoy diste varios.",
    )

    private val NONE = listOf(
        "Hoy el experimento no salió, y está bien: también es un dato. Mañana lo retomamos.",
        "No todas las reacciones ocurren al primer intento. Vuelve cuando quieras.",
    )

    /** Mensaje al terminar el plan; [seed] lo mantiene igual mientras se ve el resumen. */
    fun finished(workSessions: Int, seed: Long): String {
        val pool = if (workSessions == 0) NONE else DONE
        return pool[Random(seed).nextInt(pool.size)]
    }
}
