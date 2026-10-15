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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.MedalRepository
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.MedalProgress
import com.jjas.labpomodoro.domain.model.MedalTier
import com.jjas.labpomodoro.ui.promo.ShareText
import com.jjas.labpomodoro.ui.promo.shareText
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import com.jjas.labpomodoro.ui.components.LocalGlassShimmer
import com.jjas.labpomodoro.ui.components.rememberGlassShimmer
import com.jjas.labpomodoro.ui.components.rememberLiquidTilt
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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

/** Cristal de cada metal: brillo, cuerpo, fondo hondo (del mismo tono, sin gris) y resplandor. */
private class MedalGlass(val highlight: Color, val base: Color, val deep: Color, val glow: Color)

private fun MedalTier.glass(): MedalGlass = when (this) {
    MedalTier.BRONZE -> MedalGlass(Color(0xFFFFCBA4), Color(0xFFD97742), Color(0xFF5C240E), Color(0xFFFF9E6D))
    MedalTier.SILVER -> MedalGlass(Color.White, Color(0xFF94A3B8), Color(0xFF1E293B), Color(0xFFE2E8F0))
    MedalTier.GOLD -> MedalGlass(Color(0xFFFFF59D), Color(0xFFF59E0B), Color(0xFF78350F), Color(0xFFFFD54F))
}

/** Arco de avance de las medallas sin ganar: índigo que termina en una punta brillante. */
private val ArcDark = listOf(Color(0xFF6366F1), Color(0xFFA5B4FC), Color.White)
private val ArcLight = listOf(Color(0xFF4F46E5), Color(0xFF6366F1), Color(0xFF818CF8))

/**
 * Medalla de cristal: ganada, una esfera del metal con halo, anillo grabado, borde especular y
 * domo de lente encima del símbolo (plata y oro con destello); sin ganar, cristal ahumado con un
 * arco que muestra cuánto falta y, si [showPercent], el porcentaje.
 * [lean] (-1..1) corre el reflejo del domo hacia donde se inclina el teléfono.
 */
@Composable
fun MedalBadge(
    progress: MedalProgress,
    size: Dp,
    modifier: Modifier = Modifier,
    showPercent: Boolean = true,
    lean: (() -> Float)? = null,
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val earned = progress.earned
    val tier = progress.medal.tier
    val shimmer: State<Float>? = if (earned && tier != MedalTier.BRONZE) LocalGlassShimmer.current ?: rememberGlassShimmer() else null
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            if (earned) drawEarnedBody(tier.glass(), tier == MedalTier.GOLD, light) else drawLockedBody(progress.fraction, light)
        }
        Text(
            progress.medal.symbol,
            fontSize = (size.value * 0.4f).sp,
            modifier = Modifier.graphicsLayer {
                alpha = if (earned) 1f else 0.35f
                val scale = if (earned) 1f else 0.92f
                scaleX = scale
                scaleY = scale
            },
        )
        Canvas(Modifier.fillMaxSize()) {
            drawDome(earned, light, lean?.invoke() ?: 0f)
            if (shimmer != null) drawMedalShimmer(shimmer.value, tier == MedalTier.GOLD)
        }
        if (!earned && showPercent && progress.fraction > 0f) {
            val accent = if (light) Color(0xFF4F46E5) else Color(0xFF818CF8)
            Text(
                "${(progress.fraction * 100).roundToInt()}%",
                fontSize = (size.value * 0.16f).coerceIn(8f, 11f).sp,
                fontWeight = FontWeight.Bold,
                color = if (light) accent else Color(0xFFC7D2FE),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = size * 0.06f)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), CircleShape)
                    .border(1.dp, accent.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 5.dp),
            )
        }
    }
}

/** Radio del cuerpo: el resto, hasta el borde del lienzo, es para el halo. */
private fun DrawScope.bodyRadius() = size.minDimension / 2f * 0.86f

/**
 * Medalla ganada, neón cristalino: vidrio oscuro con el metal encendido por dentro (desde abajo),
 * anillo grabado y aro neón del color del nivel. En el tema claro, el vidrio es claro y el aro del
 * metal pleno.
 */
