package com.jjas.labpomodoro.domain.model

/** Qué hacer cuando las horas pedidas no son múltiplo exacto de la duración del pomodoro. */
enum class PlanRounding {
    /** Recorta el último pomodoro para no pasarse nunca de las horas pedidas (por defecto). */
    TRIM_LAST,

    /** Completa siempre pomodoros enteros, aunque se pase un poco del tiempo pedido. */
    WHOLE_POMODOROS,
}

/**
 * Configuración del plan de sesiones. Los valores por defecto y los rangos son los del prototipo web;
 * [normalized] los fuerza a esos rangos para que un valor corrupto en disco no rompa el timer.
 */
data class SessionConfig(
    val totalHours: Int = 2,
    val workMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val pomodorosUntilLong: Int = 4,
    val rounding: PlanRounding = PlanRounding.TRIM_LAST,
) {
    fun normalized() = copy(
        totalHours = totalHours.coerceIn(TOTAL_HOURS),
        workMinutes = workMinutes.coerceIn(WORK_MINUTES),
        shortBreakMinutes = shortBreakMinutes.coerceIn(SHORT_BREAK_MINUTES),
        longBreakMinutes = longBreakMinutes.coerceIn(LONG_BREAK_MINUTES),
        pomodorosUntilLong = pomodorosUntilLong.coerceIn(POMODOROS_UNTIL_LONG),
    )

    companion object {
        val TOTAL_HOURS = 1..24
        val WORK_MINUTES = 5..120
        val SHORT_BREAK_MINUTES = 1..60
        val LONG_BREAK_MINUTES = 5..120
        val POMODOROS_UNTIL_LONG = 2..10
    }
}

data class AppSettings(
    val session: SessionConfig = SessionConfig(),
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    /** Mantiene la pantalla encendida mientras corre el timer. */
    val keepScreenOn: Boolean = false,
    /** Pro: tras [ambientDelayMinutes] sin tocar la pantalla, solo queda el reloj atenuado. */
    val ambientMode: Boolean = false,
    val ambientDelayMinutes: Int = 3,
    /** Pro: colores de Material You tomados del fondo de pantalla (Android 12+). */
    val dynamicColor: Boolean = false,
    /** Pro comprado en Google Play (suscripción vigente o pago único). Lo actualiza la tienda. */
    val proPurchased: Boolean = false,
    /** Interruptor de pruebas: solo tiene efecto en compilaciones debug. */
    val proTesting: Boolean = false,
    /** La guía de uso se muestra sola la primera vez. */
    val guideSeen: Boolean = false,
    /**
     * Elemento que tiñe los recipientes de trabajo. 0 = automático: uno distinto de tu
     * colección en cada sesión.
     */
    val vesselElement: Int = 0,
    /** Sonido de fondo para concentrarse (Pro). */
    val focusSound: FocusSound = FocusSound.OFF,
    /** Volumen del sonido de fondo, 0..1. */
    val focusVolume: Float = 0.5f,
) {
    /** El modo ambiente solo aplica si el usuario es Pro. */
    val isPro: Boolean get() = proPurchased || proTesting

    val ambientActive: Boolean get() = isPro && ambientMode

    val dynamicColorActive: Boolean get() = isPro && dynamicColor

    companion object {
        val AMBIENT_DELAYS = listOf(3, 5)
    }
}
