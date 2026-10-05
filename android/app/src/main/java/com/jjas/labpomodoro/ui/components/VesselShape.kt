package com.jjas.labpomodoro.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Cristalería de laboratorio. [aspect] = ancho / alto del recipiente. */
enum class VesselShape(val aspect: Float) {
    BEAKER(0.5f),
    ERLENMEYER(0.72f),
    TEST_TUBE(0.3f),
    ROUND_FLASK(0.69f),
}

/**
 * Un recipiente colocado en el Canvas. Coordenadas normalizadas: u = 0..1 a lo ancho,
 * v = 0 (fondo) .. 1 (borde superior). Sirve igual para el recipiente grande, la repisa y el PiP.
 */
class VesselGeometry private constructor(
    val shape: VesselShape,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val stroke: Float,
) {
    val right = left + width
    val bottom = top + height

    /** Medida de referencia para tamaños de partículas (no depende de lo delgado del recipiente). */
    val unit = min(width, height * 0.5f)

    fun px(u: Float) = left + u * width
    fun py(v: Float) = bottom - v * height

    // Matraz de fondo redondo: esfera de diámetro = ancho y cuello centrado
    private val sphereR = width / 2f
    private val sphereCy = bottom - sphereR
    private val neckHalf = width * 0.13f
    private val neckJunctionY = sphereCy - sqrt(sphereR * sphereR - neckHalf * neckHalf)

    /** Nivel máximo del líquido (en v) cuando fill = 1: en los matraces llega al inicio del cuello. */
    val maxFill: Float = when (shape) {
        VesselShape.BEAKER -> 0.95f
        VesselShape.TEST_TUBE -> 0.9f
        VesselShape.ERLENMEYER -> 0.7f
        VesselShape.ROUND_FLASK -> (bottom - neckJunctionY) / height * 0.97f
    }

    fun surfaceY(fill: Float) = py(fill.coerceIn(0f, 1f) * maxFill)

    /** Medio ancho interior (en u) a la altura v; las partículas se mantienen dentro. */
    fun halfWidth(v: Float): Float {
        if (v > 1f) return halfWidth(0.99f) + (v - 1f) * 0.6f // El vapor se abre al salir
        return when (shape) {
            VesselShape.BEAKER -> 0.46f
            VesselShape.TEST_TUBE -> 0.3f
            VesselShape.ERLENMEYER -> when {
                v >= 0.6f -> 0.12f
                v >= 0.13f -> 0.44f + (0.12f - 0.44f) * (v - 0.13f) / 0.47f
                else -> 0.4f
            }
            VesselShape.ROUND_FLASK -> {
                val y = py(v)
                if (y < neckJunctionY) 0.12f
                else sqrt(max(0f, sphereR * sphereR - (y - sphereCy) * (y - sphereCy))) / width * 0.9f
            }
        }
    }

    /** Silueta cerrada del interior, para recortar el líquido y las burbujas. */
    val interior: Path = Path().apply { body(closed = true) }

    /** Contorno del vidrio (abierto arriba) más el borde/labio. */
    val outline: Path = Path().apply {
        body(closed = false)
        when (shape) {
            VesselShape.BEAKER -> Unit // El vaso lleva su pico dentro de body()
            VesselShape.TEST_TUBE -> lipRing(0f, 1f)
            VesselShape.ERLENMEYER -> lipRing(0.3f, 0.7f)
            VesselShape.ROUND_FLASK -> lipRing(0.33f, 0.67f)
        }
    }

    private fun Path.lipRing(u0: Float, u1: Float) {
        val h = height * 0.06f
        addRoundRect(RoundRect(px(u0), top, px(u1), top + h, CornerRadius(h / 2)))
    }

    private fun Path.body(closed: Boolean) {
        when (shape) {
            VesselShape.BEAKER -> {
                val r = width * 0.4f
                val lip = stroke * 2.5f
                if (closed) {
                    addRoundRect(
                        RoundRect(
                            left, top, right, bottom,
                            topLeftCornerRadius = CornerRadius.Zero, topRightCornerRadius = CornerRadius.Zero,
                            bottomLeftCornerRadius = CornerRadius(r), bottomRightCornerRadius = CornerRadius(r),
                        )
                    )
                    return
                }
                moveTo(left - lip, top)
                lineTo(left, top + lip)
                lineTo(left, bottom - r)
                arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 180f, -90f, false)
                lineTo(right - r, bottom)
                arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
                lineTo(right, top + lip)
                lineTo(right + lip, top)
            }

            VesselShape.TEST_TUBE -> {
                val l = px(0.15f)
                val r = px(0.85f)
                val radius = (r - l) / 2
                val neckTop = py(0.94f)
                moveTo(l, neckTop)
                lineTo(l, bottom - radius)
                arcTo(Rect(l, bottom - 2 * radius, r, bottom), 180f, -180f, false)
                lineTo(r, neckTop)
                if (closed) close()
            }

            VesselShape.ERLENMEYER -> {
                val neckTop = py(0.94f)
                moveTo(px(0.37f), neckTop)
                lineTo(px(0.37f), py(0.6f))
                lineTo(px(0.04f), py(0.13f))
                quadraticTo(px(0f), py(0f), px(0.14f), py(0f))
                lineTo(px(0.86f), py(0f))
                quadraticTo(px(1f), py(0f), px(0.96f), py(0.13f))
                lineTo(px(0.63f), py(0.6f))
                lineTo(px(0.63f), neckTop)
                if (closed) close()
            }

            VesselShape.ROUND_FLASK -> {
                val cx = px(0.5f)
                val neckTop = py(0.94f)
                val alpha = Math.toDegrees(asin((neckHalf / sphereR).toDouble())).toFloat()
                moveTo(cx - neckHalf, neckTop)
                lineTo(cx - neckHalf, neckJunctionY)
                arcTo(
                    Rect(cx - sphereR, sphereCy - sphereR, cx + sphereR, sphereCy + sphereR),
                    270f - alpha,
                    -(360f - 2 * alpha),
                    false,
                )
                lineTo(cx + neckHalf, neckTop)
                if (closed) close()
            }
        }
    }

    companion object {
        /**
         * Acomoda el recipiente centrado en un Canvas de [canvasWidth] × [canvasHeight], dejando
         * [headroom] (fracción del alto) libre arriba para el vapor.
         */
        fun fit(
            shape: VesselShape,
            canvasWidth: Float,
            canvasHeight: Float,
            headroom: Float = 0f,
            strokeWidth: Float? = null,
        ): VesselGeometry {
            val stroke = strokeWidth ?: (canvasHeight * 0.016f)
            val maxHeight = canvasHeight * (1f - headroom) - stroke
            val w = min(canvasWidth * 0.9f - stroke * 4, maxHeight * shape.aspect)
            val h = w / shape.aspect
            val bottom = canvasHeight - stroke / 2
            return VesselGeometry(shape, (canvasWidth - w) / 2f, bottom - h, w, h, stroke)
        }
    }
}
