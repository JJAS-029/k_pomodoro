package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementCategory
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.ui.theme.isLight

/** Color de cada familia de la tabla periódica, en tonos suaves sobre fondo negro. */
fun ElementCategory.color(): Color = Color.hsl(hue(), saturation(), 0.62f)

/** Matiz de cada familia, repartidos para que ninguna se confunda con su vecina. */
fun ElementCategory.hue(): Float = when (this) {
    ElementCategory.ALKALI_METAL -> 2f
    ElementCategory.ALKALINE_EARTH_METAL -> 28f
    ElementCategory.TRANSITION_METAL -> 212f
    ElementCategory.POST_TRANSITION_METAL -> 192f
    ElementCategory.METALLOID -> 96f
    ElementCategory.NONMETAL -> 162f
    ElementCategory.HALOGEN -> 46f
    ElementCategory.NOBLE_GAS -> 274f
    ElementCategory.LANTHANIDE -> 326f
    ElementCategory.ACTINIDE -> 246f
    ElementCategory.UNKNOWN -> 220f
}

/** Saturación: casi todas igual de vivas; "otros metales" va acerado y los desconocidos, grises. */
fun ElementCategory.saturation(): Float = when (this) {
    ElementCategory.POST_TRANSITION_METAL -> 0.28f
    ElementCategory.UNKNOWN -> 0.06f
    else -> 0.72f
}

/** A partir de este tamaño la casilla es una "tarjeta": nombre y barra de maestría. */
private val CARD_SIZE = 56.dp

/** Por debajo solo cabe el símbolo (tabla completa en vertical). */
private val COMPACT_SIZE = 32.dp

/** Marco del oro en la casilla: más claro que [GoldColor] para que brille sobre el cristal. */
private val GoldFrame = Color(0xFFFFD54F)

/**
 * Casilla de la tabla periódica, estilo "neón vial": un frasco casi negro con borde luminoso del
 * color de su familia y su líquido adentro. El nivel del líquido es la maestría: en la tabla, el
 * nivel alcanzado; en las tarjetas grandes, el avance hacia el siguiente metal. Si aún no se
 * descubre se ve el frasco vacío y apagado, con el símbolo tenue.
 *
 * @param progress avance hacia el siguiente nivel de maestría (0..1); si es null, el nivel sale de
 *   [mastery].
 * @param specular corrimiento horizontal del reflejo (-1..1), para que se mueva al ladear el
 *   teléfono; se lee al dibujar, así que no recompone.
 */
