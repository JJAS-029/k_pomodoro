package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** Un recipiente de la repisa: una sesión del plan. */
data class ShelfItemUi(
    val shape: VesselShape,
    val color: Color,
    val fill: Float,
    val isCurrent: Boolean,
    val skipped: Boolean,
    /** Firma del elemento, si tiene; si no, los tonos salen del color. */
    val tones: LiquidTones? = null,
)

private val ItemHeight = 56.dp

/**
 * Repisa con la cristalería del plan: cada sesión tiene su recipiente (el mismo que se ve en grande).
 * Se desplaza sola para mantener visible la sesión en curso.
 */
@Composable
fun VesselShelf(items: List<ShelfItemUi>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    val currentIndex = items.indexOfFirst { it.isCurrent }
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
    }

    Box(modifier) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(ItemHeight + 6.dp)
                .padding(bottom = 4.dp),
        ) {
            itemsIndexed(items) { _, item -> ShelfVessel(item) }
        }
        // Tabla de madera de la repisa
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(5.dp)
                .align(Alignment.BottomCenter)
        ) {
            drawRoundRect(Color(0xFF5D4037), cornerRadius = CornerRadius(size.height / 2))
        }
    }
}

@Composable
private fun ShelfVessel(item: ShelfItemUi) {
    val fill by animateFloatAsState(item.fill.coerceIn(0f, 1f), tween(300), label = "shelf")
    val full = item.fill >= 1f && !item.skipped
    // Destello al llenarse (no al abrir la pantalla con recipientes ya llenos)
    val glow = remember { Animatable(0f) }
    var wasFull by remember { mutableStateOf(full) }
    LaunchedEffect(full) {
        if (full && !wasFull) {
            glow.snapTo(1f)
            glow.animateTo(0f, tween(1400))
        }
        wasFull = full
    }
    // El recipiente de la sesión actual burbujea suave
    val bubbles = if (item.isCurrent && !item.skipped) {
        rememberInfiniteTransition(label = "shelfBubbles")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "phase").value
    } else {
        null
    }
    Box(contentAlignment = Alignment.Center) {
        if (glow.value > 0f) {
            Canvas(Modifier.size(ItemHeight)) {
                val r = size.minDimension * 0.6f
                drawCircle(Brush.radialGradient(listOf(item.color.copy(alpha = 0.55f * glow.value), Color.Transparent), center, r), r)
            }
        }
        MiniVessel(
            shape = item.shape,
            fill = if (item.skipped) 0f else fill,
            liquidColor = item.color,
            outlineColor = when {
                item.isCurrent -> item.color
                item.skipped -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                else -> glassOutlineColor().copy(alpha = 0.7f)
            },
            // Recipiente punteado = sesión saltada
            dashed = item.skipped,
            // Los anchos (cristalizador) se achican para no ocupar media repisa
            modifier = Modifier.size(width = ItemHeight * item.shape.aspect.coerceAtMost(1f) + 4.dp, height = ItemHeight),
            bubbles = bubbles,
            tones = item.tones,
        )
    }
}
