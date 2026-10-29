package com.jjas.labpomodoro.wear

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationText
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Complicación (Pro): un anillo con lo que lleva la sesión y los minutos que faltan, para
 * cualquier carátula. El sistema cuenta los minutos solo con la hora de fin. Sin Pro muestra un
 * candado y abre la app.
 */
class TimerComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = when (type) {
        ComplicationType.RANGED_VALUE -> ranged(text("12'"), value = 0.5f, description = getString(R.string.work), color = 0)
        ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text("12'"), text(getString(R.string.work))).setMonochromaticImage(icon()).build()
        else -> null
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val timer = shownTimer(this)
        val description = text(getString(R.string.app_name))
        return when {
            timer == null || !timer.active -> idle(request.complicationType, getString(R.string.tile_idle))
            // La complicación es Pro: candado y la app al tocarla
            !timer.isPro -> idle(request.complicationType, getString(R.string.complication_pro), locked = true)
            else -> {
                val label = getString(WatchNotifier.sessionLabel(timer))
                val minutes: ComplicationText = if (timer.paused) {
                    text("${(timer.remaining + 59_999) / 60_000}'")
                } else {
                    TimeDifferenceComplicationText.Builder(TimeDifferenceStyle.SHORT_SINGLE_UNIT, CountDownTimeReference(Instant.ofEpochMilli(timer.endsAt)))
                        .setMinimumTimeUnit(TimeUnit.MINUTES)
                        .build()
                }
                val done = 1f - timer.remainingAt(System.currentTimeMillis()).toFloat() / timer.duration
                when (request.complicationType) {
                    ComplicationType.RANGED_VALUE -> ranged(minutes, done, label, timer.color)
                    ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(minutes, description)
                        .setTitle(text(label))
                        .setMonochromaticImage(icon())
                        .setTapAction(openApp())
                        .build()
                    else -> NoDataComplicationData()
                }
            }
        }
    }

    private fun ranged(minutes: ComplicationText, value: Float, description: String, color: Int) =
        RangedValueComplicationData.Builder(value.coerceIn(0f, 1f), 0f, 1f, text(description))
            .setText(minutes)
            .setMonochromaticImage(icon())
            .setTapAction(openApp())
            .apply { if (color != 0) setColorRamp(androidx.wear.watchface.complications.data.ColorRamp(intArrayOf(color, color), false)) }
            .build()

    private fun idle(type: ComplicationType, description: String, locked: Boolean = false): ComplicationData {
        val icon = if (locked) MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_lock)).build() else icon()
        return when (type) {
            ComplicationType.RANGED_VALUE -> RangedValueComplicationData.Builder(0f, 0f, 1f, text(description))
                .setText(text("--"))
                .setMonochromaticImage(icon)
                .setTapAction(openApp())
                .build()
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(text("--"), text(description))
                .setMonochromaticImage(icon)
                .setTapAction(openApp())
                .build()
            else -> NoDataComplicationData()
        }
    }

    private fun icon() = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_stat_timer)).build()

    private fun text(value: String) = PlainComplicationText.Builder(value).build()

    private fun openApp() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, WearMainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        /** Pide a las carátulas que vuelvan a leer la complicación. */
        fun refresh(context: Context) {
            ComplicationDataSourceUpdateRequester.create(context, ComponentName(context, TimerComplicationService::class.java)).requestUpdateAll()
        }
    }
}
