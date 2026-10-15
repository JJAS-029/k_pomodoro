package com.jjas.labpomodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

/** Propuestas de ilustración para las medallas: emoji, línea neón y silueta plana. */
class MedalArtActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LabPomodoroTheme(darkTheme = true) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text("Ilustraciones de medallas", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                        listOf("Emoji (actual)", "A · Línea neón", "B · Silueta plana").forEachIndexed { style, title ->
                            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Prop.entries.forEach { prop ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Medal(style, prop)
                                        Text(prop.label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class Prop(val label: String, val emoji: String, val metal: Color) {
    MOON("Koala nocturno", "🦉", Color(0xFFE2E8F0)),
    SUN("Koala madrugador", "🌅", Color(0xFFE2E8F0)),
    FLASK("Koala alquimista", "⚗️", Color(0xFFFF9E6D)),
}

@Composable
private fun Medal(style: Int, prop: Prop) {
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f * 0.86f
            drawCircle(Brush.radialGradient(0.72f to prop.metal.copy(alpha = 0.3f), 1f to Color.Transparent, radius = size.minDimension / 2f))
            drawCircle(
                Brush.radialGradient(0f to prop.metal.copy(alpha = 0.35f), 0.6f to Color(0xFF15181E), 1f to Color(0xFF07080B), center = Offset(center.x, center.y + r * 0.35f), radius = r * 1.3f),
                radius = r,
            )
            drawCircle(prop.metal.copy(alpha = 0.35f), radius = r, style = Stroke(r * 0.16f))
            drawCircle(prop.metal, radius = r, style = Stroke(r * 0.06f))
            when (style) {
                1 -> lineKoala(r * 0.62f, prop)
                2 -> flatKoala(r * 0.62f, prop)
            }
            drawCircle(
                Brush.radialGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), center = Offset(center.x - r * 0.25f, center.y - r * 0.32f), radius = r * 0.75f),
                radius = r * 0.97f,
            )
        }
        if (style == 0) Text(prop.emoji, fontSize = 30.sp)
    }
}

/** Koala a trazo: orejas, cabeza, nariz y ojos, más el objeto de la medalla. */
private fun DrawScope.lineKoala(s: Float, prop: Prop) {
    val c = Offset(center.x, center.y + s * 0.12f)
    val line = Stroke(width = s * 0.09f, cap = StrokeCap.Round)
    val color = prop.metal
    drawCircle(color.copy(alpha = 0.25f), radius = s * 0.62f, center = c, style = Stroke(s * 0.22f))
    drawCircle(color, radius = s * 0.3f, center = Offset(c.x - s * 0.55f, c.y - s * 0.42f), style = line)
    drawCircle(color, radius = s * 0.3f, center = Offset(c.x + s * 0.55f, c.y - s * 0.42f), style = line)
    drawCircle(color, radius = s * 0.6f, center = c, style = line)
    drawOval(color, topLeft = Offset(c.x - s * 0.13f, c.y - s * 0.02f), size = Size(s * 0.26f, s * 0.34f))
    drawCircle(color, radius = s * 0.06f, center = Offset(c.x - s * 0.26f, c.y - s * 0.1f))
    drawCircle(color, radius = s * 0.06f, center = Offset(c.x + s * 0.26f, c.y - s * 0.1f))
    prop(prop, Offset(c.x + s * 0.62f, c.y + s * 0.55f), s * 0.32f, color, filled = false)
}

/** Koala relleno en dos tonos (gris y el metal), con el objeto de la medalla. */
private fun DrawScope.flatKoala(s: Float, prop: Prop) {
    val c = Offset(center.x, center.y + s * 0.12f)
    val fur = Color(0xFF9AA4B2)
    val inner = Color(0xFFE6EAF0)
    listOf(-1f, 1f).forEach { side ->
        drawCircle(fur, radius = s * 0.32f, center = Offset(c.x + side * s * 0.55f, c.y - s * 0.42f))
        drawCircle(inner, radius = s * 0.17f, center = Offset(c.x + side * s * 0.55f, c.y - s * 0.42f))
    }
    drawCircle(fur, radius = s * 0.6f, center = c)
    drawOval(Color(0xFF2B2F36), topLeft = Offset(c.x - s * 0.15f, c.y - s * 0.04f), size = Size(s * 0.3f, s * 0.38f))
    drawCircle(Color(0xFF2B2F36), radius = s * 0.07f, center = Offset(c.x - s * 0.27f, c.y - s * 0.12f))
    drawCircle(Color(0xFF2B2F36), radius = s * 0.07f, center = Offset(c.x + s * 0.27f, c.y - s * 0.12f))
    prop(prop, Offset(c.x + s * 0.62f, c.y + s * 0.55f), s * 0.34f, prop.metal, filled = true)
}

private fun DrawScope.prop(prop: Prop, at: Offset, r: Float, color: Color, filled: Boolean) {
    val style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = r * 0.22f, cap = StrokeCap.Round)
    when (prop) {
        // Luna creciente
        Prop.MOON -> {
            val moon = Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(at, r))
                val cut = Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(at.x + r * 0.45f, at.y - r * 0.3f), r)) }
                op(this, cut, androidx.compose.ui.graphics.PathOperation.Difference)
            }
            drawPath(moon, color, style = style)
        }
        // Sol con rayos
        Prop.SUN -> {
            drawCircle(color, radius = r * 0.5f, center = at, style = style)
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val d = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                drawLine(color, at + d * (r * 0.75f), at + d * (r * 1.05f), strokeWidth = r * 0.18f, cap = StrokeCap.Round)
            }
        }
        // Matraz
        Prop.FLASK -> {
            val flask = Path().apply {
                moveTo(at.x - r * 0.25f, at.y - r)
                lineTo(at.x - r * 0.25f, at.y - r * 0.2f)
                lineTo(at.x - r * 0.9f, at.y + r)
                lineTo(at.x + r * 0.9f, at.y + r)
                lineTo(at.x + r * 0.25f, at.y - r * 0.2f)
                lineTo(at.x + r * 0.25f, at.y - r)
                close()
            }
            drawPath(flask, color, style = style)
        }
    }
}