private fun DrawScope.drawEarnedBody(g: MedalGlass, gold: Boolean, light: Boolean) {
    val r = bodyRadius()
    val c = center
    // Halo: degradado radial en vez de desenfoque, que en Android 8 no se ve
    drawCircle(
        Brush.radialGradient(0.72f to g.glow.copy(alpha = if (gold) 0.55f else 0.35f), 1f to Color.Transparent, center = c, radius = size.minDimension / 2f),
        radius = size.minDimension / 2f,
    )
    val body = if (light) {
        Brush.radialGradient(
            0f to Color.White,
            0.5f to g.highlight.copy(alpha = 0.9f),
            1f to g.base.copy(alpha = 0.85f),
            center = Offset(c.x, c.y + r * 0.3f),
            radius = r * 1.3f,
        )
    } else {
        Brush.radialGradient(
            0f to g.base.copy(alpha = 0.6f),
            0.6f to g.deep.copy(alpha = 0.9f),
            1f to Color(0xFF07080B),
            center = Offset(c.x, c.y + r * 0.35f),
            radius = r * 1.3f,
        )
    }
    drawCircle(body, radius = r)
    // Anillo grabado, como la tapa de una cápsula
    drawCircle((if (light) g.deep else g.glow).copy(alpha = 0.45f), radius = r * 0.8f, style = Stroke(width = r * 0.03f))
    // Aro neón: un trazo ancho y tenue con uno fino y brillante encima
    val ring = if (light) g.base else g.glow
    drawCircle(ring.copy(alpha = 0.35f), radius = r, style = Stroke(width = r * 0.16f))
    drawCircle(ring, radius = r, style = Stroke(width = r * 0.06f))
}

private fun DrawScope.drawLockedBody(fraction: Float, light: Boolean) {
    val r = bodyRadius()
    val c = center
    val body = if (light) {
        listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
    } else {
        listOf(Color(0xFF94A3B8).copy(alpha = 0.16f), Color(0xFF1E293B).copy(alpha = 0.5f), Color(0xFF090D16).copy(alpha = 0.92f))
    }
    drawCircle(
        Brush.radialGradient(0f to body[0], 0.58f to body[1], 1f to body[2], center = Offset(c.x - r * 0.3f, c.y - r * 0.3f), radius = r * 1.45f),
        radius = r,
    )
    val stroke = r * 0.09f
    drawCircle((if (light) Color.Black else Color.White).copy(alpha = 0.08f), radius = r, style = Stroke(stroke))
    if (fraction <= 0f) return
    val colors = if (light) ArcLight else ArcDark
    val sweep = 360f * fraction
    // El degradado de barrido empieza a las 3; se gira para que empiece arriba con el arco
    rotate(-90f) {
        drawArc(
            Brush.sweepGradient(0f to colors[0], fraction * 0.7f to colors[1], fraction to colors[2], center = c),
            startAngle = 0f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - r, c.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
    // Punta brillante al final del arco
    val end = Math.toRadians((sweep - 90f).toDouble())
    val tip = Offset(c.x + r * cos(end).toFloat(), c.y + r * sin(end).toFloat())
    drawCircle(Color.White, radius = stroke * 0.62f, center = tip)
    if (light) drawCircle(colors[0], radius = stroke * 0.62f, center = tip, style = Stroke(stroke * 0.25f))
}

/** Domo de lente sobre el símbolo: el reflejo que lo encierra dentro del cristal. */
private fun DrawScope.drawDome(earned: Boolean, light: Boolean, lean: Float) {
    val r = bodyRadius()
    val alpha = when {
        earned -> if (light) 0.5f else 0.38f
        else -> if (light) 0.45f else 0.14f
    }
    drawCircle(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = alpha * 0.13f), Color.Transparent),
            center = Offset(center.x - r * 0.25f + lean * r * 0.35f, center.y - r * 0.32f),
            radius = r * 0.75f,
        ),
        radius = r * 0.97f,
    )
}

/** Destello diagonal que cruza plata y oro, recortado a la esfera. */
private fun DrawScope.drawMedalShimmer(phase: Float, gold: Boolean) {
    val r = bodyRadius()
    val x = size.width * (phase * 3f - 1f)
    val sphere = Path().apply { addOval(Rect(center, r)) }
    clipPath(sphere) {
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = if (gold) 0.4f else 0.28f), Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + size.width * 0.45f, size.height),
            ),
        )
    }
}

/** Qué medallas resaltar: las demás se apagan. */
enum class MedalFilter { ALL, EARNED, IN_PROGRESS }

/**
 * Cabecera de las medallas: cuántas de cada metal, barra de avance general (bronce → plata → oro)
 * y filtro para resaltar ganadas o en curso.
 */
