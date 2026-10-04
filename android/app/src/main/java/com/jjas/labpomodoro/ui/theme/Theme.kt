package com.jjas.labpomodoro.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
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

/**
 * La app siempre es oscura. [dynamicColor] (Material You) queda reservado para la versión Pro (Fase 5);
 * aun con color dinámico se fuerza el fondo negro puro.
 */
@Composable
fun LabPomodoroTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current).copy(background = TrueBlack, surface = TrueBlack)
    } else {
        LabDarkScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = LabTypography,
        content = content
    )
}
