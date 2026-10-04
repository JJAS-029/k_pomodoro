package com.jjas.labpomodoro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.R

// Las .ttf de res/font son fuentes variables: cada peso se obtiene ajustando el eje "wght".
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

val SpaceGrotesk = FontFamily(
    variableFont(R.font.space_grotesk, FontWeight.Normal),
    variableFont(R.font.space_grotesk, FontWeight.Medium),
    variableFont(R.font.space_grotesk, FontWeight.Bold),
)

/** Para el reloj: dígitos monoespaciados, así el tiempo no "baila" al cambiar. */
val JetBrainsMono = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Normal),
    variableFont(R.font.jetbrains_mono, FontWeight.Bold),
)

private val base = Typography()

val LabTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold),
    displaySmall = base.displaySmall.copy(fontFamily = JetBrainsMono),
    headlineLarge = base.headlineLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    titleMedium = base.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    titleSmall = base.titleSmall.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = base.bodyMedium.copy(fontFamily = SpaceGrotesk),
    bodySmall = base.bodySmall.copy(fontFamily = SpaceGrotesk),
    labelLarge = base.labelLarge.copy(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium),
    labelMedium = base.labelMedium.copy(fontFamily = SpaceGrotesk),
    labelSmall = base.labelSmall.copy(fontFamily = SpaceGrotesk),
)