@Composable
fun MedalHeader(medals: List<MedalProgress>, filter: MedalFilter, onFilter: (MedalFilter) -> Unit) {
    val earned = medals.count { it.earned }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MedalTier.entries.reversed().forEach { tier ->
                val count = medals.count { it.earned && it.medal.tier == tier }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Box(Modifier.size(7.dp).background(tier.glass().base, CircleShape))
                    Spacer(Modifier.width(5.dp))
                    Text("$count ${stringResource(tier.labelRes)}", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(Color.Gray.copy(alpha = 0.18f), cornerRadius = radius)
            val w = size.width * earned / medals.size.coerceAtLeast(1)
            if (w > 0f) {
                drawRoundRect(
                    Brush.horizontalGradient(listOf(Color(0xFFD97742), Color(0xFFCBD5E1), Color(0xFFFBBF24)), endX = size.width),
                    size = Size(w, size.height),
                    cornerRadius = radius,
                )
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val counts = mapOf(
                MedalFilter.ALL to medals.size,
                MedalFilter.EARNED to earned,
                MedalFilter.IN_PROGRESS to medals.size - earned,
            )
            MedalFilter.entries.forEach { option ->
                FilterChip(
                    selected = filter == option,
                    onClick = { onFilter(option) },
                    label = {
                        Text(
                            stringResource(
                                when (option) {
                                    MedalFilter.ALL -> R.string.prog_medal_filter_all
                                    MedalFilter.EARNED -> R.string.prog_medal_filter_earned
                                    MedalFilter.IN_PROGRESS -> R.string.prog_medal_filter_progress
                                },
                                counts.getValue(option),
                            ),
                        )
                    },
                )
            }
        }
    }
}

/** Todas las medallas, cuatro por fila; al tocar una se ve su detalle. Las que no pasan el filtro se apagan. */
@Composable
fun MedalGrid(medals: List<MedalProgress>, onSelect: (MedalProgress) -> Unit, filter: MedalFilter = MedalFilter.ALL) {
    val anyShiny = medals.any { it.earned && it.medal.tier != MedalTier.BRONZE }
    CompositionLocalProvider(LocalGlassShimmer provides if (anyShiny) rememberGlassShimmer() else null) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            medals.chunked(COLUMNS).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { progress ->
                        val shown = when (filter) {
                            MedalFilter.ALL -> true
                            MedalFilter.EARNED -> progress.earned
                            MedalFilter.IN_PROGRESS -> !progress.earned
                        }
                        val alpha by animateFloatAsState(if (shown) 1f else 0.16f, label = "filtro")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { this.alpha = alpha }
                                .clickable { onSelect(progress) }
                                .padding(vertical = 4.dp),
                        ) {
                            MedalBadge(progress, 64.dp)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(progress.medal.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (progress.earned) FontWeight.SemiBold else FontWeight.Normal,
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
}

/** Detalle de una medalla: qué pide, cuánto falta y, si ya se ganó, compartirla. */
@Composable
fun MedalDialog(progress: MedalProgress, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val medal = progress.medal
    val title = stringResource(medal.titleRes)
    val description = stringResource(medal.descriptionRes)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            // Se ladea con el teléfono y el reflejo se corre al mismo lado, como la ficha del elemento
            val tilt = rememberLiquidTilt(enabled = true)
            val lean = remember(tilt) { { (tilt.value / TILT_RANGE).coerceIn(-1f, 1f) } }
            MedalBadge(
                progress,
                112.dp,
                showPercent = false,
                lean = lean,
                modifier = Modifier.graphicsLayer {
                    val t = lean()
                    rotationY = t * MAX_ROTATION
                    rotationX = -abs(t) * MAX_ROTATION * 0.35f
                    cameraDistance = 14f * density
                },
            )
        },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(description, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                if (progress.earned) {
                    Text(
                        stringResource(R.string.prog_medal_earned, stringResource(medal.tier.labelRes).lowercase(locale)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    LinearProgressIndicator(progress = { progress.fraction }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        pluralStringResource(medal.progressRes, medal.target.toInt(), progress.shown, medal.target),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.prog_close)) } },
        dismissButton = if (progress.earned) {
            {
                TextButton(onClick = { context.shareText(ShareText.medal(context, title, description)) }) {
                    Text(stringResource(R.string.prog_share))
                }
            }
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
                    pluralStringResource(R.plurals.prog_new_medals, medals.size, medals.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    medals.map { stringResource(it.titleRes) }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.prog_close)) }
            TextButton(onClick = onOpen) { Text(stringResource(R.string.prog_view)) }
        }
    }
}

private const val COLUMNS = 4

/** Inclinación del teléfono que lleva la medalla del detalle a su giro máximo. */
private const val TILT_RANGE = 0.3f

/** Giro máximo de la medalla del detalle, en grados. */
private const val MAX_ROTATION = 12f
