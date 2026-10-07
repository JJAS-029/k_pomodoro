package com.jjas.labpomodoro.ui.medals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.MedalRepository
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.MedalTier
import com.jjas.labpomodoro.ui.promo.ShareText
import com.jjas.labpomodoro.ui.promo.shareText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedalViewModel @Inject constructor(private val medals: MedalRepository) : ViewModel() {

    /** Medallas ganadas que aún no se han celebrado. */
    val unseen: StateFlow<List<Medal>> = medals.unseen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismiss() {
        val current = unseen.value
        viewModelScope.launch { medals.markSeen(current) }
    }
}

/** Colores claro y oscuro del metal de cada nivel. */
fun MedalTier.colors(): List<Color> = when (this) {
    MedalTier.BRONZE -> listOf(Color(0xFFF0B985), Color(0xFF9A5B2A))
    MedalTier.SILVER -> listOf(Color(0xFFF5F7FA), Color(0xFF8E98A3))
    MedalTier.GOLD -> listOf(Color(0xFFFFE680), Color(0xFFC08A12))
}

/**
 * Medalla redonda: ganada, de metal con su símbolo; sin ganar, apagada y con un anillo que muestra
 * cuánto falta.
 */
@Composable
fun MedalBadge(progress: MedalProgress, size: Dp, modifier: Modifier = Modifier) {
    val (light, dark) = progress.medal.tier.colors()
    val track = MaterialTheme.colorScheme.outlineVariant
    val ring = MaterialTheme.colorScheme.primary
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (progress.earned) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(light, dark)), CircleShape)
                    .border(BorderStroke(size * 0.06f, Brush.linearGradient(listOf(dark, light))), CircleShape),
            )
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, CircleShape))
            Canvas(Modifier.fillMaxSize()) {
                val stroke = this.size.minDimension * 0.06f
                val inset = stroke / 2
                val arc = Size(this.size.width - stroke, this.size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
                if (progress.fraction > 0f) {
                    drawArc(ring, -90f, 360f * progress.fraction, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Text(
            progress.medal.symbol,
            fontSize = (size.value * 0.42f).sp,
            modifier = Modifier.alpha(if (progress.earned) 1f else 0.3f),
        )
    }
}

/** Todas las medallas, cuatro por fila; al tocar una se ve su detalle. */
@Composable
fun MedalGrid(medals: List<MedalProgress>, onSelect: (MedalProgress) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        medals.chunked(COLUMNS).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { progress ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(progress) }
                            .padding(vertical = 4.dp),
                    ) {
                        MedalBadge(progress, 56.dp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            progress.medal.title,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            color = if (progress.earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Detalle de una medalla: qué pide, cuánto falta y, si ya se ganó, compartirla. */
@Composable
fun MedalDialog(progress: MedalProgress, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val medal = progress.medal
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { MedalBadge(progress, 88.dp) },
        title = { Text(medal.title, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(medal.description, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                if (progress.earned) {
                    Text(
                        "¡Ganada! · Medalla de ${medal.tier.label.lowercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(progress.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        dismissButton = if (progress.earned) {
            { TextButton(onClick = { context.shareText(ShareText.medal(medal.title, medal.description)) }) { Text("Compartir") } }
        } else {
            null
        },
    )
}

/** Aviso de medallas recién ganadas en la pantalla principal. */
@Composable
fun MedalBanner(medals: List<Medal>, onOpen: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (medals.isEmpty()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        ) {
            // La de mayor nivel al frente
            val best = medals.maxBy { it.tier }
            MedalBadge(MedalProgress(best, best.target), 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (medals.size == 1) "¡Nueva medalla!" else "¡${medals.size} medallas nuevas!",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (medals.size == 1) medals[0].title else medals.joinToString(" · ") { it.title },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onDismiss) { Text("Cerrar") }
            TextButton(onClick = onOpen) { Text("Ver") }
        }
    }
}

private const val COLUMNS = 4
