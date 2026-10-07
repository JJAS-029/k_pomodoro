package com.jjas.labpomodoro.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import com.jjas.labpomodoro.MainActivity
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.InventoryItem
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.AppSettings
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.service.TimerService
import com.jjas.labpomodoro.service.formatMinutesSeconds
import com.jjas.labpomodoro.service.labelRes
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.VesselGeometry
import com.jjas.labpomodoro.ui.components.VesselReagents
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.localizedName
import com.jjas.labpomodoro.ui.components.look
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** El widget de la pantalla de inicio. Solo avisa a [TimerWidgetSync]; él dibuja. */
@AndroidEntryPoint
class TimerWidgetProvider : AppWidgetProvider() {

    @Inject lateinit var sync: TimerWidgetSync

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = sync.refreshNow()
}

/** "Iniciar" desde el widget: arranca el plan sin abrir la app. */
@AndroidEntryPoint
class WidgetActionReceiver : BroadcastReceiver() {

    @Inject lateinit var engine: TimerEngine

    @Inject lateinit var settings: SettingsRepository

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_START) return
        val pending = goAsync()
        scope.launch {
            try {
                // Tocar un widget permite arrancar el servicio en primer plano desde segundo plano
                engine.start(settings.settings.first().session)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_START = "com.jjas.labpomodoro.action.WIDGET_START"
    }
}

/**
 * Mantiene el widget al día: cambia con el estado del timer y, mientras corre, redibuja el
 * recipiente cada 30 s (el tiempo lo lleva solo el cronómetro del sistema). No hace nada si no
 * hay widgets en la pantalla de inicio.
 */
