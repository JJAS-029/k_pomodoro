package com.jjas.labpomodoro.ui.main

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    MainContent(
        state = state,
        onOpenSettings = onOpenSettings,
        onOpenAchievements = onOpenAchievements,
        onOpenPro = onOpenPro,
        onSignIn = { viewModel.signIn(context) },
        onSignOut = viewModel::signOut,
    )
}

/** Placeholder de la Fase 0: confirma tema, tipografía y navegación. El timer real llega en la Fase 2. */
@Composable
private fun MainContent(
    state: MainUiState,
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

            Text(
                text = "00:00",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 88.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.weight(1f))

            Button(onClick = { /* Fase 2 */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Start")
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
            AccountSection(state = state, onSignIn = onSignIn, onSignOut = onSignOut)
        }
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun MainContentPreview() {
    LabPomodoroTheme {
        MainContent(MainUiState(), {}, {}, {}, {}, {})
    }
}
