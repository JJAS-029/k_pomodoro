package com.jjas.labpomodoro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Paleta tonal de Material 3 a partir del verde de laboratorio (tono = L* de CIELAB): cada color
// tiene su contenedor suave, así las tarjetas y las opciones elegidas toman el tono de la marca
private val LabDarkScheme = darkColorScheme(
    primary = Color(0xFF98D686),
    onPrimary = Color(0xFF063900),
    primaryContainer = Color(0xFF11520A),
    onPrimaryContainer = Color(0xFFB4F3A1),
    secondary = Color(0xFFBACBB4),
    onSecondary = Color(0xFF263422),
    secondaryContainer = Color(0xFF3C4B37),
    onSecondaryContainer = Color(0xFFD6E8D0),
    tertiary = Color(0xFF72D5E5),
    onTertiary = Color(0xFF01363D),
    tertiaryContainer = Color(0xFF014F58),
    onTertiaryContainer = Color(0xFF9AF0FF),
    background = Color(0xFF10140E),
    onBackground = Color(0xFFDFE4DD),
    surface = Color(0xFF10140E),
    onSurface = Color(0xFFDFE4DD),
    surfaceVariant = Color(0xFF41493E),
    onSurfaceVariant = Color(0xFFC0C9BC),
    outline = Color(0xFF8A9387),
    outlineVariant = Color(0xFF41493E),
    inverseSurface = Color(0xFFDFE4DD),
    inverseOnSurface = Color(0xFF2E312C),
    inversePrimary = Color(0xFF2F6B23),
    surfaceDim = Color(0xFF10140E),
    surfaceBright = Color(0xFF363A35),
    surfaceContainerLowest = Color(0xFF0A1008),
    surfaceContainerLow = Color(0xFF191D18),
    surfaceContainer = Color(0xFF1D211C),
    surfaceContainerHigh = Color(0xFF272B26),
    surfaceContainerHighest = Color(0xFF323630),
)

private val LabLightScheme = lightColorScheme(
    primary = Color(0xFF2F6B23),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB4F3A1),
    onPrimaryContainer = Color(0xFF0D2000),
    secondary = Color(0xFF54634E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD6E8D0),
    onSecondaryContainer = Color(0xFF121F0C),
    tertiary = Color(0xFF006875),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9AF0FF),
    onTertiaryContainer = Color(0xFF001F24),
    background = Color(0xFFF6FBF4),
    onBackground = Color(0xFF191D18),
    surface = Color(0xFFF6FBF4),
    onSurface = Color(0xFF191D18),
    surfaceVariant = Color(0xFFDBE5D8),
    onSurfaceVariant = Color(0xFF41493E),
    outline = Color(0xFF717A6D),
    outlineVariant = Color(0xFFC0C9BC),
    inverseSurface = Color(0xFF2E312C),
    inverseOnSurface = Color(0xFFEDF2EB),
    inversePrimary = Color(0xFF98D686),
    surfaceDim = Color(0xFFD6DBD5),
    surfaceBright = Color(0xFFF6FBF4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF0F5EE),
    surfaceContainer = Color(0xFFEAEFE8),
    surfaceContainerHigh = Color(0xFFE5EAE3),
    surfaceContainerHighest = Color(0xFFDFE4DD),
)

/** Esquinas más redondeadas, al estilo Expressive. */
private val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * Negro puro para pantallas AMOLED: fondo y contenedores (tarjetas, hojas, diálogos, menús) en negros
 * profundos, con escalones mínimos para que se distingan entre sí. Sin el tinte de elevación de
 * Material, que aclara las superficies elevadas.
 */
private fun ColorScheme.pureBlack(): ColorScheme = copy(
    background = TrueBlack,
    surface = TrueBlack,
    surfaceDim = TrueBlack,
    surfaceContainerLowest = TrueBlack,
    surfaceContainerLow = PureBlackLow,
    surfaceContainer = PureBlackMid,
    surfaceContainerHigh = PureBlackHigh,
    surfaceContainerHighest = PureBlackHighest,
    surfaceVariant = PureBlackHighest,
    surfaceBright = PureBlackBright,
    surfaceTint = TrueBlack,
)

/** true con el tema claro; los dibujos (vidrio, vapor) cambian de blanco a gris. */
val ColorScheme.isLight: Boolean get() = background.luminance() > 0.5f

/**
 * Sigue el tema del teléfono (claro u oscuro); el modo ambiente y la ventana flotante lo fuerzan a
 * oscuro con [darkTheme]. Con color dinámico (Material You) el oscuro conserva el fondo negro puro;
 * con [pureBlack] también tarjetas, hojas y diálogos (pantallas AMOLED).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LabPomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    pureBlack: Boolean = true,
    content: @Composable () -> Unit
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current).copy(background = TrueBlack, surface = TrueBlack)
        dynamic -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> LabDarkScheme
        else -> LabLightScheme
    }.let { if (darkTheme && pureBlack) it.pureBlack() else it }

    // Material 3 Expressive: movimientos con resorte y las formas y componentes nuevos
    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = LabShapes,
        typography = LabTypography,
        content = content
    )
}
