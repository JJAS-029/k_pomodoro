package com.jjas.labpomodoro.ui.components

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sin
import kotlin.random.Random

/** Una gota: sube y baja lento y se mece de lado, cada una a su ritmo. */
private class Blob(
    val lane: Float,
    val sway: Float,
    val swaySpeed: Float,
    val riseSpeed: Float,
    val phase: Float,
    val radius: Float,
)

/**
 * Gotas que se funden entre sí al tocarse y se vuelven a separar, como en una lámpara de lava o
 * el mercurio (técnica de "metaballs"): se dibujan círculos, se desenfocan y luego se corta la
 * transparencia con un umbral, así dos manchas cercanas se unen en una sola forma con borde nítido.
 * En Android 12+ se usa RenderEffect; antes se ven como gotas sueltas.
 *
 * @param time segundos, para la animación (lo da el recipiente).
 */
@Composable
fun LiquidBlobs(
    shape: VesselShape,
    fill: Float,
    color: Color,
    hot: Boolean,
    time: Float,
    modifier: Modifier = Modifier,
) {
    val blobs = remember(shape) {
        val random = Random(shape.ordinal + 11)
        List(BLOB_COUNT) {
            Blob(
                lane = random.nextFloat() * 1.2f - 0.6f,
                sway = 0.15f + random.nextFloat() * 0.25f,
                swaySpeed = 0.2f + random.nextFloat() * 0.3f,
                riseSpeed = 0.08f + random.nextFloat() * 0.12f,
                phase = random.nextFloat() * 6.28f,
                radius = 0.05f + random.nextFloat() * 0.06f,
            )
        }
    }
    val blurPx = with(LocalDensity.current) { BLUR_DP * density }
    val gooey = remember(blurPx) { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) gooeyEffect(blurPx) else null }
    // Centro claro y borde del color del líquido: en los luminosos el centro casi blanco parece encendido
    val core = lerp(color, Color.White, if (hot) 0.65f else 0.4f)
    val edge = lerp(color, Color.White, if (hot) 0.25f else 0.1f)

    // El recorte va por fuera del efecto, para que el desenfoque no se salga del vidrio
    Box(
        modifier.drawWithContent {
            val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
            clipPath(g.interior) {
                clipRect(top = g.surfaceY(fill)) { this@drawWithContent.drawContent() }
            }
        },
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { renderEffect = gooey?.asComposeRenderEffect() },
        ) {
            val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
            if (fill <= 0.05f) return@Canvas
            blobs.forEach { b ->
                // Sube y baja dentro del líquido, sin pasar de la superficie
                val v = fill * g.maxFill * (0.12f + 0.7f * (0.5f + 0.5f * sin(time * b.riseSpeed * 6.28f + b.phase)))
                val lane = (b.lane + b.sway * sin(time * b.swaySpeed * 6.28f + b.phase * 1.3f)).coerceIn(-0.85f, 0.85f)
                val center = Offset(g.px(0.5f + lane * g.halfWidth(v)), g.py(v))
                val r = g.width * b.radius
                drawCircle(Brush.radialGradient(listOf(core, edge), center, r), r, center)
            }
        }
    }
}

/** Desenfoque y después umbral de transparencia: lo que queda por encima del umbral, nítido. */
@RequiresApi(Build.VERSION_CODES.S)
private fun gooeyEffect(blurPx: Float): RenderEffect {
    val blur = RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.DECAL)
    val threshold = ColorMatrix(
        floatArrayOf(
            1f, 0f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 0f,
            // alfa' = 18·alfa − 7·255: corte en ~40 % de opacidad
            0f, 0f, 0f, 18f, -7f * 255f,
        )
    )
    return RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(threshold), blur)
}

private const val BLOB_COUNT = 6
private const val BLUR_DP = 7f