@Singleton
class TimerWidgetSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: TimerEngine,
    private val settings: SettingsRepository,
    private val inventory: InventoryRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val manager = AppWidgetManager.getInstance(context)
    private val component = ComponentName(context, TimerWidgetProvider::class.java)

    fun start() {
        scope.launch {
            combine(engine.state, settings.settings, inventory.items, ::Triple).collectLatest { (state, prefs, items) ->
                while (true) {
                    // Al cruzar el segundo, para que el cronómetro del widget vaya parejo con la app
                    if (state is TimerState.Active && !state.isPaused) delay(state.millisToNextSecond(SystemClock.elapsedRealtime()))
                    push(state, prefs, items)
                    if (state !is TimerState.Active || state.isPaused) break
                    delay(REFRESH_MILLIS)
                }
            }
        }
    }

    fun refreshNow() {
        scope.launch { push(engine.state.value, settings.settings.first(), inventory.items.first()) }
    }

    private fun push(state: TimerState, prefs: AppSettings, items: List<InventoryItem>) {
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        manager.updateAppWidget(ids, render(state, prefs, items))
    }

    private fun render(state: TimerState, prefs: AppSettings, items: List<InventoryItem>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_timer)
        views.setOnClickPendingIntent(R.id.widget_root, openApp())
        when (state) {
            is TimerState.Active -> active(views, state, prefs, items)
            is TimerState.Finished -> {
                views.setTextViewText(R.id.widget_label, context.getString(R.string.main_experiment_complete))
                staticTime(views, "✓")
                views.setTextViewText(R.id.widget_next, pomodorosText(state.completedWorkSessions, (state.completedWorkSeconds / 60).toInt()))
                views.setImageViewBitmap(R.id.widget_vessel, vessel(VesselShape.BEAKER, 0.8f, RestingColor))
                startButton(views)
            }
            TimerState.Idle -> {
                val plan = SessionPlanGenerator.generate(prefs.session)
                views.setTextViewText(R.id.widget_label, context.getString(R.string.app_name))
                staticTime(views, formatMinutesSeconds(plan.first().durationSeconds * 1000L))
                val pomodoros = plan.count { it.type == SessionType.WORK }
                val workMinutes = plan.filter { it.type == SessionType.WORK }.sumOf { it.durationSeconds } / 60
                views.setTextViewText(R.id.widget_next, pomodorosText(pomodoros, workMinutes))
                views.setImageViewBitmap(R.id.widget_vessel, vessel(VesselShape.BEAKER, 0.8f, RestingColor))
                startButton(views)
            }
        }
        // Pro: colores de Material You, igual que la app
        if (prefs.dynamicColorActive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicColors(views)
        return views
    }

    /** Fondo, borde, botones y textos con los tonos del fondo de pantalla (Android 12+). */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamicColors(views: RemoteViews) {
        views.setInt(R.id.widget_root, "setBackgroundResource", R.drawable.widget_background_dynamic)
        listOf(R.id.widget_primary, R.id.widget_skip, R.id.widget_stop).forEach {
            views.setInt(it, "setBackgroundResource", R.drawable.widget_button_dynamic)
        }
        val clock = context.getColor(android.R.color.system_accent1_100)
        views.setTextColor(R.id.widget_chrono, clock)
        views.setTextColor(R.id.widget_time, clock)
        views.setTextColor(R.id.widget_label, context.getColor(android.R.color.system_accent2_200))
        views.setTextColor(R.id.widget_next, context.getColor(android.R.color.system_neutral2_300))
    }

    private fun active(views: RemoteViews, state: TimerState.Active, prefs: AppSettings, items: List<InventoryItem>) {
        val remaining = state.remainingMillis(SystemClock.elapsedRealtime())
        val type = state.current.type
        val element = VesselReagents.reagent(
            state.plan, state.index, state.planSeed, VesselReagents.pool(items, state.planSeed), prefs.vesselElement,
        )
        views.setTextViewText(
            R.id.widget_label,
            buildString {
                append(context.getString(type.labelRes()))
                element?.let { append(" · ${it.localizedName(context)}") }
                if (state.isPaused) append(" · ${context.getString(R.string.main_paused)}")
            },
        )
        if (state.isPaused) {
            staticTime(views, formatMinutesSeconds(remaining))
        } else {
            views.setViewVisibility(R.id.widget_chrono, View.VISIBLE)
            views.setViewVisibility(R.id.widget_time, View.GONE)
            // El cronómetro del sistema redondea hacia abajo y la app hacia arriba: con 1 s de más y
            // actualizando justo al cruzar el segundo (TimerWidgetSync), marcan exactamente lo mismo
            views.setChronometer(R.id.widget_chrono, SystemClock.elapsedRealtime() + remaining + ROUND_UP_MILLIS, null, true)
            views.setChronometerCountDown(R.id.widget_chrono, true)
        }
        views.setTextViewText(
            R.id.widget_next,
            // Corto para que quepa en widgets angostos
            state.next?.let { context.getString(R.string.main_widget_next, context.getString(it.type.labelRes()).lowercase(), it.durationSeconds / 60) }
                ?: context.getString(R.string.main_last_session),
        )
        val progress = (1f - remaining / (state.current.durationSeconds * 1000f)).coerceIn(0f, 1f)
        val color = element?.look()?.color ?: LiquidPalette.liquid(type, state.planSeed, state.index)
        views.setImageViewBitmap(
            R.id.widget_vessel,
            vessel(LiquidPalette.vessel(state.planSeed, state.index), if (type == SessionType.WORK) 1f - progress else progress, color),
        )
        val (icon, action, description) = if (state.isPaused) {
            Triple(R.drawable.ic_play, TimerService.ACTION_RESUME, context.getString(R.string.main_action_resume))
        } else {
            Triple(R.drawable.ic_pause, TimerService.ACTION_PAUSE, context.getString(R.string.main_action_pause))
        }
        views.setImageViewResource(R.id.widget_primary, icon)
        views.setContentDescription(R.id.widget_primary, description)
        views.setOnClickPendingIntent(R.id.widget_primary, TimerService.actionIntent(context, action))
        views.setViewVisibility(R.id.widget_skip, View.VISIBLE)
        views.setViewVisibility(R.id.widget_stop, View.VISIBLE)
        views.setOnClickPendingIntent(R.id.widget_skip, TimerService.actionIntent(context, TimerService.ACTION_SKIP))
        views.setOnClickPendingIntent(R.id.widget_stop, TimerService.actionIntent(context, TimerService.ACTION_STOP))
    }

    /** "3 pomodoros · 1 h 15 min". */
    private fun pomodorosText(pomodoros: Int, minutes: Int): String =
        context.resources.getQuantityString(R.plurals.main_pomodoros_with_time, pomodoros, pomodoros, minutesText(minutes))

    /** "25 min", "2 h", "1 h 30 min". */
    private fun minutesText(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${minutes % 60} min"
    }

    private fun staticTime(views: RemoteViews, text: String) {
        views.setViewVisibility(R.id.widget_chrono, View.GONE)
        views.setViewVisibility(R.id.widget_time, View.VISIBLE)
        views.setTextViewText(R.id.widget_time, text)
    }

    private fun startButton(views: RemoteViews) {
        views.setImageViewResource(R.id.widget_primary, R.drawable.ic_play)
        views.setContentDescription(R.id.widget_primary, context.getString(R.string.main_action_start))
        val start = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, WidgetActionReceiver::class.java).setAction(WidgetActionReceiver.ACTION_START),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_primary, start)
        views.setViewVisibility(R.id.widget_skip, View.GONE)
        views.setViewVisibility(R.id.widget_stop, View.GONE)
    }

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** El recipiente dibujado como imagen: el widget no puede usar Compose. */
    private fun vessel(shape: VesselShape, fill: Float, liquid: Color): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (VESSEL_WIDTH_DP * density).toInt()
        val height = (VESSEL_HEIGHT_DP * density).toInt()
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val g = VesselGeometry.fit(shape, width.toFloat(), height.toFloat(), headroom = 0.06f, strokeWidth = 2.5f * density)
        if (fill > 0.01f) {
            canvas.withClip(g.interior.asAndroidPath()) {
                val surface = g.surfaceY(fill)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        0f, surface, 0f, g.bottom,
                        lerp(liquid, Color.White, 0.18f).toArgb(),
                        lerp(liquid, Color.Black, 0.35f).toArgb(),
                        Shader.TileMode.CLAMP,
                    )
                }
                drawRect(g.left, surface, g.right, g.bottom, paint)
            }
        }
        val glass = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = g.stroke
            strokeCap = Paint.Cap.ROUND
            color = GlassColor
        }
        canvas.drawPath(g.outline.asAndroidPath(), glass)
        return bitmap
    }

    private companion object {
        const val REFRESH_MILLIS = 30_000L
        const val ROUND_UP_MILLIS = 1_000L
        const val VESSEL_WIDTH_DP = 64
        const val VESSEL_HEIGHT_DP = 88
        val RestingColor = Color(0xFF39FF14).copy(alpha = 0.8f)
        const val GlassColor = 0xFFCCCCCC.toInt()
    }
}

/** Pide a la pantalla de inicio fijar el widget (Android 8+, si el lanzador lo permite). */
fun Context.requestPinTimerWidget(): Boolean {
    val manager = AppWidgetManager.getInstance(this)
    if (!manager.isRequestPinAppWidgetSupported) return false
    return manager.requestPinAppWidget(ComponentName(this, TimerWidgetProvider::class.java), null, null)
}
