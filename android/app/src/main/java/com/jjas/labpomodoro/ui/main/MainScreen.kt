package com.jjas.labpomodoro.ui.main

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.Discovery
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.service.formatMinutesSeconds
import com.jjas.labpomodoro.service.label
import com.jjas.labpomodoro.ui.components.DotState
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.PlanDot
import com.jjas.labpomodoro.ui.components.SessionIndicator
import com.jjas.labpomodoro.ui.components.ShelfItemUi
import com.jjas.labpomodoro.ui.components.UpNext
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.VesselShelf
import com.jjas.labpomodoro.ui.components.VesselView
import com.jjas.labpomodoro.ui.sound.FocusSoundPanel
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    timerViewModel: TimerViewModel = hiltViewModel(),
    discoveryViewModel: DiscoveryViewModel = hiltViewModel(),
) {
    val account by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveries by discoveryViewModel.unseen.collectAsStateWithLifecycle()
    val timer by timerViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // La primera vez se abre la guía sola (una sola vez, aunque DataStore tarde en guardar)
    var guideAutoOpened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(timer.guideSeen) {
        if (timer.guideSeen == false && !guideAutoOpened) {
            guideAutoOpened = true
            onOpenGuide()
        }
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

    // Modo ambiente (Pro): tras N minutos sin tocar la pantalla con el timer corriendo
    var ambient by remember { mutableStateOf(false) }
    var lastTouch by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    val ambientDelay = timer.ambientDelayMillis
    LaunchedEffect(ambientDelay, lastTouch) {
        if (ambientDelay != null) {
            delay(ambientDelay)
            ambient = true
        }
    }
    // Solo hay modo ambiente mientras exista una sesión; al terminar o detener se apaga
    val active = timer.timer as? TimerUi.Active
    LaunchedEffect(active == null) {
        if (active == null) ambient = false
    }
    val showAmbient = ambient && active != null

    // Hoja para elegir el sonido de concentración sin salir del timer
    var soundSheet by rememberSaveable { mutableStateOf(false) }
    if (soundSheet) {
        ModalBottomSheet(onDismissRequest = { soundSheet = false }) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text("Sonido de concentración", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                FocusSoundPanel(onOpenPro = {
                    soundSheet = false
                    onOpenPro()
                })
            }
        }
    }
    AmbientWindowEffect(showAmbient)

    // La pantalla no se apaga mientras corre la sesión (si el usuario lo activó) ni en modo ambiente
    val view = LocalView.current
    val keepScreenOn = timer.keepScreenOn || showAmbient
    DisposableEffect(keepScreenOn) {
        view.keepScreenOn = keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Transición suave: al entrar se funde a negro despacio (como al apagarse el celular);
    // al despertar es más rápida (como al encender la pantalla)
    AnimatedContent(
        targetState = showAmbient,
        transitionSpec = {
            if (targetState) {
                fadeIn(tween(700, delayMillis = 500)) togetherWith fadeOut(tween(600))
            } else {
                fadeIn(tween(350)) togetherWith fadeOut(tween(250))
            }
        },
        modifier = Modifier.background(Color.Black),
        label = "ambient",
    ) { inAmbient ->
        if (inAmbient && active != null) {
            AmbientScreen(
                active,
                dynamicColor = timer.dynamicColor,
                onWake = {
                    ambient = false
                    lastTouch = SystemClock.uptimeMillis()
                },
            )
        } else if (inAmbient) {
            // El plan terminó mientras se desvanecía el modo ambiente
            Box(Modifier.fillMaxSize())
        } else {
            MainContent(
                timer = timer.timer,
                account = account,
                showTitle = !timer.isPro,
                dynamicColor = timer.dynamicColor,
                discoveries = discoveries,
                // Observa los toques sin consumirlos, para reiniciar la cuenta del modo ambiente
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            // Solo toques reales: el mouse pasando por encima (hover) no cuenta
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type != PointerEventType.Press) continue
                            val now = SystemClock.uptimeMillis()
                            if (now - lastTouch > 1_000) lastTouch = now
                        }
                    }
                },
                actions = MainActions(
                    onStart = onStart,
                    onPause = timerViewModel::pause,
                    onResume = timerViewModel::resume,
                    onSkip = timerViewModel::skip,
                    onStop = timerViewModel::stop,
                    onDismissSummary = timerViewModel::dismissSummary,
                    onOpenSettings = onOpenSettings,
                    onOpenAchievements = onOpenAchievements,
                    onOpenPro = onOpenPro,
                    onOpenGuide = onOpenGuide,
                    onDismissDiscoveries = discoveryViewModel::dismiss,
                    onOpenSound = { soundSheet = true },
                    onSignIn = { viewModel.signIn(context) },
                    onSignOut = viewModel::signOut,
                    // Modo ambiente al instante (Pro); si no es Pro, lleva a la pantalla Pro
                    onEnterAmbient = { if (timer.isPro) ambient = true else onOpenPro() },
                ),
            )
        }
    }
}

