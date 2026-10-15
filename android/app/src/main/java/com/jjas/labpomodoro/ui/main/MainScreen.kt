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
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import androidx.compose.material3.FilledTonalButton
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.ui.settings.StepperRow
import com.jjas.labpomodoro.ui.components.ElementSignatures
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import com.jjas.labpomodoro.data.repository.Discovery
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.domain.model.Medal
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.service.canScheduleExactAlarms
import com.jjas.labpomodoro.service.formatMinutesSeconds
import com.jjas.labpomodoro.service.labelRes
import com.jjas.labpomodoro.service.requestExactAlarms
import com.jjas.labpomodoro.ui.components.Celebration
import com.jjas.labpomodoro.ui.components.ConfettiBurst
import com.jjas.labpomodoro.ui.components.BunsenBurner
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
import com.jjas.labpomodoro.ui.components.localizedName
import com.jjas.labpomodoro.ui.components.look
import com.jjas.labpomodoro.ui.medals.MedalBanner
import com.jjas.labpomodoro.ui.medals.MedalViewModel
import com.jjas.labpomodoro.ui.medals.colors
import com.jjas.labpomodoro.ui.promo.ShareText
import com.jjas.labpomodoro.ui.promo.shareText
import com.jjas.labpomodoro.ui.sound.FocusSoundPanel
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import com.jjas.labpomodoro.ui.theme.isLight
import kotlinx.coroutines.delay
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenPro: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenStats: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
    timerViewModel: TimerViewModel = hiltViewModel(),
    discoveryViewModel: DiscoveryViewModel = hiltViewModel(),
    medalViewModel: MedalViewModel = hiltViewModel(),
    tagViewModel: TagViewModel = hiltViewModel(),
) {
    val account by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveries by discoveryViewModel.unseen.collectAsStateWithLifecycle()
    val medals by medalViewModel.unseen.collectAsStateWithLifecycle()
    val timer by timerViewModel.state.collectAsStateWithLifecycle()
    val tags by tagViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tagActions = TagActions(
        onSelect = tagViewModel::select,
        onCreate = tagViewModel::create,
        onUpdate = tagViewModel::update,
        onDelete = tagViewModel::delete,
        onOpenPro = onOpenPro,
    )

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
    // Android 14+ niega las alarmas exactas al instalar: se pide una vez, al primer plan. Sin ellas
    // el timer funciona igual, pero con la pantalla apagada el fin de sesión puede llegar tarde
    var askExactAlarm by rememberSaveable { mutableStateOf(false) }
    val startPlan = {
        timerViewModel.start()
        if (timer.exactAlarmAsked == false && !context.canScheduleExactAlarms()) askExactAlarm = true
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { startPlan() }
    val onStart = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startPlan()
        }
    }
    if (askExactAlarm) {
        val close = {
            askExactAlarm = false
            timerViewModel.markExactAlarmAsked()
        }
        AlertDialog(
            onDismissRequest = close,
            confirmButton = {
                TextButton(onClick = {
                    close()
                    context.requestExactAlarms()
                }) { Text(stringResource(R.string.main_exact_alarm_allow)) }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.main_exact_alarm_later)) } },
            title = { Text(stringResource(R.string.main_exact_alarm_title)) },
            text = { Text(stringResource(R.string.main_exact_alarm_text)) },
        )
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

    // Teléfono nuevo con respaldo en la nube: se ofrece recuperar el progreso
    account.restoreOffer?.let { info ->
        RestoreOfferDialog(info, onRestore = viewModel::acceptRestore, onDismiss = viewModel::declineRestore)
    }

    // Hoja para elegir el sonido de concentración sin salir del timer
    // Hoja para ajustar el plan en curso (alargar, sumar o quitar pomodoros, terminar antes)
    var planSheet by rememberSaveable { mutableStateOf(false) }
    val adjustable = timer.timer as? TimerUi.Active
    if (planSheet && adjustable != null) {
        ModalBottomSheet(onDismissRequest = { planSheet = false }) {
            AdjustPlanSheet(
                active = adjustable,
                onExtend = timerViewModel::extend,
                onAdd = timerViewModel::addPomodoro,
                onRemove = timerViewModel::removeLastPomodoro,
                onFinishAfter = {
                    timerViewModel.finishAfterCurrent()
                    planSheet = false
                },
            )
        }
    } else if (planSheet) {
        planSheet = false
    }

    var soundSheet by rememberSaveable { mutableStateOf(false) }
    if (soundSheet) {
        ModalBottomSheet(onDismissRequest = { soundSheet = false }) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(stringResource(R.string.main_focus_sound_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                FocusSoundPanel(onOpenPro = {
                    soundSheet = false
                    onOpenPro()
                })
            }
        }
    }
    // Antes de iniciar: tocar el reloj ajusta este plan (duraciones y etiqueta)
    var planEditor by rememberSaveable { mutableStateOf(false) }
    val idle = timer.timer as? TimerUi.Idle
    if (planEditor && idle != null) {
        ModalBottomSheet(onDismissRequest = { planEditor = false }) {
            PlanEditorSheet(
                idle = idle,
                tags = tags,
                tagActions = tagActions,
                onEdit = timerViewModel::editPlan,
                onReset = timerViewModel::resetPlan,
                onSaveDefault = timerViewModel::savePlanAsDefault,
            )
        }
    } else if (planEditor) {
        planEditor = false
    }

    // Cambiar la etiqueta a medio plan
    var tagSheet by rememberSaveable { mutableStateOf(false) }
    if (tagSheet) {
        ModalBottomSheet(onDismissRequest = { tagSheet = false }) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(stringResource(R.string.tag_question), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.tag_change_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                TagChips(tags, tags.current, tagActions.copy(onSelect = { tagViewModel.select(it); tagSheet = false }))
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
            // El modo ambiente es siempre oscuro, aunque el teléfono use el tema claro
            LabPomodoroTheme(darkTheme = true, dynamicColor = timer.dynamicColor) {
                AmbientScreen(
                    active,
                    dynamicColor = timer.dynamicColor,
                    rain = timer.rainAmbience,
                    onWake = {
                        ambient = false
                        lastTouch = SystemClock.uptimeMillis()
                    },
                )
            }
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
                medals = medals,
                tags = tags,
                tagActions = tagActions,
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
                    onOpenStats = onOpenStats,
                    onDismissDiscoveries = discoveryViewModel::dismiss,
                    onDismissMedals = medalViewModel::dismiss,
                    onOpenSound = { soundSheet = true },
                    onOpenTags = { tagSheet = true },
                    onOpenPlanEditor = { planEditor = true },
                    onOpenPlan = { planSheet = true },
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
    val onOpenStats: () -> Unit = {},
    val onOpenSound: () -> Unit = {},
    val onOpenTags: () -> Unit = {},
    val onOpenPlanEditor: () -> Unit = {},
    val onOpenPlan: () -> Unit = {},
    val onDismissDiscoveries: () -> Unit = {},
    val onDismissMedals: () -> Unit = {},
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
    medals: List<Medal> = emptyList(),
    tags: TagsUi = TagsUi(),
    tagActions: TagActions = TagActions(),
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
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Vessel(
                    vessel = (timer as? TimerUi.Active)?.vessel ?: VesselUi.Resting,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    // Pro: vidrio con refracción (Android 13+)
                    refraction = !showTitle,
                    // Se inclina con el teléfono y hace ondas al tocarlo
                    interactive = true,
                )
                // Mechero bajo el recipiente: encendido mientras se trabaja (por eso se evapora)
                val active = timer as? TimerUi.Active
                BunsenBurner(
                    lit = active != null && active.type == SessionType.WORK && !active.isPaused,
                    modifier = Modifier
                        // La punta de la llama toca el fondo del recipiente
                        .offset(y = (-6).dp)
                        .size(width = 120.dp, height = 60.dp),
                )
                when (timer) {
                    null -> CircularProgressIndicator()
                    is TimerUi.Idle -> IdlePanel(timer, clockColor, tags, tagActions, actions.onOpenPlanEditor)
                    is TimerUi.Active -> ActivePanel(timer, clockColor, tags, actions.onOpenTags)
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

            // Elementos ganados: salen del recipiente en una burbuja; tocarla abre la tabla periódica
            AnimatedVisibility(
                visible = discoveries.any { it.source != DiscoverySource.FUSION },
                enter = fadeIn(),
                exit = fadeOut() + scaleOut(),
            ) {
                // Se conserva la última lista mientras se anima la salida
                var shown by remember { mutableStateOf(discoveries) }
                if (discoveries.any { it.source != DiscoverySource.FUSION }) shown = discoveries
                ElementBubble(
                    discoveries = shown,
                    onOpen = {
                        actions.onDismissDiscoveries()
                        actions.onOpenAchievements()
                    },
                    onDismiss = actions.onDismissDiscoveries,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Medallas nuevas; "Ver" lleva a Progreso, donde están todas
                AnimatedVisibility(
                    visible = medals.isNotEmpty(),
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                ) {
                    // Se conserva la última lista mientras se anima la salida
                    var shown by remember { mutableStateOf(medals) }
                    if (medals.isNotEmpty()) shown = medals
                    MedalBanner(
                        medals = shown,
                        onOpen = actions.onOpenStats,
                        onDismiss = actions.onDismissMedals,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Celebrations(timer, discoveries, medals)

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
 * Confeti en los momentos que valen la pena: un pomodoro completado (con el color de su
 * recipiente), el plan terminado y elementos nuevos (con los colores de los elementos).
 */
@Composable
private fun Celebrations(timer: TimerUi?, discoveries: List<Discovery>, medals: List<Medal>) {
    var celebration by remember { mutableStateOf<Celebration?>(null) }

    val active = timer as? TimerUi.Active
    // Se recuerda al girar la pantalla para no repetir la ráfaga
    var lastCompleted by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(active?.completedWork) {
        val now = active?.completedWork ?: return@LaunchedEffect
        val before = lastCompleted
        lastCompleted = now
        if (before != null && now > before) {
            val color = active.dots.lastOrNull { it.state == DotState.DONE }?.color ?: active.vessel.liquid
            celebration = Celebration(System.nanoTime(), listOf(color, lerp(color, Color.White, 0.5f)), originY = 0.35f)
        }
    }

    val finished = timer as? TimerUi.Finished
    var finishedCelebrated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(finished != null) {
        if (finished == null) {
            finishedCelebrated = false
        } else if (!finishedCelebrated && finished.workSessions > 0) {
            finishedCelebrated = true
            celebration = Celebration(System.nanoTime(), ConfettiPalette, big = true, originY = 0.45f)
        }
    }

    val rewards = discoveries.filter { it.source != DiscoverySource.FUSION }
    var lastRewards by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(rewards.size) {
        val before = lastRewards
        lastRewards = rewards.size
        if (before >= 0 && rewards.size > before) {
            celebration = Celebration(System.nanoTime(), rewards.map { it.element.look().color }, originY = 0.12f)
        }
    }

    var lastMedals by rememberSaveable { mutableIntStateOf(-1) }
    LaunchedEffect(medals.size) {
        val before = lastMedals
        lastMedals = medals.size
        if (before >= 0 && medals.size > before) {
            celebration = Celebration(System.nanoTime(), medals.flatMap { it.tier.colors() }, originY = 0.2f)
        }
    }

    ConfettiBurst(celebration, Modifier.fillMaxSize())
}

private val ConfettiPalette = listOf(
    Color(0xFF39FF14), Color(0xFFFFC107), Color(0xFF29B6F6), Color(0xFFF06292), Color(0xFFB388FF), Color(0xFFFF7043),
)

/**
 * Barra flotante inferior: el botón principal siempre visible y el resto de opciones
 * desplegables con la flecha, para dejar la pantalla libre para el recipiente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabDock(timer: TimerUi?, account: MainUiState, actions: MainActions, modifier: Modifier = Modifier) {
    // Todas las opciones viven en una hoja inferior; abajo quedan siempre los controles del timer
    var menu by rememberSaveable { mutableStateOf(false) }
    if (menu) {
        // Completa desde el inicio: la lista es larga
        ModalBottomSheet(onDismissRequest = { menu = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            LabMenu(timer, account, actions, close = { menu = false })
        }
    }

    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (timer is TimerUi.Active) {
            // Controles tipo reproductor: detener · pausar/continuar · saltar
            PlayerButton(R.drawable.ic_stop, stringResource(R.string.main_action_stop), actions.onStop)
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
                    contentDescription = stringResource(if (timer.isPaused) R.string.main_action_resume else R.string.main_action_pause),
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            PlayerButton(R.drawable.ic_skip, stringResource(R.string.main_action_skip), actions.onSkip)
        } else {
            val (label, onClick) = when (timer) {
                is TimerUi.Finished -> stringResource(R.string.main_new_experiment) to actions.onDismissSummary
                else -> stringResource(R.string.main_start_button) to actions.onStart
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
            onClick = { menu = true },
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_menu),
                contentDescription = stringResource(R.string.main_more_options),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** El menú completo, como lista de Android: lo del plan en curso arriba y luego la app. */
@Composable
private fun LabMenu(timer: TimerUi?, account: MainUiState, actions: MainActions, close: () -> Unit) {
    // Cada opción cierra la hoja antes de abrir lo suyo
    fun run(action: () -> Unit): () -> Unit = {
        close()
        action()
    }
    Column(
        Modifier
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        if (timer is TimerUi.Active) {
            MenuSection(stringResource(R.string.main_menu_section_plan))
            MenuItem(R.drawable.ic_more_time, stringResource(R.string.main_dock_plan), stringResource(R.string.main_menu_plan_desc), run(actions.onOpenPlan))
            MenuItem(R.drawable.ic_moon, stringResource(R.string.main_dock_ambient), stringResource(R.string.main_menu_ambient_desc), run(actions.onEnterAmbient))
            MenuItem(R.drawable.ic_headphones, stringResource(R.string.main_dock_sound), stringResource(R.string.main_menu_sound_desc), run(actions.onOpenSound))
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
        }
        MenuSection(stringResource(R.string.app_name))
        MenuItem(R.drawable.ic_star, stringResource(R.string.main_dock_achievements), stringResource(R.string.main_menu_achievements_desc), run(actions.onOpenAchievements))
        MenuItem(R.drawable.ic_chart, stringResource(R.string.main_dock_progress), stringResource(R.string.main_menu_progress_desc), run(actions.onOpenStats))
        if (timer !is TimerUi.Active) {
            MenuItem(R.drawable.ic_headphones, stringResource(R.string.main_dock_sound), stringResource(R.string.main_menu_sound_desc), run(actions.onOpenSound))
        }
        MenuItem(R.drawable.ic_tune, stringResource(R.string.main_dock_settings), stringResource(R.string.main_menu_settings_desc), run(actions.onOpenSettings))
        MenuItem(R.drawable.ic_help, stringResource(R.string.main_dock_guide), stringResource(R.string.main_menu_guide_desc), run(actions.onOpenGuide))
        MenuItem(R.drawable.ic_diamond, stringResource(R.string.main_dock_pro), stringResource(R.string.main_menu_pro_desc), run(actions.onOpenPro))
        when {
            account.isBusy -> MenuItem(R.drawable.ic_person, "…", null) {}
            account.isSignedIn -> MenuItem(R.drawable.ic_person, stringResource(R.string.main_dock_sign_out), stringResource(R.string.main_menu_sign_out_desc), run(actions.onSignOut))
            else -> MenuItem(R.drawable.ic_person, stringResource(R.string.main_dock_sign_in), stringResource(R.string.main_menu_sign_in_desc), run(actions.onSignIn))
        }
    }
}

@Composable
private fun MenuSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun MenuItem(icon: Int, title: String, description: String?, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = description?.let { { Text(it) } },
        leadingContent = { Icon(painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    )
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
private fun ClockText(millis: Long, color: Color = MaterialTheme.colorScheme.onBackground) {
    Text(
        text = formatMinutesSeconds(millis),
        style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
        color = color,
    )
}

@Composable
private fun IdlePanel(
    idle: TimerUi.Idle,
    clockColor: Color,
    tags: TagsUi = TagsUi(),
    tagActions: TagActions = TagActions(),
    onEditPlan: () -> Unit = {},
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // La hora se toma al mostrar la pantalla; no hace falta que cambie mientras está abierta
        val resources = LocalResources.current
        val greeting = remember { Encouragement.greeting(resources, LocalTime.now().hour) }
        Text(
            greeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Reloj y resumen se tocan para ajustar este plan
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .clickable(onClickLabel = stringResource(R.string.main_edit_plan), onClick = onEditPlan)
                .padding(horizontal = 16.dp),
        ) {
            ClockText(idle.firstSessionMillis, clockColor)
            Text(
                text = pluralStringResource(R.plurals.main_plan_summary, idle.pomodoros, idle.pomodoros, formatDuration(idle.workMinutes)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.main_with_breaks, formatDuration(idle.totalMinutes)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(if (idle.customized) R.string.main_plan_customized else R.string.main_tap_to_edit),
                style = MaterialTheme.typography.labelMedium,
                color = if (idle.customized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.tag_question),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        TagChips(tags, tags.selected, tagActions)
    }
}

@Composable
private fun ActivePanel(active: TimerUi.Active, clockColor: Color, tags: TagsUi = TagsUi(), onOpenTags: () -> Unit = {}) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val element = active.vessel.element
            val typeLabel = stringResource(active.type.labelRes())
            Text(
                text = if (element != null) stringResource(R.string.main_label_with_element, typeLabel, element.localizedName()) else typeLabel,
                style = MaterialTheme.typography.titleLarge,
                // Sobre el tema claro, el color del líquido se oscurece para que se lea (el amarillo casi no se ve)
                color = if (MaterialTheme.colorScheme.isLight) lerp(active.vessel.liquid, Color.Black, 0.4f) else active.vessel.liquid,
            )
            // El elemento que hay en el recipiente
            if (element != null) {
                Spacer(Modifier.width(8.dp))
                ElementTile(element, discovered = true, size = 26.dp)
            }
        }
        ClockText(active.remainingMillis, color = if (active.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else clockColor)
        if (active.isPaused) {
            Text(stringResource(R.string.main_paused), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        ActiveIndicator(active)
        // Solo si ya usa etiquetas: quien no las usa no ve nada nuevo mientras trabaja
        if (tags.tags.isNotEmpty()) CurrentTag(tags.find(tags.current), onOpenTags)
    }
}

@Composable
private fun FinishedPanel(finished: TimerUi.Finished) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.main_experiment_complete),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = pluralStringResource(
                R.plurals.main_plan_summary,
                finished.workSessions,
                finished.workSessions,
                formatDuration(finished.workMinutes.toInt()),
            ),
            style = MaterialTheme.typography.bodyLarge,
        )
        // Mensaje de ánimo; se elige una vez y no cambia al girar la pantalla
        val seed = rememberSaveable { System.nanoTime() }
        val resources = LocalResources.current
        Spacer(Modifier.height(12.dp))
        Text(
            text = Encouragement.finished(resources, finished.workSessions, seed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (finished.workSessions > 0) {
            val context = LocalContext.current
            TextButton(onClick = {
                context.shareText(ShareText.finished(context, finished.workSessions, formatDuration(finished.workMinutes.toInt())))
            }) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.main_share))
            }
        }
    }
}

@Composable
fun ActiveIndicator(active: TimerUi.Active, modifier: Modifier = Modifier) {
    SessionIndicator(
        current = active.type,
        currentColor = active.vessel.liquid,
        next = active.upNext,
        dots = active.dots,
        // Para lectores de pantalla: lo que el indicador simbólico dice con íconos
        description = active.next?.let {
            stringResource(
                R.string.main_session_description_next,
                active.sessionNumber,
                active.totalSessions,
                stringResource(it.labelRes()).lowercase(),
            )
        } ?: stringResource(R.string.main_session_description_last, active.sessionNumber, active.totalSessions),
        modifier = modifier,
    )
}

@Composable
fun Vessel(vessel: VesselUi, modifier: Modifier = Modifier, refraction: Boolean = false, interactive: Boolean = false) {
    // En el último tramo de un pomodoro con premio, el líquido muta hacia el elemento que se va a
    // ganar; si son varios, pasa de uno a otro
    val targets = vessel.morphTo
    val morphing = targets.isNotEmpty() && vessel.morph > 0f
    var liquid = vessel.liquid
    var behavior = vessel.behavior
    var tones = vessel.tones
    if (morphing) {
        val cycle = if (targets.size > 1) {
            rememberInfiniteTransition(label = "muta").animateFloat(
                0f,
                targets.size.toFloat(),
                infiniteRepeatable(tween(MORPH_SLOT_MS * targets.size, easing = LinearEasing)),
                label = "ciclo",
            ).value
        } else {
            0f
        }
        val slot = cycle.toInt() % targets.size
        val current = targets[slot]
        // Se queda en cada elemento y se funde con el siguiente al final de su turno
        val crossfade = ((cycle - cycle.toInt() - 0.7f) / 0.3f).coerceIn(0f, 1f)
        val target = lerp(current.look().color, targets[(slot + 1) % targets.size].look().color, crossfade)
        liquid = lerp(vessel.liquid, target, vessel.morph)
        // Pasada la mitad ya se comporta como el premio (metal fundido, brillo radiactivo…)
        if (vessel.morph > 0.6f) behavior = current.look().behavior
        tones = if (vessel.morph >= 0.99f && targets.size == 1) ElementSignatures.tones(current.atomicNumber) else null
    }
    VesselView(
        shape = vessel.shape,
        fill = vessel.fill,
        liquidColor = liquid,
        bubbleColor = vessel.bubbles,
        effect = vessel.effect,
        animate = vessel.animate,
        modifier = modifier,
        behavior = behavior,
        refraction = refraction,
        interactive = interactive,
        tones = tones,
    )
}

/** Cuánto se queda el líquido en cada premio cuando son varios. */
private const val MORPH_SLOT_MS = 1_600

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

/** Hoja "Este plan": duraciones y etiqueta del próximo plan, sin tocar Configuración. */
@Composable
private fun PlanEditorSheet(
    idle: TimerUi.Idle,
    tags: TagsUi,
    tagActions: TagActions,
    onEdit: ((SessionConfig) -> SessionConfig) -> Unit,
    onReset: () -> Unit,
    onSaveDefault: () -> Unit,
) {
    val config = idle.config
    Column(
        Modifier
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(stringResource(R.string.main_plan_editor_title), style = MaterialTheme.typography.titleLarge)
        Text(
            pluralStringResource(R.plurals.main_plan_summary, idle.pomodoros, idle.pomodoros, formatDuration(idle.workMinutes)) +
                " · " + stringResource(R.string.main_with_breaks, formatDuration(idle.totalMinutes)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        StepperRow(stringResource(R.string.set_total_hours), config.totalHours, SessionConfig.TOTAL_HOURS, "h") { v -> onEdit { it.copy(totalHours = v) } }
        StepperRow(stringResource(R.string.set_pomodoro), config.workMinutes, SessionConfig.WORK_MINUTES, "min", step = 5) { v -> onEdit { it.copy(workMinutes = v) } }
        StepperRow(stringResource(R.string.set_short_break), config.shortBreakMinutes, SessionConfig.SHORT_BREAK_MINUTES, "min") { v -> onEdit { it.copy(shortBreakMinutes = v) } }
        StepperRow(stringResource(R.string.set_long_break), config.longBreakMinutes, SessionConfig.LONG_BREAK_MINUTES, "min", step = 5) { v -> onEdit { it.copy(longBreakMinutes = v) } }
        StepperRow(stringResource(R.string.set_pomodoros_until_long), config.pomodorosUntilLong, SessionConfig.POMODOROS_UNTIL_LONG, "") { v -> onEdit { it.copy(pomodorosUntilLong = v) } }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.tag_question), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        TagChips(tags, tags.selected, tagActions)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.main_plan_editor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (idle.customized) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                TextButton(onClick = onReset) { Text(stringResource(R.string.main_plan_reset)) }
                FilledTonalButton(onClick = onSaveDefault) { Text(stringResource(R.string.main_plan_save_default)) }
            }
        }
    }
}