@Composable
fun ElementTile(
    element: Element,
    discovered: Boolean,
    modifier: Modifier = Modifier,
    quantity: Int = 0,
    size: Dp = 40.dp,
    highlighted: Boolean = false,
    mastery: Mastery = Mastery.NONE,
    progress: Float? = null,
    specular: (() -> Float)? = null,
) {
    val light = MaterialTheme.colorScheme.isLight
    val palette = element.glass(light)
    val compact = size < COMPACT_SIZE
    val card = size >= CARD_SIZE
    val shiny = discovered && (mastery == Mastery.SILVER || mastery == Mastery.GOLD)
    // Una sola animación para todas; solo las de plata y oro la leen (y solo al dibujar)
    val shimmer: State<Float>? = if (shiny) LocalGlassShimmer.current ?: rememberGlassShimmer() else null
    val frame: Color? = when {
        highlighted -> if (light) MaterialTheme.colorScheme.primary else Color.White
        !discovered -> null
        mastery == Mastery.GOLD -> GoldFrame
        mastery == Mastery.SILVER -> SilverColor
        mastery == Mastery.BRONZE -> BronzeColor
        else -> null
    }
    val glow = if (shiny && !highlighted) frame else null
    val borderWidth = if (frame != null) (size * 0.05f).coerceIn(1.dp, 2.2.dp) else (size * 0.0375f).coerceIn(0.75.dp, 1.5.dp)
    // Nivel del líquido: el avance si lo hay; si no, el escalón de maestría
    val level = when {
        !discovered -> 0f
        progress != null -> 0.12f + 0.83f * progress.coerceIn(0f, 1f)
        else -> when (mastery) {
            Mastery.NONE, Mastery.DISCOVERED -> 0.3f
            Mastery.BRONZE -> 0.5f
            Mastery.SILVER -> 0.7f
            Mastery.GOLD -> 0.9f
        }
    }
    // Ola diagonal en la tabla: cada casilla brillante pasa el destello un poco después que la anterior
    val phaseOffset = ((element.group ?: 3) + element.period) * 0.035f

    val name = element.localizedName()
    val description = when {
        !discovered -> stringResource(R.string.el_tile_undiscovered, name)
        quantity > 0 -> stringResource(R.string.el_tile_quantity, name, quantity)
        else -> name
    }
    val density = LocalDensity.current
    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description }
            .drawWithCache {
                val w = this.size.width
                val h = this.size.height
                val radius = w * 0.14f
                val clip = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(radius))) }
                val background = if (discovered) {
                    Brush.linearGradient(
                        0f to palette.top,
                        0.5f to palette.mid,
                        1f to palette.deep,
                        start = Offset.Zero,
                        end = Offset(w, h),
                    )
                } else {
                    Brush.linearGradient(listOf(palette.dimTop, palette.dimDeep), Offset.Zero, Offset(w, h))
                }
                val lens = specularBrush(w, h, 0f, discovered)
                val surface = h * (1f - level)
                val liquid = Brush.verticalGradient(listOf(palette.liquidTop, palette.liquidDeep), startY = surface, endY = h)
                val bottomGlow = Brush.radialGradient(
                    listOf(palette.border.copy(alpha = if (light) 0.18f else 0.35f), Color.Transparent),
                    center = Offset(w / 2f, h),
                    radius = w * 0.9f,
                )
                val bw = borderWidth.toPx()
                val edge = frame ?: if (discovered) palette.border else palette.dimBorder
                val rim = Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (discovered) 0.55f else 0.15f),
                    0.3f to Color.Transparent,
                    startY = 0f,
                    endY = h,
                )
                val glowWidth = (size * 0.07f).coerceIn(1.5.dp, 5.dp).toPx()
                onDrawWithContent {
                    // Halo de plata u oro: trazos anchos y tenues alrededor (sin blur, que no hay en API 26)
                    if (glow != null) {
                        drawRoundRect(glow.copy(alpha = 0.14f), cornerRadius = CornerRadius(radius), style = Stroke(glowWidth * 2f))
                        drawRoundRect(glow.copy(alpha = 0.28f), cornerRadius = CornerRadius(radius), style = Stroke(glowWidth))
                    }
                    clipPath(clip) {
                        drawRect(background)
                        if (level > 0f) {
                            drawRect(bottomGlow)
                            drawRect(liquid, topLeft = Offset(0f, surface), size = Size(w, h - surface))
                            // Menisco: una línea de luz en la superficie del líquido
                            drawRect(
                                palette.border.copy(alpha = 0.9f),
                                topLeft = Offset(0f, surface),
                                size = Size(w, (h * 0.025f).coerceAtLeast(1f)),
                            )
                        }
                        // Reflejo convexo de lente; se corre con la inclinación si la hay
                        drawRect(if (specular != null) specularBrush(w, h, specular(), discovered) else lens)
                        this@onDrawWithContent.drawContent()
                        if (shimmer != null) drawShimmer(w, h, (shimmer.value + phaseOffset) % 1f, mastery == Mastery.GOLD)
                    }
                    drawRoundRect(
                        edge,
                        topLeft = Offset(bw / 2, bw / 2),
                        size = Size(w - bw, h - bw),
                        cornerRadius = CornerRadius((radius - bw / 2).coerceAtLeast(0f)),
                        style = Stroke(bw),
                    )
                    // Filo de luz arriba, por dentro del borde
                    if (!compact) {
                        drawRoundRect(
                            rim,
                            topLeft = Offset(bw, bw),
                            size = Size(w - bw * 2, h - bw * 2),
                            cornerRadius = CornerRadius((radius - bw).coerceAtLeast(0f)),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                }
            },
    ) {
        val faint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (light) 0.4f else 0.35f)
        val symbolColor = if (discovered) palette.symbol else faint
        val numberColor = if (discovered) palette.number else faint
        val shadow = if (discovered) {
            Shadow(palette.symbolShadow, Offset(0f, with(density) { (size * 0.02f).toPx() }), with(density) { (size * 0.08f).toPx() })
        } else {
            null
        }
        // Tamaños en proporción a la casilla (dp → sp sin escala de fuente: el texto debe caber)
        fun ratio(f: Float): TextUnit = with(density) { (size * f).toSp() }
        when {
            compact -> Text(
                text = element.symbol,
                style = tileText(ratio(0.45f), FontWeight.Bold, symbolColor, shadow),
                modifier = Modifier.align(Alignment.Center),
            )
            card -> {
                Text(
                    text = element.atomicNumber.toString(),
                    style = tileText(ratio(0.13f), FontWeight.Medium, numberColor, null),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = size * 0.09f, top = size * 0.07f),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = -size * 0.02f)
                        .padding(horizontal = size * 0.06f),
                ) {
                    Text(element.symbol, style = tileText(ratio(0.30f), FontWeight.Bold, symbolColor, shadow))
                    Text(
                        text = name,
                        style = tileText(ratio(0.115f), FontWeight.Medium, symbolColor.copy(alpha = if (discovered) 0.85f else symbolColor.alpha), null),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (discovered && quantity > 1) {
                    CountBadge(
                        quantity,
                        ratio(0.11f),
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = size * 0.07f, bottom = size * 0.06f),
                    )
                }
            }
            else -> {
                Text(
                    text = element.atomicNumber.toString(),
                    style = tileText(ratio(0.1875f), FontWeight.Medium, numberColor, null),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = size * 0.075f, top = size * 0.04f),
                )
                Text(
                    text = element.symbol,
                    style = tileText(ratio(0.36f), FontWeight.Bold, symbolColor, shadow),
                    modifier = Modifier.align(Alignment.Center),
                )
                if (discovered && quantity > 1) {
                    CountBadge(
                        quantity,
                        ratio(0.17f),
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = size * 0.05f, bottom = size * 0.04f),
                    )
                }
            }
        }
    }
}

