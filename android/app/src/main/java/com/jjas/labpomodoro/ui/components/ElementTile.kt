package com.jjas.labpomodoro.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory
import com.jjas.labpomodoro.domain.model.Mastery

/** Color de cada familia de la tabla periódica, en tonos suaves sobre fondo negro. */
fun ElementCategory.color(): Color = when (this) {
    ElementCategory.ALKALI_METAL -> Color(0xFFEF9A9A)
    ElementCategory.ALKALINE_EARTH_METAL -> Color(0xFFFFCC80)
    ElementCategory.TRANSITION_METAL -> Color(0xFF90CAF9)
    ElementCategory.POST_TRANSITION_METAL -> Color(0xFFB0BEC5)
    ElementCategory.METALLOID -> Color(0xFFC5E1A5)
    ElementCategory.NONMETAL -> Color(0xFF80CBC4)
    ElementCategory.HALOGEN -> Color(0xFFFFF59D)
    ElementCategory.NOBLE_GAS -> Color(0xFFCE93D8)
    ElementCategory.LANTHANIDE -> Color(0xFFF48FB1)
    ElementCategory.ACTINIDE -> Color(0xFFBCAAA4)
    ElementCategory.UNKNOWN -> Color(0xFF9E9E9E)
}

/**
 * Casilla de la tabla periódica. Si aún no se descubre se ve solo el contorno con el símbolo
 * apagado, para que se note lo que falta sin revelar el color.
 */
@Composable
fun ElementTile(
    element: Element,
    discovered: Boolean,
    modifier: Modifier = Modifier,
    quantity: Int = 0,
    size: Dp = 40.dp,
    highlighted: Boolean = false,
    mastery: Mastery = Mastery.NONE,
) {
    val color = element.category.color()
    val border = when {
        highlighted -> BorderStroke(2.dp, Color.White)
        mastery == Mastery.GOLD -> BorderStroke(2.dp, goldShimmer(size))
        mastery == Mastery.SILVER -> BorderStroke(1.8.dp, SilverColor)
        mastery == Mastery.BRONZE -> BorderStroke(1.8.dp, BronzeColor)
        else -> BorderStroke(1.dp, if (discovered) color else MaterialTheme.colorScheme.outline)
    }
    val shape = RoundedCornerShape(size * 0.14f)
    val scale = size.value / 40f
    // Casillas chicas (tabla completa en vertical): solo el símbolo, más grande en proporción
    val compact = size < 32.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (discovered) color.copy(alpha = 0.85f) else Color.Transparent)
            .border(border, shape)
            .semantics {
                contentDescription = buildString {
                    append(element.name)
                    if (!discovered) append(", sin descubrir") else if (quantity > 0) append(", $quantity")
                }
            },
    ) {
        val textColor = if (discovered) Color(0xFF111111) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        if (!compact) {
            Text(
                text = element.atomicNumber.toString(),
                fontSize = (7.5f * scale).sp,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = (3 * scale).dp, top = (1 * scale).dp),
            )
        }
        Text(
            text = element.symbol,
            fontSize = (if (compact) 18f * scale else 14f * scale).sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            modifier = Modifier.align(Alignment.Center),
        )
        if (!compact && discovered && quantity > 1) {
            Text(
                text = "×$quantity",
                fontSize = (7.5f * scale).sp,
                color = textColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = (3 * scale).dp, bottom = (1 * scale).dp),
            )
        }
    }
}

val BronzeColor = Color(0xFFCD7F32)
val SilverColor = Color(0xFFD8DEE4)
val GoldColor = Color(0xFFFFC94A)

/** Color del nivel de maestría; null para los niveles sin metal. */
fun Mastery.metalColor(): Color? = when (this) {
    Mastery.BRONZE -> BronzeColor
    Mastery.SILVER -> SilverColor
    Mastery.GOLD -> GoldColor
    else -> null
}

/** Contorno dorado con un destello que lo cruza cada pocos segundos. */
@Composable
private fun goldShimmer(size: Dp): Brush {
    val transition = rememberInfiniteTransition(label = "oro")
    val sweep by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3_200, easing = LinearEasing)),
        label = "destello",
    )
    val px = with(LocalDensity.current) { size.toPx() }
    return Brush.linearGradient(
        0f to GoldColor,
        0.4f to GoldColor,
        0.5f to Color.White,
        0.6f to GoldColor,
        1f to GoldColor,
        start = Offset(px * sweep - px, 0f),
        end = Offset(px * sweep, px),
    )
}

