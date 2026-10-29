package com.jjas.labpomodoro.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** App del reloj: el tiempo del plan que corre en el teléfono, con sus controles. */
class WearMainActivity : ComponentActivity() {

    private lateinit var link: PhoneLink

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = PhoneLink(this)
        // Para la actividad en curso (Wear OS 4+ pide permiso para notificar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent {
            MaterialTheme {
                AppScaffold {
                    ScreenScaffold {
                        val timer by link.timer.collectAsStateWithLifecycle()
                        val scope = rememberCoroutineScope()
                        WatchTimerScreen(timer, onCommand = { command -> scope.launch { link.send(command) } })
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { link.start() }
    }

    override fun onStop() {
        link.stop()
        super.onStop()
    }
}

@Composable
private fun WatchTimerScreen(timer: WatchTimer?, onCommand: (String) -> Unit) {
    // El reloj cuenta los segundos solo, con la hora de fin que mandó el teléfono
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000)
        }
    }
    val color = timer?.color?.let(::Color) ?: Color(0xFF39FF14)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (timer != null && timer.active) {
            val remaining = timer.remainingAt(now)
            CircularProgressIndicator(
                progress = { 1f - remaining.toFloat() / timer.duration },
                modifier = Modifier.fillMaxSize().padding(2.dp),
                colors = ProgressIndicatorDefaults.colors(indicatorColor = color),
                strokeWidth = 6.dp,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            // Abajo de la hora del sistema, que va arriba al centro
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 22.dp),
        ) {
            when {
                timer == null -> Text(stringResource(R.string.open_phone), textAlign = TextAlign.Center)
                timer.active -> ActiveTimer(timer, timer.remainingAt(now), color, onCommand)
                else -> {
                    WatchVessel("BEAKER", Color(0xFF39FF14), level = 0.8f, animated = false, modifier = Modifier.size(width = 32.dp, height = 38.dp))
                    if (timer.state == "FINISHED") Text(stringResource(R.string.finished), textAlign = TextAlign.Center)
                    Button(
                        onClick = { onCommand(WearContract.START) },
                        colors = ButtonDefaults.buttonColors(containerColor = LabGreen, contentColor = Color.Black),
                    ) { Text(stringResource(R.string.start)) }
                }
            }
        }
    }
}

@Composable
private fun ActiveTimer(timer: WatchTimer, remaining: Long, color: Color, onCommand: (String) -> Unit) {
    val work = timer.type == "WORK"
    // Trabajando se evapora; en el descanso se vuelve a llenar
    val fraction = remaining.toFloat() / timer.duration
    // Con Pro, la forma de la sesión y el líquido en movimiento
    WatchVessel(timer.shape, color, level = if (work) fraction else 1f - fraction, animated = timer.isPro, modifier = Modifier.size(width = 32.dp, height = 38.dp))
    Text(
        stringResource(
            when {
                timer.paused -> R.string.paused
                work -> R.string.work
                timer.type == "LONG_BREAK" -> R.string.long_break
                else -> R.string.short_break
            }
        ),
        color = color,
        style = MaterialTheme.typography.labelMedium,
    )
    val seconds = (remaining + 999) / 1000
    Text("%d:%02d".format(seconds / 60, seconds % 60), fontSize = 30.sp, style = MaterialTheme.typography.displaySmall)
    if (timer.pomodoros > 0) Text(stringResource(R.string.pomodoro_of, timer.pomodoro, timer.pomodoros), style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledIconButton(
            onClick = { onCommand(if (timer.paused) WearContract.RESUME else WearContract.PAUSE) },
            enabled = timer.canPause,
            colors = LabButtonColors(),
        ) {
            Icon(
                painterResource(if (timer.paused) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = stringResource(if (timer.paused) R.string.resume else R.string.pause),
            )
        }
        // Saltar es Pro (y en una sala nadie salta)
        if (timer.canSkip) {
            FilledIconButton(onClick = { onCommand(WearContract.SKIP) }, colors = IconButtonDefaults.filledTonalIconButtonColors()) {
                Icon(painterResource(R.drawable.ic_skip), contentDescription = stringResource(R.string.skip))
            }
        }
    }
}

/** El verde neón de la app del teléfono. */
private val LabGreen = Color(0xFF39FF14)

@Composable
private fun LabButtonColors() = IconButtonDefaults.filledIconButtonColors(containerColor = LabGreen, contentColor = Color.Black)
