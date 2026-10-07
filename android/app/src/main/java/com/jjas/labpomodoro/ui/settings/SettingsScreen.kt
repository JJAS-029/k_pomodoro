package com.jjas.labpomodoro.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.BuildConfig
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.ads.AdBanner
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.service.requestAddTimerTile
import com.jjas.labpomodoro.ui.components.localizedName
import com.jjas.labpomodoro.ui.promo.GITHUB_URL
import com.jjas.labpomodoro.ui.promo.Podcast
import com.jjas.labpomodoro.ui.promo.SUGGESTIONS_EMAIL
import com.jjas.labpomodoro.ui.promo.ShareText
import com.jjas.labpomodoro.ui.promo.openUrl
import com.jjas.labpomodoro.ui.promo.sendSuggestion
import com.jjas.labpomodoro.ui.promo.shareText
import com.jjas.labpomodoro.ui.sound.FocusSoundPanel
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme
import com.jjas.labpomodoro.widget.requestPinTimerWidget

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPro: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val exportMessage by viewModel.message.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val exportFileName = stringResource(R.string.set_export_file_name)
    // El usuario elige dónde guardar el archivo (Descargas, Drive…)
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsv)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val current = settings
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column {
                SettingsContent(
                    modifier = Modifier.weight(1f),
                    settings = current,
                    onBack = onBack,
                    onSessionChange = viewModel::updateSession,
                    onSoundChange = viewModel::setSoundEnabled,
                    onVibrationChange = viewModel::setVibrationEnabled,
                    onKeepScreenOnChange = viewModel::setKeepScreenOn,
                    onAmbientChange = viewModel::setAmbientMode,
                    onAmbientDelayChange = viewModel::setAmbientDelayMinutes,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onResetVesselElement = viewModel::resetVesselElement,
                    onReminderChange = viewModel::setReminder,
                    onReminderHourChange = viewModel::setReminderHour,
                    soundSection = { FocusSoundPanel(onOpenPro = onOpenPro) },
                    backupSection = { CloudBackupSection() },
                    dataSection = {
                        DataSection(
                            isPro = current.isPro,
                            message = exportMessage,
                            onExport = { exportLauncher.launch(exportFileName) },
                            onOpenPro = onOpenPro,
                            showPrivacy = !current.isPro && viewModel.privacyOptionsRequired,
                            onPrivacy = { activity?.let(viewModel::showPrivacyOptions) },
                        )
                    },
                )
                // Solo en la versión gratis; no tapa nada porque va debajo del contenido
                AdBanner(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun DataSection(
    isPro: Boolean,
    message: String?,
    onExport: () -> Unit,
    onOpenPro: () -> Unit,
    showPrivacy: Boolean,
    onPrivacy: () -> Unit,
) {
    Text(
        stringResource(R.string.set_export_desc),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    if (isPro) {
        OutlinedButton(onClick = onExport) { Text(stringResource(R.string.set_export_csv)) }
    } else {
        FilledTonalButton(onClick = onOpenPro) { Text(stringResource(R.string.set_export_pro)) }
    }
    message?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
    if (showPrivacy) {
        TextButton(onClick = onPrivacy) { Text(stringResource(R.string.set_ads_privacy)) }
    }
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onBack: () -> Unit,
    onSessionChange: ((SessionConfig) -> SessionConfig) -> Unit,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onAmbientChange: (Boolean) -> Unit,
    onAmbientDelayChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onResetVesselElement: () -> Unit = {},
    onReminderChange: (Boolean) -> Unit = {},
    onReminderHourChange: (Int) -> Unit = {},
    soundSection: @Composable () -> Unit = {},
    dataSection: @Composable () -> Unit = {},
    backupSection: @Composable () -> Unit = {},
) {
    val session = settings.session
    Column(
        modifier = modifier
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.set_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onBack) { Text(stringResource(R.string.set_done)) }
        }

        LanguageSection()

        SectionTitle(stringResource(R.string.set_section_plan))
        StepperRow(stringResource(R.string.set_total_hours), session.totalHours, SessionConfig.TOTAL_HOURS, "h") { v ->
            onSessionChange { it.copy(totalHours = v) }
        }
        StepperRow(stringResource(R.string.set_pomodoro), session.workMinutes, SessionConfig.WORK_MINUTES, "min", step = 5) { v ->
            onSessionChange { it.copy(workMinutes = v) }
        }
        StepperRow(stringResource(R.string.set_short_break), session.shortBreakMinutes, SessionConfig.SHORT_BREAK_MINUTES, "min") { v ->
            onSessionChange { it.copy(shortBreakMinutes = v) }
        }
        StepperRow(stringResource(R.string.set_long_break), session.longBreakMinutes, SessionConfig.LONG_BREAK_MINUTES, "min", step = 5) { v ->
            onSessionChange { it.copy(longBreakMinutes = v) }
        }
        StepperRow(stringResource(R.string.set_pomodoros_until_long), session.pomodorosUntilLong, SessionConfig.POMODOROS_UNTIL_LONG, "") { v ->
            onSessionChange { it.copy(pomodorosUntilLong = v) }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.set_rounding_title), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        val options = listOf(
            PlanRounding.TRIM_LAST to stringResource(R.string.set_rounding_trim),
            PlanRounding.WHOLE_POMODOROS to stringResource(R.string.set_rounding_whole),
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (mode, label) ->
                SegmentedButton(
                    selected = session.rounding == mode,
                    onClick = { onSessionChange { it.copy(rounding = mode) } },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) { Text(label) }
            }
        }
        Text(
            text = when (session.rounding) {
                PlanRounding.TRIM_LAST -> stringResource(R.string.set_rounding_trim_desc)
                PlanRounding.WHOLE_POMODOROS -> stringResource(R.string.set_rounding_whole_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionTitle(stringResource(R.string.set_section_alerts))
        SwitchRow(stringResource(R.string.set_sounds), settings.soundEnabled, onSoundChange)
        SwitchRow(stringResource(R.string.set_vibration), settings.vibrationEnabled, onVibrationChange)
        SwitchRow(stringResource(R.string.set_keep_screen_on), settings.keepScreenOn, onKeepScreenOnChange)
        SwitchRow(stringResource(R.string.set_streak_reminder), settings.reminderEnabled, onReminderChange)
        Text(
            stringResource(R.string.set_streak_reminder_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.reminderEnabled) {
            Spacer(Modifier.height(8.dp))
            val hours = AppSettings.REMINDER_HOURS
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                hours.forEachIndexed { index, hour ->
                    SegmentedButton(
                        selected = settings.reminderHour == hour,
                        onClick = { onReminderHourChange(hour) },
                        shape = SegmentedButtonDefaults.itemShape(index, hours.size),
                    ) { Text("$hour:00") }
                }
            }
        }

        SectionTitle(proTitle(R.string.set_section_ambient, settings.isPro))
        SwitchRow(
            label = stringResource(R.string.set_ambient_switch),
            checked = settings.ambientActive,
            onCheckedChange = onAmbientChange,
            enabled = settings.isPro,
        )
        Text(
            text = stringResource(if (settings.isPro) R.string.set_ambient_desc else R.string.set_available_in_pro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (settings.ambientActive) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.set_ambient_after), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            val delays = AppSettings.AMBIENT_DELAYS
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                delays.forEachIndexed { index, minutes ->
                    SegmentedButton(
                        selected = settings.ambientDelayMinutes == minutes,
                        onClick = { onAmbientDelayChange(minutes) },
                        shape = SegmentedButtonDefaults.itemShape(index, delays.size),
                    ) { Text("$minutes min") }
                }
            }
        }

        SectionTitle(stringResource(R.string.set_section_shortcuts))
        val context = LocalContext.current
        Text(
            stringResource(R.string.set_shortcuts_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = { context.requestPinTimerWidget() }) { Text(stringResource(R.string.set_add_home_screen)) }
        // Android 13+ permite agregar el botón de los ajustes rápidos desde la app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OutlinedButton(onClick = { context.requestAddTimerTile() }) { Text(stringResource(R.string.set_add_quick_settings)) }
        }

        SectionTitle(stringResource(R.string.set_section_vessels))
        val fixed = settings.vesselElement.takeIf { it in 1..PeriodicTable.SIZE }?.let { PeriodicTable[it] }
        Text(
            text = if (fixed == null) {
                stringResource(R.string.set_vessels_varied)
            } else {
                val locale = LocalConfiguration.current.locales[0]
                stringResource(R.string.set_vessels_fixed, fixed.localizedName().lowercase(locale))
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        if (fixed != null) {
            TextButton(onClick = onResetVesselElement) { Text(stringResource(R.string.set_vessels_reset)) }
        } else {
            Text(
                stringResource(R.string.set_vessels_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Material You existe desde Android 12
        val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        SectionTitle(proTitle(R.string.set_section_appearance, settings.isPro))
        SwitchRow(
            label = stringResource(R.string.set_dynamic_color),
            checked = settings.dynamicColorActive,
            onCheckedChange = onDynamicColorChange,
            enabled = settings.isPro && dynamicAvailable,
        )
        Text(
            text = when {
                !dynamicAvailable -> stringResource(R.string.set_requires_android_12)
                !settings.isPro -> stringResource(R.string.set_available_in_pro)
                else -> stringResource(R.string.set_dynamic_color_desc)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(proTitle(R.string.set_section_focus_sound, settings.isPro))
        soundSection()

        SectionTitle(stringResource(R.string.set_section_backup))
        backupSection()

        SectionTitle(proTitle(R.string.set_section_data, settings.isPro))
        dataSection()

        SectionTitle(stringResource(R.string.set_section_about))
        AboutSection()
        Spacer(Modifier.height(24.dp))
    }
}

/** Título de sección con " · Pro" si el usuario aún no es Pro. */
@Composable
private fun proTitle(@StringRes title: Int, isPro: Boolean): String =
    if (isPro) stringResource(title) else stringResource(R.string.set_pro_suffix, stringResource(title))

/**
 * Idioma de la app. Desde Android 13 se elige por app en los ajustes del sistema; antes, la app
 * sigue el idioma del teléfono y la fila no se muestra.
 */
@Composable
private fun LanguageSection() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    SectionTitle(stringResource(R.string.set_section_general))
    Column(
        Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            }
            .padding(vertical = 6.dp),
    ) {
        Text(stringResource(R.string.set_language), style = MaterialTheme.typography.bodyLarge)
        Text(
            locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(24.dp))
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
    HorizontalDivider(Modifier.padding(top = 4.dp, bottom = 8.dp), color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    range: IntRange,
    unit: String,
    step: Int = 1,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedIconButton(
            onClick = { onValueChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
        ) { Text("−") }
        Text(
            text = if (unit.isEmpty()) "$value" else "$value $unit",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MaterialTheme.typography.displaySmall.fontFamily),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        OutlinedIconButton(
            onClick = { onValueChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
        ) { Text("+") }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SettingsContentPreview() {
    LabPomodoroTheme {
        SettingsContent(AppSettings(), {}, {}, {}, {}, {}, {}, {}, {})
    }
}

/** Créditos: el creador, sus podcasts y su GitHub. Se ve también en Pro. */
@Composable
private fun AboutSection() {
    val context = LocalContext.current
    Text(
        stringResource(R.string.set_about_credits, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Podcast.entries.forEach { podcast ->
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { context.openUrl(podcast.spotifyUrl) }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painterResource(podcast.cover),
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(podcast.title, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(podcast.taglineRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(stringResource(R.string.set_listen), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.set_suggest_prompt),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = { context.sendSuggestion(BuildConfig.VERSION_NAME) }) { Text(stringResource(R.string.set_send_suggestions)) }
    Text(SUGGESTIONS_EMAIL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FilledTonalButton(onClick = { context.shareText(ShareText.invite(context)) }) { Text(stringResource(R.string.set_recommend)) }
    TextButton(onClick = { context.openUrl(GITHUB_URL) }) { Text(stringResource(R.string.set_github)) }
}
