package com.jjas.labpomodoro.data.remote

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

/** Envoltura de Firebase Analytics para no esparcir nombres de eventos por la app. */
@Singleton
class AnalyticsTracker @Inject constructor(
    private val analytics: FirebaseAnalytics,
) {

    /** Base para el reporte de "horas pico de productividad". [type]: work, short_break o long_break. */
    fun logSessionCompleted(type: String, hourOfDay: Int, durationMin: Int) {
        analytics.logEvent("session_completed") {
            param("session_type", type)
            param("hour_of_day", hourOfDay.toLong())
            param("duration_min", durationMin.toLong())
        }
    }

    fun logLogin() {
        analytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, "google")
        }
    }
}
