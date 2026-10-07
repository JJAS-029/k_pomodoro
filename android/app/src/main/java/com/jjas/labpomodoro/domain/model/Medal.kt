package com.jjas.labpomodoro.domain.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.jjas.labpomodoro.R

enum class MedalTier(@StringRes val labelRes: Int) {
    BRONZE(R.string.prog_tier_bronze),
    SILVER(R.string.prog_tier_silver),
    GOLD(R.string.prog_tier_gold),
}

/**
 * Medallas por logros. Se calculan del historial cada vez (no se guardan), así que un respaldo
 * restaurado las trae de vuelta solo. Solo se guarda cuáles ya se celebraron.
 */
enum class Medal(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val symbol: String,
    val tier: MedalTier,
    val target: Long,
    /** Texto del progreso con su unidad, para "4/7 días"; el plural va por la meta. */
    @PluralsRes val progressRes: Int,
) {
    FIRST_POMODORO(R.string.prog_medal_first_pomodoro_title, R.string.prog_medal_first_pomodoro_desc, "🧪", MedalTier.BRONZE, 1, R.plurals.prog_medal_progress_pomodoros),
    STREAK_3(R.string.prog_medal_streak_3_title, R.string.prog_medal_streak_3_desc, "🔥", MedalTier.BRONZE, 3, R.plurals.prog_medal_progress_days),
    STREAK_7(R.string.prog_medal_streak_7_title, R.string.prog_medal_streak_7_desc, "🔥", MedalTier.SILVER, 7, R.plurals.prog_medal_progress_days),
    STREAK_30(R.string.prog_medal_streak_30_title, R.string.prog_medal_streak_30_desc, "🔥", MedalTier.GOLD, 30, R.plurals.prog_medal_progress_days),
    STREAK_100(R.string.prog_medal_streak_100_title, R.string.prog_medal_streak_100_desc, "☄️", MedalTier.GOLD, 100, R.plurals.prog_medal_progress_days),
    HOURS_10(R.string.prog_medal_hours_10_title, R.string.prog_medal_hours_10_desc, "⏳", MedalTier.BRONZE, 10, R.plurals.prog_medal_progress_hours),
    HOURS_50(R.string.prog_medal_hours_50_title, R.string.prog_medal_hours_50_desc, "⏳", MedalTier.SILVER, 50, R.plurals.prog_medal_progress_hours),
    HOURS_100(R.string.prog_medal_hours_100_title, R.string.prog_medal_hours_100_desc, "🎓", MedalTier.GOLD, 100, R.plurals.prog_medal_progress_hours),
    MARATHON(R.string.prog_medal_marathon_title, R.string.prog_medal_marathon_desc, "🏃", MedalTier.SILVER, 8, R.plurals.prog_medal_progress_pomodoros),
    EARLY_BIRD(R.string.prog_medal_early_bird_title, R.string.prog_medal_early_bird_desc, "🌅", MedalTier.SILVER, 5, R.plurals.prog_medal_progress_pomodoros),
    NIGHT_OWL(R.string.prog_medal_night_owl_title, R.string.prog_medal_night_owl_desc, "🦉", MedalTier.SILVER, 5, R.plurals.prog_medal_progress_pomodoros),
    FIRST_FUSION(R.string.prog_medal_first_fusion_title, R.string.prog_medal_first_fusion_desc, "⚗️", MedalTier.BRONZE, 1, R.plurals.prog_medal_progress_fusions),
    FUSION_MASTER(R.string.prog_medal_fusion_master_title, R.string.prog_medal_fusion_master_desc, "💥", MedalTier.GOLD, 25, R.plurals.prog_medal_progress_fusions),
    COLLECTOR(R.string.prog_medal_collector_title, R.string.prog_medal_collector_desc, "🔬", MedalTier.BRONZE, 10, R.plurals.prog_medal_progress_elements),
    HALF_TABLE(R.string.prog_medal_half_table_title, R.string.prog_medal_half_table_desc, "🧫", MedalTier.SILVER, 59, R.plurals.prog_medal_progress_elements),
    FULL_TABLE(R.string.prog_medal_full_table_title, R.string.prog_medal_full_table_desc, "🏆", MedalTier.GOLD, PeriodicTable.SIZE.toLong(), R.plurals.prog_medal_progress_elements),
    FIRST_GOLD(R.string.prog_medal_first_gold_title, R.string.prog_medal_first_gold_desc, "🥇", MedalTier.SILVER, 1, R.plurals.prog_medal_progress_elements),
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
    /** Lo que se muestra del progreso ("4" de "4/7 días"), sin pasarse de la meta. */
    val shown: Long get() = current.coerceAtMost(medal.target)
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
