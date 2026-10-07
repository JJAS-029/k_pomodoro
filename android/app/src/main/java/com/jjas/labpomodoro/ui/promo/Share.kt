package com.jjas.labpomodoro.ui.promo

import android.content.Context
import android.content.Intent
import com.jjas.labpomodoro.R

/** Enlace de la app en Google Play (funciona en cuanto esté publicada). */
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.jjas.labpomodoro"

/** Textos para compartir; siempre terminan con el enlace para que un amigo pueda instalarla. */
object ShareText {

    fun invite(context: Context): String = context.getString(R.string.main_share_invite, PLAY_STORE_URL)

    fun finished(context: Context, pomodoros: Int, focus: String): String =
        context.resources.getQuantityString(R.plurals.main_share_finished, pomodoros, pomodoros, focus, PLAY_STORE_URL)

    fun medal(context: Context, title: String, description: String): String =
        context.getString(R.string.main_share_medal, title, description, PLAY_STORE_URL)

    fun progress(
        context: Context,
        discovered: Int,
        total: Int,
        bronze: Int,
        silver: Int,
        gold: Int,
        streak: Int,
        focus: String,
    ): String {
        val mastery = listOfNotNull(
            bronze.takeIf { it > 0 }?.let { "🥉$it" },
            silver.takeIf { it > 0 }?.let { "🥈$it" },
            gold.takeIf { it > 0 }?.let { "🥇$it" },
        ).joinToString(" ")
        val masteryText = if (mastery.isNotEmpty()) " ($mastery)" else ""
        val streakText = if (streak > 0) {
            context.resources.getQuantityString(R.plurals.main_share_progress_streak, streak, streak)
        } else {
            ""
        }
        return context.getString(R.string.main_share_progress, discovered, total, masteryText, focus, streakText, PLAY_STORE_URL)
    }
}

/** Abre el menú de compartir de Android con [text]. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, getString(R.string.main_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
