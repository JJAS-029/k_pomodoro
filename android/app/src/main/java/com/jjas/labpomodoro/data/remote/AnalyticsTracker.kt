package com.jjas.labpomodoro.data.remote

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/** Envoltura de Firebase Analytics para no esparcir nombres de eventos por la app. */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {

    /** Base para el reporte de "horas pico de productividad". [type]: work, short_break o long_break. */
    fun logSessionCompleted(type: String, hourOfDay: Int, durationMin: Int) {
        analytics.logEvent(
            "session_completed",
            bundleOf(
                "session_type" to type,
                "hour_of_day" to hourOfDay.toLong(),
                "duration_min" to durationMin.toLong(),
            )
        )
    }

    fun logLogin() {
        analytics.logEvent(
            FirebaseAnalytics.Event.LOGIN,
            bundleOf(FirebaseAnalytics.Param.METHOD to "google")
        )
    }
}
