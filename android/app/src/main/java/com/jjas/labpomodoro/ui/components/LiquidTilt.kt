package com.jjas.labpomodoro.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.atan2

/** Inclinación máxima de la superficie (radianes): más allá el líquido se saldría del dibujo. */
private const val MAX_TILT = 0.45f

/** Por debajo de esto el teléfono se considera quieto (evita redibujar por el ruido del sensor). */
private const val REST = 0.012f

/**
 * Ángulo de la superficie del líquido en la pantalla (radianes; positivo = el lado derecho sube),
 * para que se mantenga horizontal al ladear el teléfono. Un resorte amortiguado sigue al sensor:
 * al mover el teléfono el líquido chapotea un poco y se calma. Con el teléfono quieto no pide
 * cuadros nuevos, así que no gasta batería. Con [enabled] en false siempre vale 0.
 */
@Composable
fun rememberLiquidTilt(enabled: Boolean): State<Float> {
    val angle = remember { mutableFloatStateOf(0f) }
    if (!enabled) {
        angle.floatValue = 0f
        return angle
    }
    val context = LocalContext.current
    val target = remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = manager.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // Girar el teléfono a la derecha (x negativa) hace que el horizonte suba por la derecha
                val raw = atan2(-event.values[0], event.values[1]).coerceIn(-MAX_TILT, MAX_TILT)
                // Filtro suave contra la vibración de la mano
                target.floatValue += (raw - target.floatValue) * 0.25f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensor?.let { manager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { manager.unregisterListener(listener) }
    }

    LaunchedEffect(Unit) {
        var velocity = 0f
        while (true) {
            // Quieto: espera a que el teléfono se mueva
            if (abs(target.floatValue - angle.floatValue) < REST && abs(velocity) < REST) {
                velocity = 0f
                snapshotFlow { target.floatValue }.first { abs(it - angle.floatValue) >= REST }
            }
            var last = 0L
            // Resorte amortiguado hasta que se calme
            do {
                withFrameNanos { now ->
                    val dt = if (last == 0L) 0f else ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
                    last = now
                    val acceleration = 55f * (target.floatValue - angle.floatValue) - 6f * velocity
                    velocity += acceleration * dt
                    angle.floatValue = (angle.floatValue + velocity * dt).coerceIn(-MAX_TILT, MAX_TILT)
                }
            } while (abs(target.floatValue - angle.floatValue) >= REST / 2 || abs(velocity) >= REST)
        }
    }
    return angle
}
