package com.jjas.labpomodoro.ui.main

import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.jjas.labpomodoro.service.formatMinutesSeconds
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Modo ambiente (Pro): emula un "always on". Quedan solo el recipiente, el reloj y el indicador de
 * sesión, atenuados sobre negro (en OLED casi no gasta); el título, la repisa y el dock se ocultan.
 * Todo se desplaza un poco cada minuto para no marcar la pantalla. Un toque vuelve a la vista normal.
 */
@Composable
fun AmbientScreen(active: TimerUi.Active, onWake: () -> Unit, dynamicColor: Boolean = false) {
    var dx by remember { mutableIntStateOf(0) }
    var dy by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            dx = Random.nextInt(-12, 13)
            dy = Random.nextInt(-24, 25)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = null, indication = null, onClick = onWake),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
                .offset { IntOffset(dx.dp.roundToPx(), dy.dp.roundToPx()) }
                .alpha(0.75f),
        ) {
            // Tamaño justo al recipiente (con su espacio para el vapor) para centrar el grupo
            Vessel(
                vessel = active.vessel,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .aspectRatio(active.vessel.shape.aspect / 0.8f, matchHeightConstraintsFirst = true),
            )
            Text(
                text = formatMinutesSeconds(active.remainingMillis),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                // Con Material You el reloj conserva el color del sistema, atenuado
                color = if (dynamicColor) MaterialTheme.colorScheme.primary else Color(0xFF9E9E9E),
            )
            Spacer(Modifier.height(8.dp))
            ActiveIndicator(active)
        }
    }
}

private const val AMBIENT_BRIGHTNESS = 0.05f

/**
 * Mientras dura el modo ambiente: barras del sistema ocultas y brillo bajo. El brillo baja poco a
 * poco al entrar (como al apagarse el celular) y sube rápido al despertar.
 */
@Composable
fun AmbientWindowEffect(ambient: Boolean) {
    val activity = LocalActivity.current ?: return
    val window = activity.window
    // Punto de partida/llegada: el brillo que tiene el sistema (aproximado si el brillo es automático)
    val systemBrightness = remember {
        val raw = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        (raw / 255f).coerceIn(AMBIENT_BRIGHTNESS, 1f)
    }

    fun setBrightness(value: Float) {
        window.attributes = window.attributes.apply { screenBrightness = value }
    }

    LaunchedEffect(ambient) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val current = window.attributes.screenBrightness
        if (ambient) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            animate(if (current >= 0f) current else systemBrightness, AMBIENT_BRIGHTNESS, animationSpec = tween(1200)) { v, _ ->
                setBrightness(v)
            }
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            if (current >= 0f) {
                animate(current, systemBrightness, animationSpec = tween(400)) { v, _ -> setBrightness(v) }
            }
            // Devuelve el control del brillo al sistema
            setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
            WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
