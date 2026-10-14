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

    /** Probeta graduada: alta y angosta, con pie ancho y marcas finas. */
    GRADUATED_CYLINDER(0.3f),

    /** Matraz aforado: bola con cuello largo y la marca de aforo, hasta donde llega el líquido. */
    VOLUMETRIC_FLASK(0.55f),
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

    // Matraces de bola (fondo redondo y aforado): esfera de diámetro = ancho y cuello centrado
    private val sphereR = width / 2f
    private val sphereCy = bottom - sphereR
    private val neckHalf = width * if (shape == VesselShape.VOLUMETRIC_FLASK) 0.1f else 0.13f
    private val neckJunctionY = sphereCy - sqrt(sphereR * sphereR - neckHalf * neckHalf)

    /** Nivel máximo del líquido (en v) cuando fill = 1: en los matraces llega al inicio del cuello. */
    val maxFill: Float = when (shape) {
        VesselShape.BEAKER -> 0.95f
        VesselShape.TEST_TUBE -> 0.9f
        VesselShape.ERLENMEYER -> 0.7f
        VesselShape.ROUND_FLASK -> (bottom - neckJunctionY) / height * 0.97f
        VesselShape.GRADUATED_CYLINDER -> 0.88f
        // Lleno hasta la marca de aforo, en el cuello
        VesselShape.VOLUMETRIC_FLASK -> VOLUMETRIC_MARK
    }

    /** Arriba del pie de la probeta (v); el líquido y las partículas empiezan ahí. */
    val cylinderFloor = 0.045f

    /** Donde empieza el líquido (v): el fondo, o arriba del pie en la probeta. */
    private val floorV = if (shape == VesselShape.GRADUATED_CYLINDER) cylinderFloor else 0f

    /**
     * Altura (v) para un nivel de llenado 0..1. En la probeta el cero está arriba del pie, para
     * que el líquido se vea desde el primer instante y se acabe justo al terminar la sesión.
     */
    fun levelV(fill: Float): Float = floorV + fill * (maxFill - floorV)

    fun surfaceY(fill: Float) = py(levelV(fill.coerceIn(0f, 1f)))

    /** Medio ancho interior (en u) a la altura v; las partículas se mantienen dentro. */
    fun halfWidth(v: Float): Float {
        if (v > 1f) return halfWidth(0.99f) + (v - 1f) * 0.6f // El vapor se abre al salir
        return when (shape) {
            VesselShape.BEAKER -> 0.46f
            VesselShape.TEST_TUBE -> 0.3f
            // Cono que se curva al llegar al cuello (hombros suaves)
            VesselShape.ERLENMEYER -> when {
                v >= 0.66f -> 0.12f
                v >= 0.13f -> {
                    val k = (v - 0.13f) / 0.53f
                    0.44f + (0.12f - 0.44f) * (k * (2f - k))
                }
                else -> 0.4f
            }
            VesselShape.ROUND_FLASK, VesselShape.VOLUMETRIC_FLASK -> {
                val y = py(v)
                if (y < neckJunctionY) neckHalf / width * 0.9f
                else sqrt(max(0f, sphereR * sphereR - (y - sphereCy) * (y - sphereCy))) / width * 0.9f
            }
            VesselShape.GRADUATED_CYLINDER -> 0.24f
        }
    }

    /** Donde empieza el interior por arriba (y en px): la boca, debajo del labio. */
    val mouthY: Float = when (shape) {
        VesselShape.BEAKER -> top
        VesselShape.GRADUATED_CYLINDER -> py(0.96f)
        else -> py(0.94f)
    }

    /** Área del vidrio para el esmerilado: en el vaso incluye el pico, que el interior no lleva. */
    val glassArea: Path = Path().apply { body(closed = false); close() }

    /** Silueta cerrada del interior, para recortar el líquido y las burbujas. */
    val interior: Path = Path().apply { body(closed = true) }

    /** Labio de la boca: de dónde a dónde va (u) y su alto (px); null en el vaso, que lleva pico. */
    val lip: Triple<Float, Float, Float>? = when (shape) {
        VesselShape.BEAKER -> null
        VesselShape.TEST_TUBE -> Triple(0f, 1f, height * 0.06f)
        VesselShape.ERLENMEYER -> Triple(0.3f, 0.7f, height * 0.06f)
        VesselShape.ROUND_FLASK -> Triple(0.33f, 0.67f, height * 0.06f)
        VesselShape.VOLUMETRIC_FLASK -> Triple(0.36f, 0.64f, height * 0.06f)
        // La probeta es muy alta: el labio va por ancho para no quedar grueso
        // Alto igual al tramo sobre las paredes (py(0.96)), para que el labio y las paredes se toquen
        VesselShape.GRADUATED_CYLINDER -> Triple(0.12f, 0.88f, height * 0.04f)
    }

    /** El vidrio sin el labio (con el pie de la probeta): cada estilo dibuja la boca a su manera. */
    val bodyOutline: Path = Path().apply {
        body(closed = false)
        if (shape == VesselShape.GRADUATED_CYLINDER) {
            val foot = height * cylinderFloor
            addRoundRect(RoundRect(left, bottom - foot, right, bottom, CornerRadius(foot / 2)))
        }
    }

    /**
     * Dónde empiezan las paredes bajo el labio: el borde exterior de su trazo, para que las puntas
     * de las paredes no se encimen en el aro. En el vaso, que no lleva labio, la boca.
     */
    val wallTop: Float get() = lip?.let { (_, _, h) -> maxOf(mouthY, top + h) + stroke * 0.35f } ?: (top - stroke)

    /** Contorno del vidrio (abierto arriba) más el borde/labio. */
    val outline: Path = Path().apply {
        addPath(bodyOutline)
        lip?.let { (u0, u1, h) -> addRoundRect(RoundRect(px(u0), top, px(u1), top + h, CornerRadius(h / 2))) }
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
                // Pico vertedor curvo
                moveTo(left - lip, top)
                quadraticTo(left - lip * 0.05f, top + lip * 0.2f, left, top + lip * 1.4f)
                lineTo(left, bottom - r)
                arcTo(Rect(left, bottom - 2 * r, left + 2 * r, bottom), 180f, -90f, false)
                lineTo(right - r, bottom)
                arcTo(Rect(right - 2 * r, bottom - 2 * r, right, bottom), 90f, -90f, false)
                lineTo(right, top + lip * 1.4f)
                quadraticTo(right + lip * 0.05f, top + lip * 0.2f, right + lip, top)
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
                // Hombros curvos del cuello al cono y base redondeada
                moveTo(px(0.37f), neckTop)
                lineTo(px(0.37f), py(0.66f))
                cubicTo(px(0.37f), py(0.56f), px(0.12f), py(0.3f), px(0.04f), py(0.13f))
                quadraticTo(px(0f), py(0f), px(0.14f), py(0f))
                lineTo(px(0.86f), py(0f))
                quadraticTo(px(1f), py(0f), px(0.96f), py(0.13f))
                cubicTo(px(0.88f), py(0.3f), px(0.63f), py(0.56f), px(0.63f), py(0.66f))
                lineTo(px(0.63f), neckTop)
                if (closed) close()
            }

            VesselShape.GRADUATED_CYLINDER -> {
                val neckTop = py(0.96f)
                moveTo(px(0.22f), neckTop)
                lineTo(px(0.22f), py(cylinderFloor))
                lineTo(px(0.78f), py(cylinderFloor))
                lineTo(px(0.78f), neckTop)
                if (closed) close()
            }

            VesselShape.ROUND_FLASK, VesselShape.VOLUMETRIC_FLASK -> {
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
        /** Altura (v) de la marca de aforo del matraz aforado. */
        const val VOLUMETRIC_MARK = 0.8f

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
