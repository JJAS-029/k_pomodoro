package com.jjas.labpomodoro.ui.components

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Lente cilíndrica: el recipiente es un cilindro de vidrio, así que en el centro el líquido se ve
 * tal cual y hacia las paredes se deforma y se separan un poco los colores (aberración cromática),
 * como al mirar a través de un vaso real.
 */
private const val GLASS_SHADER = """
uniform shader content;
uniform float centerX;
uniform float halfWidth;
uniform float strength;

half4 main(float2 p) {
    float d = (p.x - centerX) / halfWidth;
    float ad = abs(d);
    half4 here = content.eval(p);
    // Fuera del líquido no hay nada que deformar: así la lente no saca líquido del vidrio
    if (ad >= 1.0 || here.a < 0.02) {
        return here;
    }
    // Casi nada en el centro y mucho cerca de la pared
    float bend = strength * ad * ad * ad;
    float2 q = float2(p.x - sign(d) * bend * halfWidth, p.y);
    float spread = bend * halfWidth * 0.06;
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
        renderEffect = refraction(shader as RuntimeShader, g.px(0.5f), g.halfWidth(g.maxFill / 2f) * g.width)
            ?.asComposeRenderEffect()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun refraction(shader: RuntimeShader, centerX: Float, halfWidth: Float): RenderEffect? {
    if (halfWidth <= 0f) return null
    shader.setFloatUniform("centerX", centerX)
    shader.setFloatUniform("halfWidth", halfWidth)
    shader.setFloatUniform("strength", STRENGTH)
    return RenderEffect.createRuntimeShaderEffect(shader, "content")
}

/** Cuánto se deforma junto a la pared, en fracciones del medio ancho. */
private const val STRENGTH = 0.18f
