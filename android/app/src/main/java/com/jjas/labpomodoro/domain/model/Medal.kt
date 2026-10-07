package com.jjas.labpomodoro.domain.model

enum class MedalTier(val label: String) {
    BRONZE("Bronce"),
    SILVER("Plata"),
    GOLD("Oro"),
}

/**
 * Medallas por logros. Se calculan del historial cada vez (no se guardan), así que un respaldo
 * restaurado las trae de vuelta solo. Solo se guarda cuáles ya se celebraron.
 */
enum class Medal(
    val title: String,
    val description: String,
    val symbol: String,
    val tier: MedalTier,
    val target: Long,
    /** Unidad del progreso, para "4/7 días". */
    val unit: String,
) {
    FIRST_POMODORO("Primer experimento", "Completa tu primer pomodoro", "🧪", MedalTier.BRONZE, 1, "pomodoro"),
    STREAK_3("Constancia", "Racha de 3 días seguidos", "🔥", MedalTier.BRONZE, 3, "días"),
    STREAK_7("Semana perfecta", "Racha de 7 días seguidos", "🔥", MedalTier.SILVER, 7, "días"),
    STREAK_30("Mes de laboratorio", "Racha de 30 días seguidos", "🔥", MedalTier.GOLD, 30, "días"),
    STREAK_100("Cien días", "Racha de 100 días seguidos", "☄️", MedalTier.GOLD, 100, "días"),
    HOURS_10("Becario", "10 horas de enfoque en total", "⏳", MedalTier.BRONZE, 10, "h"),
    HOURS_50("Investigador", "50 horas de enfoque en total", "⏳", MedalTier.SILVER, 50, "h"),
    HOURS_100("Doctorado", "100 horas de enfoque en total", "🎓", MedalTier.GOLD, 100, "h"),
    MARATHON("Maratón", "8 pomodoros en un mismo día", "🏃", MedalTier.SILVER, 8, "pomodoros"),
    EARLY_BIRD("Madrugador", "5 pomodoros empezados antes de las 7 de la mañana", "🌅", MedalTier.SILVER, 5, "pomodoros"),
    NIGHT_OWL("Búho nocturno", "5 pomodoros empezados después de las 10 de la noche", "🦉", MedalTier.SILVER, 5, "pomodoros"),
    FIRST_FUSION("Primera síntesis", "Fusiona dos elementos en el sintetizador", "⚗️", MedalTier.BRONZE, 1, "fusión"),
    FUSION_MASTER("Maestro de la fusión", "25 fusiones en el sintetizador", "💥", MedalTier.GOLD, 25, "fusiones"),
    COLLECTOR("Coleccionista", "Descubre 10 elementos", "🔬", MedalTier.BRONZE, 10, "elementos"),
    HALF_TABLE("Media tabla", "Descubre 59 elementos", "🧫", MedalTier.SILVER, 59, "elementos"),
    FULL_TABLE("Tabla completa", "Descubre los 118 elementos", "🏆", MedalTier.GOLD, PeriodicTable.SIZE.toLong(), "elementos"),
    FIRST_GOLD("Maestría de oro", "Lleva un elemento a la maestría de oro", "🥇", MedalTier.SILVER, 1, "elemento"),
}

/** Lo que se necesita del historial para calcular las medallas. */
data class MedalFacts(
    val longestStreak: Int = 0,
    val workSeconds: Long = 0,
    val completedPomodoros: Int = 0,
    val bestDayPomodoros: Int = 0,
    val earlyPomodoros: Int = 0,
    val latePomodoros: Int = 0,
    val fusions: Int = 0,
    val discovered: Int = 0,
    val goldElements: Int = 0,
)

data class MedalProgress(val medal: Medal, val current: Long) {
    val earned: Boolean get() = current >= medal.target
    val fraction: Float get() = (current.toFloat() / medal.target).coerceIn(0f, 1f)
    /** "4/7 días"; sin pasarse de la meta. */
    val label: String get() = "${current.coerceAtMost(medal.target)}/${medal.target} ${medal.unit}"
}

object Medals {

    /** Hora de inicio (0..23) que cuenta como madrugada y como noche. */
    val EARLY_HOURS = 4..6
    val LATE_HOURS = setOf(22, 23, 0, 1, 2, 3)

    fun evaluate(facts: MedalFacts): List<MedalProgress> = Medal.entries.map { medal ->
        val current: Long = when (medal) {
            Medal.FIRST_POMODORO -> facts.completedPomodoros.toLong()
            Medal.STREAK_3, Medal.STREAK_7, Medal.STREAK_30, Medal.STREAK_100 -> facts.longestStreak.toLong()
            Medal.HOURS_10, Medal.HOURS_50, Medal.HOURS_100 -> facts.workSeconds / 3600
            Medal.MARATHON -> facts.bestDayPomodoros.toLong()
            Medal.EARLY_BIRD -> facts.earlyPomodoros.toLong()
            Medal.NIGHT_OWL -> facts.latePomodoros.toLong()
            Medal.FIRST_FUSION, Medal.FUSION_MASTER -> facts.fusions.toLong()
            Medal.COLLECTOR, Medal.HALF_TABLE, Medal.FULL_TABLE -> facts.discovered.toLong()
            Medal.FIRST_GOLD -> facts.goldElements.toLong()
        }
        MedalProgress(medal, current)
    }

    /** Las medallas ganadas que aún no se celebraron, en orden del catálogo. */
    fun unseen(progress: List<MedalProgress>, seen: Set<String>): List<Medal> =
        progress.filter { it.earned && it.medal.name !in seen }.map { it.medal }
}
