package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.SessionType

enum class DotState { DONE, SKIPPED, CURRENT, PENDING }

/** Un pomodoro del plan, como punto. [cycleEnd] = después viene un descanso largo. */
data class PlanDot(val state: DotState, val color: Color, val cycleEnd: Boolean)

/** Lo que sigue en el plan: tipo y color; null = el plan termina. */
data class UpNext(val type: SessionType, val color: Color, val minutes: Int)

private const val MAX_DOTS = 16

/**
 * Indicador simbólico de la sesión: "ahora → después" con íconos y una fila de puntos, uno por
 * pomodoro, separados por ciclos de descanso largo. Reemplaza a "Sesión 3 de 9 · Después: ...".
 */
@Composable
fun SessionIndicator(
    current: SessionType,
    currentColor: Color,
    next: UpNext?,
    dots: List<PlanDot>,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(painterResource(current.icon()), contentDescription = null, tint = currentColor, modifier = Modifier.size(26.dp))
            Icon(
                painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp),
            )
            if (next == null) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            } else {
                Icon(painterResource(next.type.icon()), contentDescription = null, tint = next.color.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
                Text(
                    "${next.minutes}′",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.size(10.dp))
        if (dots.size <= MAX_DOTS) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                dots.forEachIndexed { i, dot ->
                    PlanDotView(dot)
                    if (i < dots.lastIndex) Spacer(Modifier.width(if (dot.cycleEnd) 14.dp else 6.dp))
                }
            }
        } else {
            // Planes largos: los puntos no caben, se muestra la cuenta
            val done = dots.count { it.state == DotState.DONE }
            Text(
                "$done / ${dots.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PlanDotView(dot: PlanDot, size: Dp = 9.dp) {
    val dim = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(size)
            .then(
                when (dot.state) {
                    DotState.DONE -> Modifier.background(dot.color, CircleShape)
                    DotState.CURRENT -> Modifier.border(2.dp, dot.color, CircleShape)
                    DotState.SKIPPED -> Modifier.border(1.dp, dim, CircleShape)
                    DotState.PENDING -> Modifier.background(dim, CircleShape)
                }
            )
    )
}

fun SessionType.icon(): Int = when (this) {
    SessionType.WORK -> R.drawable.ic_stat_timer
    SessionType.SHORT_BREAK -> R.drawable.ic_cup
    SessionType.LONG_BREAK -> R.drawable.ic_moon
}
