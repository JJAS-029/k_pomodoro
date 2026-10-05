package com.jjas.labpomodoro.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory

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
) {
    val color = element.category.color()
    val shape = RoundedCornerShape(size * 0.14f)
    val scale = size.value / 40f
    // Casillas chicas (tabla completa en vertical): solo el símbolo, más grande en proporción
    val compact = size < 32.dp
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (discovered) color.copy(alpha = 0.85f) else Color.Transparent)
            .border(
                if (highlighted) BorderStroke(2.dp, Color.White)
                else BorderStroke(1.dp, if (discovered) color else MaterialTheme.colorScheme.outline),
                shape,
            )
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
