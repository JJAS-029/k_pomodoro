package com.jjas.labpomodoro.ui.medals

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.jjas.labpomodoro.domain.model.Medal
import kotlin.math.cos
import kotlin.math.sin

/** El objeto que acompaña al koala en cada medalla: dice de qué es. */
enum class MedalProp { TEST_TUBE, FLAME, LEAF, CALENDAR, CROWN, HOURGLASS, MAGNIFIER, CAP, BOLT, SUN, MOON, FLASK, ATOM, TILES, HALF_TABLE, FULL_TABLE, STAR }

val Medal.prop: MedalProp
    get() = when (this) {
        Medal.FIRST_POMODORO -> MedalProp.TEST_TUBE
        Medal.STREAK_3 -> MedalProp.FLAME
        Medal.STREAK_7 -> MedalProp.LEAF
        Medal.STREAK_30 -> MedalProp.CALENDAR
        Medal.STREAK_100 -> MedalProp.CROWN
        Medal.HOURS_10 -> MedalProp.HOURGLASS
        Medal.HOURS_50 -> MedalProp.MAGNIFIER
        Medal.HOURS_100 -> MedalProp.CAP
        Medal.MARATHON -> MedalProp.BOLT
        Medal.EARLY_BIRD -> MedalProp.SUN
        Medal.NIGHT_OWL -> MedalProp.MOON
        Medal.FIRST_FUSION -> MedalProp.FLASK
        Medal.FUSION_MASTER -> MedalProp.ATOM
        Medal.COLLECTOR -> MedalProp.TILES
        Medal.HALF_TABLE -> MedalProp.HALF_TABLE
        Medal.FULL_TABLE -> MedalProp.FULL_TABLE
        Medal.FIRST_GOLD -> MedalProp.STAR
    }

/**
 * Ilustración en línea neón: la cabeza del koala a trazo (orejas, cara, nariz y ojos) y su objeto
 * abajo a la derecha, sobre un recorte del fondo para que los trazos no se encimen.
 *
 * @param r radio del cuerpo de la medalla.
 * @param knockout color del vidrio detrás del objeto.
 */