/** "×N" en una pastilla oscura translúcida, legible sobre cualquier tono. */
@Composable
private fun CountBadge(quantity: Int, fontSize: TextUnit, modifier: Modifier) {
    Box(
        modifier
            .background(Color.Black.copy(alpha = 0.38f), RoundedCornerShape(50))
            .padding(horizontal = 3.dp),
    ) {
        Text("×$quantity", style = tileText(fontSize, FontWeight.SemiBold, Color.White, null))
    }
}

private fun tileText(fontSize: TextUnit, weight: FontWeight, color: Color, shadow: Shadow?) = TextStyle(
    color = color,
    fontSize = fontSize,
    fontWeight = weight,
    shadow = shadow,
    // Sin el interlineado del tema: en casillas chicas el alto de línea desplaza el texto
    lineHeight = fontSize * 1.15f,
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

/** Reflejo convexo: blanco arriba a la izquierda que se desvanece hacia el centro. */
private fun specularBrush(w: Float, h: Float, shift: Float, discovered: Boolean): Brush = Brush.radialGradient(
    0f to Color.White.copy(alpha = if (discovered) 0.16f else 0.06f),
    1f to Color.Transparent,
    center = Offset(w * (0.22f + 0.3f * shift.coerceIn(-1f, 1f)), h * 0.18f),
    radius = maxOf(w, h) * 0.7f,
)

/** Banda blanca translúcida que cruza en diagonal; [phase] 0..1, cruza en el primer 40 %. */
private fun DrawScope.drawShimmer(w: Float, h: Float, phase: Float, gold: Boolean) {
    val t = phase / 0.4f
    if (t > 1f) return
    val pos = -0.35f + t * 1.7f
    val band = 0.18f
    val tint = if (gold) Color(0xFFFFF8E1) else Color.White
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, tint.copy(alpha = 0.45f), Color.Transparent),
            start = Offset(w * (pos - band), h * (pos - band)),
            end = Offset(w * (pos + band), h * (pos + band)),
        )
    )
}

val BronzeColor = Color(0xFFCD7F32)
val SilverColor = Color(0xFFD8DEE4)
val GoldColor = Color(0xFFFFC94A)

/** Color del nivel de maestría; null para los niveles sin metal. */
fun Mastery.metalColor(): Color? = when (this) {
    Mastery.BRONZE -> BronzeColor
    Mastery.SILVER -> SilverColor
    Mastery.GOLD -> GoldColor
    else -> null
}
