package com.jjas.labpomodoro.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.RectF
import android.graphics.Region
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt

/**
 * Lente que sigue la forma del recipiente: en el centro el líquido se ve tal cual y hacia las
 * paredes se deforma y se separan un poco los colores (aberración cromática), como al mirar a
 * través de vidrio curvo. El ancho del recipiente cambia con la altura (matraz redondo,
 * Erlenmeyer), así que el medio ancho real del vidrio en [ROWS] alturas viaja en una imagen de
 * ROWS×1 píxeles (en la transparencia, que no cambia con el espacio de color) que el shader lee
 * como textura: AGSL no deja leer un arreglo con un índice variable.
 */
private const val GLASS_SHADER = """
uniform shader content;
uniform float centerX;
uniform float bottomY;
uniform float topY;
uniform float strength;
uniform float maxHalfWidth;
uniform shader widths;

float halfWidthAt(float y) {
    float t = clamp((bottomY - y) / (bottomY - topY), 0.0, 1.0);
    return widths.eval(float2(t * 63.0 + 0.5, 0.5)).a * maxHalfWidth;
}

half4 main(float2 p) {
    half4 here = content.eval(p);
    float hw = halfWidthAt(p.y);
    if (hw <= 1.0) {
        return here;
    }
    float d = (p.x - centerX) / hw;
    float ad = abs(d);
    // Fuera del líquido no hay nada que deformar: así la lente no saca líquido del vidrio
    if (ad >= 1.0 || here.a < 0.02) {
        return here;
    }
    // Casi nada en el centro y mucho cerca de la pared, a cualquier altura
    float bend = strength * ad * ad * ad;
    float2 q = float2(p.x - sign(d) * bend * hw, p.y);
    float spread = bend * hw * 0.06;
    half4 c = content.eval(q);
    c.r = content.eval(q + float2(spread, 0.0)).r;
    c.b = content.eval(q - float2(spread, 0.0)).b;
    return c;
}
"""

/** El shader de vidrio, o null si el teléfono no tiene AGSL (antes de Android 13). */
fun createGlassShader(): Any? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RuntimeShader(GLASS_SHADER) else null

/**
 * Aplica la refracción al contenido (líquido y gotas). [shader] viene de [createGlassShader]; se
 * recibe como Any para no mencionar RuntimeShader en teléfonos que no lo tienen.
 */
fun Modifier.glassRefraction(shape: VesselShape, shader: Any?): Modifier {
    if (shader == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    return graphicsLayer {
        val g = VesselGeometry.fit(shape, size.width, size.height, headroom = 0.18f)
        renderEffect = refraction(shader as RuntimeShader, g)?.asComposeRenderEffect()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun refraction(shader: RuntimeShader, g: VesselGeometry): RenderEffect? {
    // Se mide el interior real del vidrio, fila por fila, a partir de su contorno
    val interior = g.interior.asAndroidPath()
    val bounds = RectF().also { interior.computeBounds(it, true) }
    if (bounds.width() <= 0f || bounds.height() <= 0f) return null
    val region = Region()
    val row = Region()
    val widths = FloatArray(ROWS) { i ->
        // De abajo hacia arriba
        val y = (bounds.bottom - bounds.height() * i / (ROWS - 1f)).toInt()
        row.set(bounds.left.toInt() - 1, y - 1, bounds.right.toInt() + 1, y + 1)
        if (region.setPath(interior, row)) region.bounds.width() / 2f else 0f
    }
    shader.setFloatUniform("centerX", bounds.centerX())
    shader.setFloatUniform("bottomY", bounds.bottom)
    shader.setFloatUniform("topY", bounds.top)
    shader.setFloatUniform("strength", STRENGTH)
    val max = widths.max().coerceAtLeast(1f)
    val texture = Bitmap.createBitmap(ROWS, 1, Bitmap.Config.ARGB_8888)
    widths.forEachIndexed { i, w -> texture.setPixel(i, 0, android.graphics.Color.argb((w / max * 255f).roundToInt(), 0, 0, 0)) }
    val widthShader = BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
        filterMode = BitmapShader.FILTER_MODE_LINEAR
    }
    shader.setFloatUniform("maxHalfWidth", max)
    shader.setInputShader("widths", widthShader)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}

/** Alturas de la tabla de anchos (debe coincidir con el arreglo del shader). */
private const val ROWS = 64

/** Cuánto se deforma junto a la pared, en fracciones del medio ancho. */
private const val STRENGTH = 0.18f
