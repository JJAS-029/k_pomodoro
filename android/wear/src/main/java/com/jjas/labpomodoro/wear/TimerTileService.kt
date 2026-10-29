package com.jjas.labpomodoro.wear

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicDuration
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInstant
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.time.Instant

/**
 * Tile del reloj: el tiempo que falta de la sesión, que avanza solo (el sistema lo calcula con la
 * hora de fin). Tocarlo abre la app; con Pro, un botón pausa o continúa ahí mismo.
 */
class TimerTileService : TileService() {

    override fun onTileRequest(request: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        // El botón del tile manda la orden al teléfono
        val local = LocalPlan.isActive(this)
        when (request.currentState.lastClickableId) {
            CLICK_PAUSE -> if (local) LocalPlan.pause(this) else PhoneLink(this).sendAsync(WearContract.PAUSE)
            CLICK_RESUME -> if (local) LocalPlan.resume(this) else PhoneLink(this).sendAsync(WearContract.RESUME)
        }
        val timer = shownTimer(this)
        val root = layout(this, timer)
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                // Se vuelve a pedir cuando el teléfono avisa de un cambio (LastTimer.save)
                .setFreshnessIntervalMillis(0)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                .build()
        )
    }

    override fun onTileResourcesRequest(request: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> =
        Futures.immediateFuture(ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build())

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val CLICK_OPEN = "open"
        private const val CLICK_PAUSE = "pause"
        private const val CLICK_RESUME = "resume"
        private const val GREEN = 0xFF39FF14.toInt()

        /** Pide al sistema que vuelva a dibujar el tile. */
        fun refresh(context: Context) {
            getUpdater(context).requestUpdate(TimerTileService::class.java)
        }

        private fun openApp(context: Context) = ActionBuilders.LaunchAction.Builder()
            .setAndroidActivity(
                ActionBuilders.AndroidActivity.Builder()
                    .setPackageName(context.packageName)
                    .setClassName(WearMainActivity::class.java.name)
                    .build()
            )
            .build()

        private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(
                LayoutElementBuilders.FontStyle.Builder()
                    .setSize(sp(size))
                    .setColor(argb(color))
                    .apply { if (bold) setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD) }
                    .build()
            )
            .build()

        fun layout(context: Context, timer: WatchTimer?): LayoutElementBuilders.LayoutElement {
            val column = LayoutElementBuilders.Column.Builder()
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            if (timer == null || !timer.active) {
                column.addContent(text(context.getString(R.string.app_name), 16f, GREEN, bold = true))
                column.addContent(spacer(6f))
                column.addContent(text(context.getString(if (timer?.state == "FINISHED") R.string.finished else R.string.tile_idle), 14f, 0xFFFFFFFF.toInt()))
            } else {
                val label = context.getString(WatchNotifier.sessionLabel(timer))
                column.addContent(text(label, 14f, timer.color))
                column.addContent(spacer(2f))
                column.addContent(countdown(timer))
                if (timer.pomodoros > 0) column.addContent(text(context.getString(R.string.pomodoro_of, timer.pomodoro, timer.pomodoros), 12f, 0xFFBBBBBB.toInt()))
                // Pausar o continuar desde el tile es Pro (y en una sala, solo el anfitrión)
                if (timer.isPro && timer.canPause) {
                    column.addContent(spacer(8f))
                    column.addContent(button(context.getString(if (timer.paused) R.string.resume else R.string.pause), if (timer.paused) CLICK_RESUME else CLICK_PAUSE))
                }
            }
            return LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setClickable(ModifiersBuilders.Clickable.Builder().setId(CLICK_OPEN).setOnClick(openApp(context)).build())
                        .build()
                )
                .addContent(column.build())
                .build()
        }

        /** Cuenta regresiva que actualiza el sistema cada segundo; en pausa, el tiempo congelado. */
        private fun countdown(timer: WatchTimer): LayoutElementBuilders.LayoutElement {
            val frozen = format(timer.remaining)
            if (timer.paused) return text(frozen, 38f, 0xFFFFFFFF.toInt(), bold = true)
            val left: DynamicDuration = DynamicInstant.platformTimeWithSecondsPrecision()
                .durationUntil(DynamicInstant.withSecondsPrecision(Instant.ofEpochMilli(timer.endsAt)))
            val twoDigits = DynamicInt32.IntFormatter.Builder().setMinIntegerDigits(2).build()
            val value: DynamicString = left.toIntMinutes().format()
                .concat(DynamicString.constant(":"))
                .concat(left.secondsPart.format(twoDigits))
            return LayoutElementBuilders.Text.Builder()
                .setText(TypeBuilders.StringProp.Builder(frozen).setDynamicValue(value).build())
                .setLayoutConstraintsForDynamicText(TypeBuilders.StringLayoutConstraint.Builder("00:00").build())
                .setFontStyle(
                    LayoutElementBuilders.FontStyle.Builder()
                        .setSize(sp(38f))
                        .setColor(argb(0xFFFFFFFF.toInt()))
                        .setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .build()
                )
                .build()
        }

        private fun button(label: String, id: String) = LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(ModifiersBuilders.Clickable.Builder().setId(id).setOnClick(ActionBuilders.LoadAction.Builder().build()).build())
                    .setBackground(
                        ModifiersBuilders.Background.Builder()
                            .setColor(argb(GREEN))
                            .setCorner(ModifiersBuilders.Corner.Builder().setRadius(dp(20f)).build())
                            .build()
                    )
                    .setPadding(ModifiersBuilders.Padding.Builder().setStart(dp(18f)).setEnd(dp(18f)).setTop(dp(8f)).setBottom(dp(8f)).build())
                    .build()
            )
            .addContent(text(label, 14f, 0xFF000000.toInt(), bold = true))
            .build()

        private fun spacer(height: Float) = LayoutElementBuilders.Spacer.Builder().setHeight(dp(height)).build()

        private fun format(millis: Long): String {
            val seconds = (millis + 999) / 1000
            return "%d:%02d".format(seconds / 60, seconds % 60)
        }
    }
}