data class MainActions(
    val onStart: () -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onDismissSummary: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenAchievements: () -> Unit = {},
    val onOpenPro: () -> Unit = {},
    val onOpenGuide: () -> Unit = {},
    val onOpenSound: () -> Unit = {},
    val onDismissDiscoveries: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onSignOut: () -> Unit = {},
    val onEnterAmbient: () -> Unit = {},
)

@Composable
private fun MainContent(
    timer: TimerUi?,
    account: MainUiState,
    actions: MainActions,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    dynamicColor: Boolean = false,
    discoveries: List<Discovery> = emptyList(),
) {
    // Con Material You el reloj toma el color del sistema
    val clockColor = if (dynamicColor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Espacio para el dock flotante
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // En Pro la pantalla queda limpia, sin título
                if (showTitle) {
                    Text(
                        text = "Lab Pomodoro",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Vessel(
                    vessel = (timer as? TimerUi.Active)?.vessel ?: VesselUi.Resting,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                when (timer) {
                    null -> CircularProgressIndicator()
                    is TimerUi.Idle -> IdlePanel(timer, clockColor)
                    is TimerUi.Active -> ActivePanel(timer, clockColor)
                    is TimerUi.Finished -> FinishedPanel(timer)
                }
                if (timer is TimerUi.Active) {
                    Spacer(Modifier.height(16.dp))
                    VesselShelf(timer.shelf, Modifier.fillMaxWidth())
                }
                account.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // Elementos ganados con el tiempo de enfoque; "Ver" lleva a la tabla periódica
            AnimatedVisibility(
                visible = discoveries.any { it.source != DiscoverySource.FUSION },
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                DiscoveryBanner(
                    discoveries = discoveries,
                    onOpen = actions.onOpenAchievements,
                    onDismiss = actions.onDismissDiscoveries,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LabDock(
                timer = timer,
                account = account,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/**
 * Barra flotante inferior: el botón principal siempre visible y el resto de opciones
 * desplegables con la flecha, para dejar la pantalla libre para el recipiente.
 */
@Composable
private fun LabDock(timer: TimerUi?, account: MainUiState, actions: MainActions, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    // Cada opción cierra el dock después de ejecutarse
    fun run(action: () -> Unit): () -> Unit = {
        expanded = false
        action()
    }

    // La burbuja contenedora solo aparece al desplegar; contraído, los botones flotan solos
    val bubbleColor by animateColorAsState(if (expanded) Color(0xFA141414) else Color.Transparent, label = "bubble")
    val borderColor by animateColorAsState(
        if (expanded) MaterialTheme.colorScheme.outline else Color.Transparent,
        label = "bubbleBorder",
    )
    val elevation by animateDpAsState(if (expanded) 8.dp else 0.dp, label = "bubbleShadow")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = bubbleColor,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = elevation,
    ) {
        Column(Modifier.padding(8.dp)) {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DockItem(R.drawable.ic_tune, "Config", run(actions.onOpenSettings))
                        DockItem(R.drawable.ic_star, "Logros", run(actions.onOpenAchievements))
                        DockItem(R.drawable.ic_diamond, "Pro", run(actions.onOpenPro))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        DockItem(R.drawable.ic_help, "Guía", run(actions.onOpenGuide))
                        if (timer is TimerUi.Active) DockItem(R.drawable.ic_moon, "Ambiente", run(actions.onEnterAmbient))
                        DockItem(R.drawable.ic_headphones, "Sonido", run(actions.onOpenSound))
                        when {
                            account.isBusy -> DockItem(R.drawable.ic_person, "…", onClick = {})
                            account.isSignedIn -> DockItem(R.drawable.ic_person, "Salir", run(actions.onSignOut))
                            else -> DockItem(R.drawable.ic_person, "Entrar", run(actions.onSignIn))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (timer is TimerUi.Active) {
                    // Controles tipo reproductor: detener · pausar/continuar · saltar
                    PlayerButton(R.drawable.ic_stop, "Detener", actions.onStop)
                    Spacer(Modifier.size(8.dp))
                    Button(
                        onClick = if (timer.isPaused) actions.onResume else actions.onPause,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Icon(
                            painterResource(if (timer.isPaused) R.drawable.ic_play else R.drawable.ic_pause),
                            contentDescription = if (timer.isPaused) "Continuar" else "Pausar",
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    PlayerButton(R.drawable.ic_skip, "Saltar", actions.onSkip)
                } else {
                    val (label, onClick) = when (timer) {
                        is TimerUi.Finished -> "Nuevo experimento" to actions.onDismissSummary
                        else -> "Start" to actions.onStart
                    }
                    Button(
                        onClick = onClick,
                        enabled = timer != null,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                    ) {
                        Text(label, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.size(8.dp))
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape),
                ) {
                    Icon(
                        painterResource(R.drawable.ic_chevron_up),
                        contentDescription = if (expanded) "Ocultar opciones" else "Más opciones",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.rotate(arrowRotation),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerButton(icon: Int, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = description, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DockItem(icon: Int, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ClockText(millis: Long, color: Color = MaterialTheme.colorScheme.onBackground) {
    Text(
        text = formatMinutesSeconds(millis),
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
        color = color,
    )
}

@Composable
private fun IdlePanel(idle: TimerUi.Idle, clockColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ClockText(idle.firstSessionMillis, clockColor)
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
private fun ActivePanel(active: TimerUi.Active, clockColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val element = active.vessel.element
            Text(
                text = if (element != null) "${active.type.label()} · ${element.name}" else active.type.label(),
                style = MaterialTheme.typography.titleLarge,
                color = active.vessel.liquid,
            )
            // El elemento que hay en el recipiente
            if (element != null) {
                Spacer(Modifier.width(8.dp))
                ElementTile(element, discovered = true, size = 26.dp)
            }
        }
        ClockText(active.remainingMillis, color = if (active.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else clockColor)
        if (active.isPaused) {
            Text("En pausa", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        ActiveIndicator(active)
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
fun ActiveIndicator(active: TimerUi.Active, modifier: Modifier = Modifier) {
    SessionIndicator(
        current = active.type,
        currentColor = active.vessel.liquid,
        next = active.upNext,
        dots = active.dots,
        description = active.description,
        modifier = modifier,
    )
}

@Composable
fun Vessel(vessel: VesselUi, modifier: Modifier = Modifier) {
    VesselView(
        shape = vessel.shape,
        fill = vessel.fill,
        liquidColor = vessel.liquid,
        bubbleColor = vessel.bubbles,
        effect = vessel.effect,
        animate = vessel.animate,
        modifier = modifier,
        behavior = vessel.behavior,
    )
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
    val shapes = VesselShape.entries
    LabPomodoroTheme {
        MainContent(
            timer = TimerUi.Active(
                type = SessionType.WORK,
                remainingMillis = 754_000,
                progress = 0.5f,
                sessionNumber = 3,
                totalSessions = 9,
                isPaused = false,
                next = SessionType.SHORT_BREAK,
                vessel = VesselUi(VesselShape.ERLENMEYER, 0.5f, LiquidPalette.liquid(SessionType.WORK, 1, 2), Color.Transparent, LiquidEffect.VAPOR, animate = false),
                shelf = List(9) { i ->
                    ShelfItemUi(shapes[i % shapes.size], LiquidPalette.liquid(SessionType.WORK, 1, i), if (i < 2) 1f else if (i == 2) 0.5f else 0f, i == 2, skipped = i == 1)
                },
                upNext = UpNext(SessionType.SHORT_BREAK, LiquidPalette.liquid(SessionType.SHORT_BREAK, 1, 3), 5),
                dots = List(5) { i ->
                    PlanDot(if (i < 1) DotState.DONE else if (i == 1) DotState.CURRENT else DotState.PENDING, LiquidPalette.liquid(SessionType.WORK, 1, i), cycleEnd = i == 3)
                },
            ),
            account = MainUiState(),
            actions = MainActions(),
        )
    }
}