fun DrawScope.drawKoalaArt(prop: MedalProp, r: Float, color: Color, knockout: Color) {
    val s = r * 0.6f
    val c = Offset(center.x - s * 0.08f, center.y + s * 0.05f)
    val line = Stroke(width = s * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    // Resplandor suave detrás del trazo
    drawCircle(color.copy(alpha = 0.18f), radius = s * 0.62f, center = c, style = Stroke(s * 0.24f))
    listOf(-1f, 1f).forEach { side ->
        drawCircle(color, radius = s * 0.3f, center = Offset(c.x + side * s * 0.55f, c.y - s * 0.42f), style = line)
    }
    drawCircle(color, radius = s * 0.6f, center = c, style = line)
    drawOval(color, topLeft = Offset(c.x - s * 0.13f, c.y - s * 0.02f), size = Size(s * 0.26f, s * 0.34f))
    drawCircle(color, radius = s * 0.065f, center = Offset(c.x - s * 0.26f, c.y - s * 0.1f))
    drawCircle(color, radius = s * 0.065f, center = Offset(c.x + s * 0.26f, c.y - s * 0.1f))

    val at = Offset(c.x + s * 0.68f, c.y + s * 0.58f)
    val pr = s * 0.36f
    drawCircle(knockout, radius = pr * 1.3f, center = at)
    drawProp(prop, at, pr, color)
}

private fun DrawScope.drawProp(prop: MedalProp, at: Offset, r: Float, color: Color) {
    val line = Stroke(width = r * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), color, style = line)
    when (prop) {
        MedalProp.TEST_TUBE -> rotate(25f, at) {
            path {
                moveTo(at.x - r * 0.35f, at.y - r)
                lineTo(at.x - r * 0.35f, at.y + r * 0.6f)
                quadraticTo(at.x - r * 0.35f, at.y + r, at.x, at.y + r)
                quadraticTo(at.x + r * 0.35f, at.y + r, at.x + r * 0.35f, at.y + r * 0.6f)
                lineTo(at.x + r * 0.35f, at.y - r)
            }
            drawLine(color, Offset(at.x - r * 0.35f, at.y + r * 0.1f), Offset(at.x + r * 0.35f, at.y + r * 0.1f), strokeWidth = r * 0.16f)
        }
        MedalProp.FLAME -> path {
            moveTo(at.x, at.y - r)
            cubicTo(at.x + r * 0.9f, at.y - r * 0.1f, at.x + r * 0.8f, at.y + r, at.x, at.y + r)
            cubicTo(at.x - r * 0.8f, at.y + r, at.x - r * 0.9f, at.y + r * 0.1f, at.x - r * 0.2f, at.y - r * 0.4f)
            cubicTo(at.x - r * 0.1f, at.y - r * 0.1f, at.x + r * 0.1f, at.y - r * 0.4f, at.x, at.y - r)
        }
        MedalProp.LEAF -> {
            path {
                moveTo(at.x - r * 0.8f, at.y + r * 0.8f)
                quadraticTo(at.x - r * 0.9f, at.y - r * 0.9f, at.x + r * 0.85f, at.y - r * 0.85f)
                quadraticTo(at.x + r * 0.9f, at.y + r * 0.9f, at.x - r * 0.8f, at.y + r * 0.8f)
            }
            drawLine(color, Offset(at.x - r * 0.8f, at.y + r * 0.8f), Offset(at.x + r * 0.3f, at.y - r * 0.3f), strokeWidth = r * 0.14f, cap = StrokeCap.Round)
        }
        MedalProp.CALENDAR -> {
            drawRoundRect(color, Offset(at.x - r * 0.85f, at.y - r * 0.6f), Size(r * 1.7f, r * 1.5f), CornerRadius(r * 0.2f), style = line)
            drawLine(color, Offset(at.x - r * 0.85f, at.y - r * 0.15f), Offset(at.x + r * 0.85f, at.y - r * 0.15f), strokeWidth = r * 0.16f)
            listOf(-0.4f, 0.4f).forEach { dx ->
                drawLine(color, Offset(at.x + r * dx, at.y - r * 0.95f), Offset(at.x + r * dx, at.y - r * 0.5f), strokeWidth = r * 0.18f, cap = StrokeCap.Round)
            }
            drawCircle(color, radius = r * 0.14f, center = Offset(at.x + r * 0.35f, at.y + r * 0.4f))
        }
        MedalProp.CROWN -> path {
            moveTo(at.x - r * 0.9f, at.y + r * 0.6f)
            lineTo(at.x - r * 0.9f, at.y - r * 0.6f)
            lineTo(at.x - r * 0.4f, at.y)
            lineTo(at.x, at.y - r * 0.8f)
            lineTo(at.x + r * 0.4f, at.y)
            lineTo(at.x + r * 0.9f, at.y - r * 0.6f)
            lineTo(at.x + r * 0.9f, at.y + r * 0.6f)
            close()
        }
        MedalProp.HOURGLASS -> {
            path {
                moveTo(at.x - r * 0.65f, at.y - r * 0.9f)
                lineTo(at.x + r * 0.65f, at.y - r * 0.9f)
                lineTo(at.x - r * 0.65f, at.y + r * 0.9f)
                lineTo(at.x + r * 0.65f, at.y + r * 0.9f)
                close()
            }
        }
        MedalProp.MAGNIFIER -> {
            drawCircle(color, radius = r * 0.55f, center = Offset(at.x - r * 0.2f, at.y - r * 0.2f), style = line)
            drawLine(color, Offset(at.x + r * 0.2f, at.y + r * 0.2f), Offset(at.x + r * 0.85f, at.y + r * 0.85f), strokeWidth = r * 0.26f, cap = StrokeCap.Round)
        }
        MedalProp.CAP -> {
            path {
                moveTo(at.x - r, at.y - r * 0.2f)
                lineTo(at.x, at.y - r * 0.7f)
                lineTo(at.x + r, at.y - r * 0.2f)
                lineTo(at.x, at.y + r * 0.3f)
                close()
            }
            path {
                moveTo(at.x - r * 0.55f, at.y + r * 0.05f)
                lineTo(at.x - r * 0.55f, at.y + r * 0.65f)
                quadraticTo(at.x, at.y + r, at.x + r * 0.55f, at.y + r * 0.65f)
                lineTo(at.x + r * 0.55f, at.y + r * 0.05f)
            }
        }
        MedalProp.BOLT -> path {
            moveTo(at.x + r * 0.2f, at.y - r)
            lineTo(at.x - r * 0.6f, at.y + r * 0.15f)
            lineTo(at.x, at.y + r * 0.15f)
            lineTo(at.x - r * 0.2f, at.y + r)
            lineTo(at.x + r * 0.6f, at.y - r * 0.15f)
            lineTo(at.x, at.y - r * 0.15f)
            close()
        }
        MedalProp.SUN -> {
            drawCircle(color, radius = r * 0.45f, center = at, style = line)
            repeat(8) { i ->
                val a = Math.toRadians(i * 45.0)
                val d = Offset(cos(a).toFloat(), sin(a).toFloat())
                drawLine(color, at + d * (r * 0.72f), at + d * (r * 1.0f), strokeWidth = r * 0.16f, cap = StrokeCap.Round)
            }
        }
        MedalProp.MOON -> {
            val moon = Path().apply {
                addOval(Rect(at, r * 0.85f))
                val cut = Path().apply { addOval(Rect(Offset(at.x + r * 0.45f, at.y - r * 0.3f), r * 0.8f)) }
                op(this, cut, PathOperation.Difference)
            }
            drawPath(moon, color, style = line)
        }
        MedalProp.FLASK -> {
            path {
                moveTo(at.x - r * 0.25f, at.y - r)
                lineTo(at.x - r * 0.25f, at.y - r * 0.2f)
                lineTo(at.x - r * 0.85f, at.y + r * 0.9f)
                lineTo(at.x + r * 0.85f, at.y + r * 0.9f)
                lineTo(at.x + r * 0.25f, at.y - r * 0.2f)
                lineTo(at.x + r * 0.25f, at.y - r)
            }
            drawLine(color, Offset(at.x - r * 0.5f, at.y + r * 0.35f), Offset(at.x + r * 0.5f, at.y + r * 0.35f), strokeWidth = r * 0.14f)
        }
        MedalProp.ATOM -> {
            listOf(0f, 60f, 120f).forEach { angle ->
                rotate(angle, at) {
                    drawOval(color, topLeft = Offset(at.x - r, at.y - r * 0.38f), size = Size(r * 2f, r * 0.76f), style = Stroke(r * 0.14f))
                }
            }
            drawCircle(color, radius = r * 0.18f, center = at)
        }
        MedalProp.TILES -> {
            val t = r * 0.75f
            listOf(Offset(-1f, -1f), Offset(0.15f, -1f), Offset(-1f, 0.15f), Offset(0.15f, 0.15f)).forEach { o ->
                drawRoundRect(color, Offset(at.x + o.x * t, at.y + o.y * t), Size(t * 0.85f, t * 0.85f), CornerRadius(t * 0.15f), style = Stroke(r * 0.15f))
            }
        }
        MedalProp.HALF_TABLE, MedalProp.FULL_TABLE -> {
            val t = r * 0.62f
            for (row in 0..2) for (col in 0..2) {
                val tl = Offset(at.x + (col - 1.5f) * t + t * 0.08f, at.y + (row - 1.5f) * t + t * 0.08f)
                val filled = prop == MedalProp.FULL_TABLE || row * 3 + col < 4
                if (filled) {
                    drawRoundRect(color, tl, Size(t * 0.84f, t * 0.84f), CornerRadius(t * 0.15f))
                } else {
                    drawRoundRect(color.copy(alpha = 0.4f), tl, Size(t * 0.84f, t * 0.84f), CornerRadius(t * 0.15f), style = Stroke(r * 0.08f))
                }
            }
        }
        MedalProp.STAR -> path {
            for (i in 0 until 10) {
                val a = Math.toRadians(-90.0 + i * 36.0)
                val rad = if (i % 2 == 0) r else r * 0.45f
                val p = Offset(at.x + rad * cos(a).toFloat(), at.y + rad * sin(a).toFloat())
                if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
            }
            close()
        }
    }
}
