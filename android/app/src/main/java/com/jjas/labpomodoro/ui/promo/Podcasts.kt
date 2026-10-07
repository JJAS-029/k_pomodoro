package com.jjas.labpomodoro.ui.promo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jjas.labpomodoro.R

/** Podcasts del creador de la app: se promocionan en los espacios de anuncios de la versión gratis. */
enum class Podcast(
    val title: String,
    @param:StringRes val taglineRes: Int,
    @param:StringRes val descriptionRes: Int,
    @param:DrawableRes val cover: Int,
    val spotifyUrl: String,
) {
    CIENCIAFICCION(
        title = "CiencIAficción",
        taglineRes = R.string.main_podcast_cienciaficcion_tagline,
        descriptionRes = R.string.main_podcast_cienciaficcion_description,
        cover = R.drawable.podcast_cienciaficcion,
        spotifyUrl = "https://open.spotify.com/show/4fYtxwJXvYiXTBmjdwMyNY",
    ),
    CIENCIA(
        title = "CiencIA",
        taglineRes = R.string.main_podcast_ciencia_tagline,
        descriptionRes = R.string.main_podcast_ciencia_description,
        cover = R.drawable.podcast_ciencia,
        spotifyUrl = "https://open.spotify.com/show/3Ez2gZpy8e9orhMqNRwXLl",
    ),
}

const val GITHUB_URL = "https://github.com/JJAS-029"

/** Correo para sugerencias y reportes. */
const val SUGGESTIONS_EMAIL = "ciencia.koala@gmail.com"

/** Abre el correo con el asunto y la versión ya escritos, para que solo haya que contar la idea. */
fun Context.sendSuggestion(appVersion: String) {
    val body = "\n\n—\nLab Pomodoro $appVersion · Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}"
    val uri = "mailto:$SUGGESTIONS_EMAIL?subject=${Uri.encode(getString(R.string.main_suggestion_subject))}&body=${Uri.encode(body)}".toUri()
    runCatching { startActivity(Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Abre un enlace: Spotify lo toma si está instalado, si no el navegador. */
fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Franja tipo banner con la portada del podcast, en lugar de un anuncio de AdMob. */
@Composable
fun PodcastBanner(podcast: Podcast, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clickable { context.openUrl(podcast.spotifyUrl) },
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(podcast.cover),
                contentDescription = null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.main_podcast_banner_title, podcast.title), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    stringResource(podcast.taglineRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { context.openUrl(podcast.spotifyUrl) }) { Text(stringResource(R.string.main_podcast_listen)) }
        }
    }
}

/** Anuncio propio de pantalla completa al terminar el plan: el podcast para el descanso. */
@Composable
fun PodcastPromoDialog(podcast: Podcast, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                context.openUrl(podcast.spotifyUrl)
                onDismiss()
            }) { Text(stringResource(R.string.main_podcast_listen_spotify)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.main_podcast_not_now)) } },
        title = { Text(stringResource(R.string.main_podcast_promo_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painterResource(podcast.cover),
                    contentDescription = stringResource(R.string.main_podcast_cover, podcast.title),
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(podcast.title, style = MaterialTheme.typography.titleLarge)
                Text(stringResource(podcast.descriptionRes), style = MaterialTheme.typography.bodyMedium)
            }
        },
    )
}
