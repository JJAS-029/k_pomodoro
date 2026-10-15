package com.jjas.labpomodoro.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.Discovery
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.glass
import com.jjas.labpomodoro.ui.components.localizedName
import com.jjas.labpomodoro.ui.theme.isLight
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Elementos recién ganados: salen de la boca del recipiente en una burbuja de cristal, como las
 * burbujas de notificación de Android, y se quedan flotando arriba. Tocarla abre la tabla;
 * arrastrarla lejos la descarta.
 *
 * @param fromY altura (desde arriba) de la boca del recipiente, de donde sale la burbuja.
 */
@Composable
fun ElementBubble(
    discoveries: List<Discovery>,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    fromY: Float = 0.22f,
) {
    val rewards = discoveries.filter { it.source != DiscoverySource.FUSION }
    if (rewards.isEmpty()) return
    val first = rewards.first()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    // Salida: 0 = en la boca del recipiente, 1 = en su lugar arriba a la derecha
    val rise = remember(first.id) { Animatable(0f) }
    LaunchedEffect(first.id) { rise.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing)) }
    val drag = remember(first.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    // Flota: sube y baja un poco cuando ya llegó
    val bob by rememberInfiniteTransition(label = "flota").animateFloat(
        -1f, 1f, infiniteRepeatable(tween(1_800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        val bubble = 76.dp
        val bubblePx = with(density) { bubble.toPx() }
        val margin = with(density) { 16.dp.toPx() }
        val start = Offset(with(density) { maxWidth.toPx() } / 2f - bubblePx / 2f, with(density) { maxHeight.toPx() } * fromY)
        val end = Offset(with(density) { maxWidth.toPx() } - bubblePx - margin, margin)
        val t = rise.value
        val arc = sin(PI * t).toFloat() * with(density) { 48.dp.toPx() }
        val x = start.x + (end.x - start.x) * t + drag.value.x
        val y = start.y + (end.y - start.y) * t - arc + drag.value.y + bob * t * with(density) { 3.dp.toPx() }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                .graphicsLayer {
                    val scale = 0.3f + 0.7f * t
                    scaleX = scale
                    scaleY = scale
                    // Se desvanece conforme se arrastra lejos
                    alpha = (1f - hypot(drag.value.x, drag.value.y) / (bubblePx * 3f)).coerceIn(0.2f, 1f)
                }
                .pointerInput(first.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (hypot(drag.value.x, drag.value.y) > bubblePx * 1.2f) {
                                onDismiss()
                            } else {
                                scope.launch { drag.animateTo(Offset.Zero, spring()) }
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        scope.launch { drag.snapTo(drag.value + amount) }
                    }
                },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(bubble).clip(CircleShape).clickable(onClick = onOpen)) {
                GlassOrb(first.element.category.glass(MaterialTheme.colorScheme.isLight).orb)
                ElementTile(first.element, discovered = true, size = 44.dp)
                if (rewards.size > 1) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    ) {
                        Text(
                            "+${rewards.size - 1}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            // El nombre aparece cuando la burbuja ya llegó
            if (t > 0.9f) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    modifier = Modifier.padding(top = 4.dp).graphicsLayer { alpha = (t - 0.9f) * 10f },
                ) {
                    Text(
                        if (rewards.size == 1) {
                            stringResource(R.string.el_banner_discovered, first.element.localizedName().lowercase(LocalConfiguration.current.locales[0]))
                        } else {
                            pluralStringResource(R.plurals.el_banner_new_elements, rewards.size, rewards.size)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

/** Esfera de cristal teñida del color de la familia, con brillo arriba y borde especular. */
@Composable
private fun GlassOrb(color: Color) {
    Canvas(Modifier.fillMaxSize()) {
        val r = size.minDimension / 2f
        drawCircle(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.55f),
                0.45f to color.copy(alpha = 0.45f),
                1f to color.copy(alpha = 0.85f),
                center = Offset(center.x - r * 0.3f, center.y - r * 0.35f),
                radius = r * 1.5f,
            ),
        )
        drawCircle(
            Brush.linearGradient(listOf(Color.White.copy(alpha = 0.9f), color.copy(alpha = 0.5f), color), start = Offset.Zero, end = Offset(size.width, size.height)),
            radius = r - r * 0.04f,
            style = Stroke(width = r * 0.08f),
        )
    }
}
