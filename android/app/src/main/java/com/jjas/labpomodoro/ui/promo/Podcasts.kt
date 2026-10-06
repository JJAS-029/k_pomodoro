package com.jjas.labpomodoro.ui.promo

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.jjas.labpomodoro.R

/** Podcasts del creador de la app: se promocionan en los espacios de anuncios de la versión gratis. */
enum class Podcast(
    val title: String,
    val tagline: String,
    val description: String,
    @param:DrawableRes val cover: Int,
    val spotifyUrl: String,
) {
    CIENCIAFICCION(
        title = "CiencIAficción",
        tagline = "Relatos de ciencia ficción narrados con IA",
        description = "Aventuras intergalácticas, futuros distópicos y tecnologías inimaginables de diversos autores, narrados con inteligencia artificial.",
        cover = R.drawable.podcast_cienciaficcion,
        spotifyUrl = "https://open.spotify.com/show/4fYtxwJXvYiXTBmjdwMyNY",
    ),
    CIENCIA(
        title = "CiencIA",
        tagline = "Ciencia y tesis narradas con IA",
        description = "Artículos científicos y tesis narrados con inteligencia artificial, para que el conocimiento llegue a todos.",
        cover = R.drawable.podcast_ciencia,
        spotifyUrl = "https://open.spotify.com/show/3Ez2gZpy8e9orhMqNRwXLl",
    ),
}

const val GITHUB_URL = "https://github.com/JJAS-029"

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
                Text("Podcast · ${podcast.title}", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    podcast.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = { context.openUrl(podcast.spotifyUrl) }) { Text("Escuchar") }
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
            }) { Text("Escuchar en Spotify") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ahora no") } },
        title = { Text("Para tu descanso") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painterResource(podcast.cover),
                    contentDescription = "Portada de ${podcast.title}",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(4.dp))
                Text(podcast.title, style = MaterialTheme.typography.titleLarge)
                Text(podcast.description, style = MaterialTheme.typography.bodyMedium)
            }
        },
    )
}
