package com.jjas.labpomodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.ui.main.Vessel
import com.jjas.labpomodoro.ui.main.VesselUi
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

/**
 * Maquetas de la reorganización de menús (no hacen nada al tocarlas). Extra "variant":
 * 1 menú agrupado, 2 barra inferior, 3 pantalla "Tu progreso", 4 Configuración en tres grupos.
 */
class MenuMockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val variant = intent.getIntExtra("variant", 1)
        setContent {
            LabPomodoroTheme {
                when (variant) {
                    1 -> GroupedMenu()
                    2 -> BottomBar()
                    3 -> ProgressScreen()
                    else -> SettingsGroups()
                }
            }
        }
    }
}

/** La pantalla de inicio de siempre, sin plan corriendo. */
@Composable
private fun Home(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Vessel(VesselUi.Resting, Modifier.weight(1f).fillMaxWidth())
        Text("25:00", fontSize = 64.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("Toca el reloj para ajustar este plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupedMenu() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Home(Modifier.safeDrawingPadding())
        ModalBottomSheet(onDismissRequest = {}, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
                Section("Este plan")
                Item(R.drawable.ic_more_time, "Plan", "Alargar, sumar o quitar pomodoros")
                Item(R.drawable.ic_moon, "Ambiente", "Solo el recipiente y el reloj, atenuados")
                Item(R.drawable.ic_headphones, "Sonido", "Ruido, lluvia u olas mientras trabajas")
                Section("Laboratorio")
                Item(R.drawable.ic_star, "Tabla periódica", "Tu colección de elementos y su maestría")
                Item(R.drawable.ic_stat_timer, "Sintetizador", "Fusiona elementos para crear otros")
                Section("Tu progreso")
                Item(R.drawable.ic_chart, "Estadísticas", "Tiempo de enfoque, racha y etiquetas")
                Item(R.drawable.ic_check, "Medallas", "10 de 17 ganadas")
                Item(R.drawable.ic_arrow_forward, "Liga", "Liga Carbono · 3.er lugar esta semana")
                Section("App")
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Chip(R.drawable.ic_tune, "Configuración")
                    Chip(R.drawable.ic_help, "Guía")
                    Chip(R.drawable.ic_diamond, "Pro")
                    Chip(R.drawable.ic_person, "Cuenta")
                }
            }
        }
    }
}

@Composable
private fun BottomBar() {
    Scaffold(
        topBar = {
            Row(Modifier.safeDrawingPadding().fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_diamond), null) }
                IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_help), null) }
                IconButton(onClick = {}) { Icon(painterResource(R.drawable.ic_tune), null) }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(true, {}, { Icon(painterResource(R.drawable.ic_stat_timer), null) }, label = { Text("Timer") })
                NavigationBarItem(false, {}, { Icon(painterResource(R.drawable.ic_star), null) }, label = { Text("Laboratorio") })
                NavigationBarItem(false, {}, { Icon(painterResource(R.drawable.ic_chart), null) }, label = { Text("Progreso") })
            }
        },
    ) { padding -> Home(Modifier.padding(padding)) }
}

@Composable
private fun ProgressScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            Text("Tu progreso", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp))
            PrimaryTabRow(selectedTabIndex = 0) {
                Tab(true, {}, text = { Text("Estadísticas") })
                Tab(false, {}, text = { Text("Medallas") })
                Tab(false, {}, text = { Text("Liga") })
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Resumen, gráficas, etiquetas y lugares", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsGroups() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeDrawingPadding()) {
            Text("Configuración", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(24.dp))
            Item(R.drawable.ic_stat_timer, "Temporizador", "Plan por defecto, avisos, modo ambiente y sonido de concentración")
            Item(R.drawable.ic_tune, "Apariencia", "Negro puro, colores, paletas, recipientes y atajos")
            Item(R.drawable.ic_person, "Cuenta y datos", "Respaldo en la nube, exportar CSV, idioma y acerca de")
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun Item(icon: Int, title: String, description: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        leadingContent = { Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun Chip(icon: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalButton(onClick = {}, modifier = Modifier.size(56.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Icon(painterResource(icon), null)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
    }
}
