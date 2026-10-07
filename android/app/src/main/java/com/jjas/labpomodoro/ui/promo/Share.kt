package com.jjas.labpomodoro.ui.promo

import android.content.Context
import android.content.Intent

/** Enlace de la app en Google Play (funciona en cuanto esté publicada). */
const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.jjas.labpomodoro"

/** Textos para compartir; siempre terminan con el enlace para que un amigo pueda instalarla. */
object ShareText {

    fun invite(): String =
        "Estoy usando Lab Pomodoro 🧪: un timer de pomodoros que convierte tu tiempo de enfoque en " +
            "elementos de la tabla periódica. Pruébala: $PLAY_STORE_URL"

    fun finished(pomodoros: Int, focus: String): String =
        "¡Experimento completado! 🧪 Hoy hice $pomodoros ${if (pomodoros == 1) "pomodoro" else "pomodoros"} " +
            "($focus de enfoque) en Lab Pomodoro. ¿Te animas? $PLAY_STORE_URL"

    fun medal(title: String, description: String): String =
        "¡Gané la medalla «$title» en Lab Pomodoro! 🏅 ($description). ¿Te animas? $PLAY_STORE_URL"

    fun progress(discovered: Int, total: Int, bronze: Int, silver: Int, gold: Int, streak: Int, focus: String): String {
        val mastery = listOfNotNull(
            bronze.takeIf { it > 0 }?.let { "🥉$it" },
            silver.takeIf { it > 0 }?.let { "🥈$it" },
            gold.takeIf { it > 0 }?.let { "🥇$it" },
        ).joinToString(" ")
        return buildString {
            append("Llevo $discovered/$total elementos de la tabla periódica")
            if (mastery.isNotEmpty()) append(" ($mastery)")
            append(", $focus de enfoque")
            if (streak > 0) append(" y una racha de $streak ${if (streak == 1) "día" else "días"}")
            append(" en Lab Pomodoro 🧪. ¿Me alcanzas? $PLAY_STORE_URL")
        }
    }
}

/** Abre el menú de compartir de Android con [text]. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, "Compartir").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
