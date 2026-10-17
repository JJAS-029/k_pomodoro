package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.domain.model.LabBackground
import kotlin.random.Random

/**
 * Fondo de laboratorio (Pro) detrás del recipiente: azulejos, papel milimétrico o fórmulas de
 * gis. Se dibuja con el color del texto a muy poca opacidad, así sirve en tema claro y oscuro.
 */
@Composable
fun LabBackdrop(background: LabBackground, modifier: Modifier = Modifier) {
    if (background == LabBackground.NONE) return
    val ink = MaterialTheme.colorScheme.onBackground
    val measurer = rememberTextMeasurer()
    // Fórmulas en los márgenes (izquierda y derecha, alternadas), repartidas a lo alto y con
    // ángulos fijos: no se enciman con el recipiente ni con el texto del centro
    val style = TextStyle(color = ink.copy(alpha = 0.08f), fontSize = 18.sp)
    val formulas = remember(style) {
        val random = Random(7)
        FORMULAS.mapIndexed { i, text ->
            Placed(measurer.measure(text, style, softWrap = false, maxLines = 1), i % 2 == 0, random.nextFloat(), random.nextFloat() * 24f - 12f)
        }
    }
    Canvas(modifier) {
        when (background) {
            LabBackground.NONE -> Unit
            // Azulejos de laboratorio: cuadros grandes con su junta
            LabBackground.TILES -> {
                val step = 44.dp.toPx()
                val joint = 1.2.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawLine(ink.copy(alpha = 0.07f), Offset(x, 0f), Offset(x, size.height), strokeWidth = joint)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(ink.copy(alpha = 0.07f), Offset(0f, y), Offset(size.width, y), strokeWidth = joint)
                    y += step
                }
            }
            // Papel milimétrico: cuadrícula fina y, cada 5, una más marcada
            LabBackground.GRAPH -> {
                val step = 8.dp.toPx()
                var i = 0
                var x = 0f
                while (x <= size.width) {
                    val bold = i % 5 == 0
                    drawLine(ink.copy(alpha = if (bold) 0.08f else 0.035f), Offset(x, 0f), Offset(x, size.height), strokeWidth = if (bold) 1.dp.toPx() else 0.5.dp.toPx())
                    x += step
                    i++
                }
                i = 0
                var y = 0f
                while (y <= size.height) {
                    val bold = i % 5 == 0
                    drawLine(ink.copy(alpha = if (bold) 0.08f else 0.035f), Offset(0f, y), Offset(size.width, y), strokeWidth = if (bold) 1.dp.toPx() else 0.5.dp.toPx())
                    y += step
                    i++
                }
            }
            // Pizarrón: fórmulas de gis repartidas, apenas visibles
            LabBackground.FORMULAS -> formulas.forEachIndexed { i, f ->
                val margin = size.width * 0.04f
                val x = if (f.left) margin + f.jitter * margin else size.width - f.layout.size.width - margin - f.jitter * margin
                // Desde el 12 % de alto: arriba a la izquierda está el anillo de la meta
                val y = size.height * (0.12f + 0.78f * (i + 0.5f) / formulas.size)
                rotate(f.angle, Offset(x, y)) { drawText(f.layout, topLeft = Offset(x, y)) }
            }
        }
    }
}

private class Placed(val layout: TextLayoutResult, val left: Boolean, val jitter: Float, val angle: Float)

private val FORMULAS = listOf(
    // Cortas: las largas se meterían debajo del recipiente
    "H₂O", "E = mc²", "PV = nRT", "NaCl", "C₆H₁₂O₆", "O₂", "Fe",
    "CO₂", "F = ma", "λ = h/p", "NH₃", "Au", "NaOH", "CH₄", "6.022 × 10²³",
)
