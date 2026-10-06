package com.jjas.labpomodoro.ui.main

import kotlin.random.Random

/** Saludos y mensajes de ánimo del laboratorio. Todos se eligen al azar para que no se repitan. */
object Encouragement {

    private val MORNING = listOf(
        "Buenos días. ¿Empezamos el experimento?",
        "Buenos días. El laboratorio ya está encendido.",
        "Café listo, matraces limpios. ¿Arrancamos?",
        "Buenos días. Las mañanas son buenas para las ideas difíciles.",
        "Nuevo día, nuevos experimentos.",
    )
    private val AFTERNOON = listOf(
        "Buenas tardes. El laboratorio está listo.",
        "Buenas tardes. ¿Un experimento más?",
        "La tarde es larga: dale un buen pomodoro.",
        "Buenas tardes. Los matraces te esperan.",
        "Hora de convertir la tarde en algo que valga la pena.",
    )
    private val NIGHT = listOf(
        "Buenas noches. Un último experimento y a descansar.",
        "Buenas noches. El laboratorio nocturno está abierto.",
        "La noche es tranquila: buen momento para concentrarse.",
        "Buenas noches. Hasta las estrellas trabajan de noche.",
    )
    private val LATE = listOf(
        "Trabajando de madrugada. Que no se te olvide dormir.",
        "Madrugada en el laboratorio. Un pomodoro y a la cama.",
        "A estas horas solo quedan los científicos y los búhos.",
    )

    /** Saludo al azar según la hora del día, para la pantalla de inicio. */
    fun greeting(hour: Int, random: Random = Random.Default): String = when (hour) {
        in 5..11 -> MORNING
        in 12..18 -> AFTERNOON
        in 19..23 -> NIGHT
        else -> LATE
    }.random(random)

    private val DONE = listOf(
        "¡Reacción completa! Tu concentración fue el mejor catalizador.",
        "Energía de activación superada: lo difícil era empezar, y lo hiciste.",
        "Cada pomodoro es un átomo; hoy armaste una molécula entera.",
        "Como el oro, tu constancia no se oxida.",
        "Experimento exitoso. Marie Curie también empezó con una sola muestra.",
        "Tu cerebro acaba de sintetizar algo valioso: tiempo bien invertido.",
        "Hasta los gases nobles necesitan estabilidad. Descansa, te lo ganaste.",
        "La ciencia avanza paso a paso, y hoy diste varios.",
        "Resultado positivo: el enfoque sí funciona. Repetible y comprobado.",
        "El carbono se vuelve diamante con presión y tiempo. Vas por buen camino.",
        "Hipótesis confirmada: puedes con esto.",
        "Tu laboratorio huele a logro. Bueno, y un poco a café.",
        "Newton tuvo su manzana; tú tuviste tu racha de pomodoros.",
        "Una reacción en cadena empieza con un solo paso. Ya diste muchos.",
        "Ni el helio se te escapa: hoy contuviste toda tu atención.",
        "Los grandes descubrimientos son constancia disfrazada de genialidad.",
        "Datos del día: más enfoque, menos distracción. Excelente muestra.",
        "Tus neuronas hicieron buenos enlaces hoy.",
        "Así como el agua talla la piedra, la constancia talla resultados.",
        "Experimento terminado. Ahora sí: bata colgada y a descansar.",
    )

    private val BIG = listOf(
        "¡Jornada de laboratorio completa! Eso fue un experimento de alto rendimiento.",
        "Tantos pomodoros merecen publicarse en una revista científica.",
        "Hoy fuiste un reactor de concentración. Recárgate bien.",
    )

    private val NONE = listOf(
        "Hoy el experimento no salió, y está bien: también es un dato. Mañana lo retomamos.",
        "No todas las reacciones ocurren al primer intento. Vuelve cuando quieras.",
        "Edison probó miles de filamentos antes del bueno. Mañana es otra prueba.",
    )

    /** Mensaje al terminar el plan; [seed] lo mantiene igual mientras se ve el resumen. */
    fun finished(workSessions: Int, seed: Long): String {
        val pool = when {
            workSessions == 0 -> NONE
            // Planes largos: a veces un mensaje especial
            workSessions >= 8 && Random(seed).nextBoolean() -> BIG
            else -> DONE
        }
        return pool[Random(seed + 1).nextInt(pool.size)]
    }
}
