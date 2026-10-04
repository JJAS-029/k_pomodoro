package com.jjas.labpomodoro.domain.model

/** Un bloque del plan: qué tipo de sesión es y cuánto dura. */
data class PlannedSession(
    val type: SessionType,
    val durationSeconds: Int,
)
