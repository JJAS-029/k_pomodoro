package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
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
    // Fórmulas en lugares y ángulos fijos (siempre los mismos)
    val formulas = remember {
        val random = Random(7)
        FORMULAS.map { Triple(it, Offset(random.nextFloat(), random.nextFloat()), random.nextFloat() * 30f - 15f) }
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
            LabBackground.FORMULAS -> formulas.forEach { (text, at, angle) ->
                val position = Offset(at.x * size.width * 0.8f, at.y * size.height * 0.92f)
                rotate(angle, position) {
                    drawText(measurer, text, position, TextStyle(color = ink.copy(alpha = 0.09f), fontSize = 20.sp))
                }
            }
        }
    }
}

private val FORMULAS = listOf(
    "H₂O", "E = mc²", "PV = nRT", "NaCl", "C₆H₁₂O₆", "2H₂ + O₂ → 2H₂O", "pH = −log[H⁺]",
    "CO₂", "F = ma", "λ = h/p", "NH₃", "Au", "ΔG = ΔH − TΔS", "CH₄", "6.022 × 10²³",
)
