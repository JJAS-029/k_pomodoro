package com.jjas.labpomodoro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

private val LabDarkScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = TrueBlack,
    secondary = LabAmber,
    onSecondary = TrueBlack,
    tertiary = LabCyan,
    onTertiary = TrueBlack,
    background = TrueBlack,
    onBackground = LabText,
    surface = TrueBlack,
    onSurface = LabText,
    surfaceVariant = LabSurfaceVariant,
    onSurfaceVariant = LabText,
    outline = LabOutline,
)

// Los mismos tonos de laboratorio, más oscuros para que se lean sobre blanco
private val LabLightScheme = lightColorScheme(
    primary = LabGreenDeep,
    onPrimary = LabPaper,
    // Opciones elegidas (botones segmentados, chips): verde claro en vez del morado por defecto
    primaryContainer = LabGreenMist,
    onPrimaryContainer = LabInk,
    secondaryContainer = LabGreenMist,
    onSecondaryContainer = LabInk,
    secondary = LabAmberDeep,
    onSecondary = LabPaper,
    tertiary = LabCyanDeep,
    onTertiary = LabPaper,
    background = LabPaper,
    onBackground = LabInk,
    surface = LabPaper,
    onSurface = LabInk,
    surfaceVariant = LabPaperVariant,
    onSurfaceVariant = LabInkMuted,
    outline = LabOutlineLight,
)

/** true con el tema claro; los dibujos (vidrio, vapor) cambian de blanco a gris. */
val ColorScheme.isLight: Boolean get() = background.luminance() > 0.5f

/**
 * Sigue el tema del teléfono (claro u oscuro); el modo ambiente y la ventana flotante lo fuerzan a
 * oscuro con [darkTheme]. Con color dinámico (Material You) el oscuro conserva el fondo negro puro.
 */
@Composable
fun LabPomodoroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scheme = when {
        dynamic && darkTheme -> dynamicDarkColorScheme(LocalContext.current).copy(background = TrueBlack, surface = TrueBlack)
        dynamic -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> LabDarkScheme
        else -> LabLightScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = LabTypography,
        content = content
    )
}
