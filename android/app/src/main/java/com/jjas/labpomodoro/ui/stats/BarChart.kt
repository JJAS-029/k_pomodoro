package com.jjas.labpomodoro.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R

/**
 * Columnas de una sola serie: todas del mismo tono y la más alta (o la que se toca) al color pleno.
 * La etiqueta de valor solo va en la barra destacada; el resto se ve tocando la barra.
 *
 * @param labels etiqueta del eje por barra ("" para dejarla vacía).
 * @param describe texto completo de una barra para la etiqueta superior y lectores de pantalla.
 */
@Composable
fun BarChart(
    values: List<Long>,
    labels: List<String>,
    describe: (Int) -> String,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
) {
    val peak = values.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
    var selected by remember(values) { mutableStateOf<Int?>(null) }
    val focus = selected ?: peak
    val accent = MaterialTheme.colorScheme.primary
    val muted = accent.copy(alpha = 0.38f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    val noData = stringResource(R.string.prog_chart_no_data)
    val noDataPeriod = stringResource(R.string.prog_chart_no_data_period)

    Column(
        modifier.semantics {
            contentDescription = values.indices.filter { values[it] > 0 }.joinToString(". ") { describe(it) }
                .ifEmpty { noData }
        },
    ) {
        // Lo que dice la barra destacada (la más alta o la que se tocó)
        Text(
            text = focus?.let { describe(it) } ?: noDataPeriod,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(values) {
                    detectTapGestures { offset ->
                        val slot = size.width / values.size.coerceAtLeast(1)
                        selected = (offset.x / slot).toInt().coerceIn(0, values.lastIndex)
                    }
                },
        ) {
            val slot = size.width / values.size.coerceAtLeast(1)
            // Barras de máximo 24 dp y siempre con aire entre ellas
            val barWidth = minOf(24.dp.toPx(), slot - 2.dp.toPx()).coerceAtLeast(1f)
            val radius = minOf(4.dp.toPx(), barWidth / 2)
            values.forEachIndexed { i, v ->
                if (v <= 0) return@forEachIndexed
                val h = (v.toFloat() / max) * size.height
                val left = i * slot + (slot - barWidth) / 2
                // Punta redondeada arriba, recta en la base
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = size.height - h,
                            right = left + barWidth,
                            bottom = size.height,
                            topLeftCornerRadius = CornerRadius(radius),
                            topRightCornerRadius = CornerRadius(radius),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        )
                    )
                }
                drawPath(path, if (i == focus) accent else muted)
            }
            // Línea base fina
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach { label ->
                Box(Modifier.weight(1f)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
