package com.jjas.labpomodoro.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.service.formatMinutesSeconds
import com.jjas.labpomodoro.service.label
import com.jjas.labpomodoro.ui.theme.LabAmber
import com.jjas.labpomodoro.ui.theme.LabCyan
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import com.jjas.labpomodoro.ui.theme.NeonGreen

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    timerViewModel: TimerViewModel = hiltViewModel(),
) {
    val account by viewModel.uiState.collectAsStateWithLifecycle()
    val timer by timerViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Modo AOD: la pantalla no se apaga mientras corre la sesión (si el usuario lo activó)
    val view = LocalView.current
    DisposableEffect(timer.keepScreenOn) {
        view.keepScreenOn = timer.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // En Android 13+ hay que pedir permiso para mostrar la cuenta regresiva en la notificación.
    // El timer arranca igual si el usuario lo niega.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { timerViewModel.start() }
    val onStart = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            timerViewModel.start()
        }
    }

    MainContent(
        timer = timer.timer,
        account = account,
        actions = TimerActions(
            onStart = onStart,
            onPause = timerViewModel::pause,
            onResume = timerViewModel::resume,
            onSkip = timerViewModel::skip,
            onStop = timerViewModel::stop,
            onDismissSummary = timerViewModel::dismissSummary,
        ),
        onOpenSettings = onOpenSettings,
        onOpenAchievements = onOpenAchievements,
        onOpenPro = onOpenPro,
        onSignIn = { viewModel.signIn(context) },
        onSignOut = viewModel::signOut,
    )
}

data class TimerActions(
    val onStart: () -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onDismissSummary: () -> Unit = {},
)

/** El vaso de precipitados y la repisa de tubos reemplazan el centro en la Fase 3. */
@Composable
private fun MainContent(
    timer: TimerUi?,
    account: MainUiState,
    actions: TimerActions,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPro: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Lab Pomodoro",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.weight(1f))
            when (timer) {
                null -> CircularProgressIndicator()
                is TimerUi.Idle -> IdlePanel(timer)
                is TimerUi.Active -> ActivePanel(timer)
                is TimerUi.Finished -> FinishedPanel(timer)
            }
            Spacer(Modifier.weight(1f))

            when (timer) {
                is TimerUi.Active -> ActiveControls(timer, actions)
                is TimerUi.Finished -> Button(onClick = actions.onDismissSummary, modifier = Modifier.fillMaxWidth()) {
                    Text("Nuevo experimento")
                }
                else -> Button(onClick = actions.onStart, enabled = timer != null, modifier = Modifier.fillMaxWidth()) {
                    Text("Start")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onOpenSettings, modifier = Modifier.weight(1f)) { Text("Config") }
                OutlinedButton(onClick = onOpenAchievements, modifier = Modifier.weight(1f)) { Text("Logros") }
                OutlinedButton(onClick = onOpenPro, modifier = Modifier.weight(1f)) { Text("Pro") }
            }

            Spacer(Modifier.height(24.dp))
            AccountSection(state = account, onSignIn = onSignIn, onSignOut = onSignOut)
        }
    }
}

@Composable
private fun ClockText(millis: Long, color: Color = MaterialTheme.colorScheme.onBackground) {
    Text(
        text = formatMinutesSeconds(millis),
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp),
        color = color,
    )
}

@Composable
private fun IdlePanel(idle: TimerUi.Idle) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ClockText(idle.firstSessionMillis)
        Text(
            text = "${idle.pomodoros} pomodoros · ${formatDuration(idle.workMinutes)} de trabajo",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Con descansos: ${formatDuration(idle.totalMinutes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActivePanel(active: TimerUi.Active) {
    val color = active.type.color()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(active.type.label(), style = MaterialTheme.typography.titleLarge, color = color)
        ClockText(active.remainingMillis, color = if (active.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground)
        LinearProgressIndicator(
            progress = { active.progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        )
        Text(
            text = "Sesión ${active.sessionNumber} de ${active.totalSessions}" +
                if (active.isPaused) " · En pausa" else "",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = active.next?.let { "Después: ${it.label().lowercase()}" } ?: "Última sesión",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FinishedPanel(finished: TimerUi.Finished) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "¡Experimento completado!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${finished.workSessions} pomodoros · ${formatDuration(finished.workMinutes.toInt())} de trabajo",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ActiveControls(active: TimerUi.Active, actions: TimerActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (active.isPaused) {
            Button(onClick = actions.onResume, modifier = Modifier.weight(1f)) { Text("Continuar") }
        } else {
            Button(onClick = actions.onPause, modifier = Modifier.weight(1f)) { Text("Pausar") }
        }
        OutlinedButton(onClick = actions.onSkip, modifier = Modifier.weight(1f)) { Text("Saltar") }
        OutlinedButton(onClick = actions.onStop, modifier = Modifier.weight(1f)) { Text("Detener") }
    }
}

/** Temporal (Fase 0.5): verifica Auth + Firestore de punta a punta. */
@Composable
private fun AccountSection(state: MainUiState, onSignIn: () -> Unit, onSignOut: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            state.isBusy -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            state.isSignedIn -> {
                Text(
                    text = "Hola, ${state.userName ?: "científico"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                TextButton(onClick = onSignOut) { Text("Cerrar sesión") }
            }
            else -> TextButton(onClick = onSignIn) { Text("Iniciar sesión con Google") }
        }
        state.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun SessionType.color(): Color = when (this) {
    SessionType.WORK -> NeonGreen
    SessionType.SHORT_BREAK -> LabCyan
    SessionType.LONG_BREAK -> LabAmber
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MainContentActivePreview() {
    LabPomodoroTheme {
        MainContent(
            timer = TimerUi.Active(SessionType.WORK, 754_000, 0.5f, 3, 9, isPaused = false, next = SessionType.SHORT_BREAK),
            account = MainUiState(),
            actions = TimerActions(),
            onOpenSettings = {}, onOpenAchievements = {}, onOpenPro = {}, onSignIn = {}, onSignOut = {},
        )
    }
}
