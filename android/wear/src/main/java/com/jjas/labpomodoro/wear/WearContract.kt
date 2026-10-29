package com.jjas.labpomodoro.wear

/**
 * Lo que se dicen el teléfono y el reloj por la Data Layer de Wear OS. Copia idéntica de la del
 * módulo `app` (mismo paquete): si cambia algo aquí, cambia allá también.
 */
object WearContract {
    /** DataItem con el estado del timer; el teléfono lo publica y el reloj lo lee. */
    const val TIMER_PATH = "/lab/timer"

    /** Mensajes del reloj al teléfono: `/lab/command/<orden>`. */
    const val COMMAND_PREFIX = "/lab/command/"
    const val START = "start"
    const val PAUSE = "pause"
    const val RESUME = "resume"
    const val SKIP = "skip"

    // Claves del DataItem
    /** IDLE, ACTIVE o FINISHED. */
    const val KEY_STATE = "state"

    /** WORK, SHORT_BREAK o LONG_BREAK. */
    const val KEY_TYPE = "type"

    /** Cuándo termina la sesión, en hora de reloj de pared (ms); el reloj cuenta solo con esto. */
    const val KEY_ENDS_AT = "endsAt"
    const val KEY_PAUSED = "paused"

    /** Lo que falta (ms) cuando está en pausa. */
    const val KEY_REMAINING = "remaining"

    /** Duración de la sesión actual (ms), para el anillo de progreso. */
    const val KEY_DURATION = "duration"

    /** Pomodoro actual (desde 1) y cuántos tiene el plan. */
    const val KEY_POMODORO = "pomodoro"
    const val KEY_POMODOROS = "pomodoros"

    /** Color del líquido (ARGB) y forma del recipiente ([com.jjas.labpomodoro.ui.components.VesselShape]). */
    const val KEY_COLOR = "color"
    const val KEY_SHAPE = "shape"
    const val KEY_PRO = "pro"

    /** En una sala de estudio; solo el anfitrión pausa y nadie salta. */
    const val KEY_ROOM = "room"
    const val KEY_HOST = "host"

    /**
     * El plan de siempre (tipo y segundos de cada sesión), para que el reloj lo corra solo si el
     * teléfono no está cerca (Pro).
     */
    const val KEY_PLAN_TYPES = "planTypes"
    const val KEY_PLAN_SECONDS = "planSeconds"

    /**
     * DataItem del reloj con los pomodoros que completó sin el teléfono (JSON: id, tipo, inicio,
     * fin y segundos). La Data Layer lo entrega al reconectarse.
     */
    const val OFFLINE_PATH = "/lab/offline"
    const val KEY_SESSIONS = "sessions"

    /** El teléfono confirma (mensaje con los ids separados por comas) lo que ya registró. */
    const val ACK_PATH = "/lab/ack"
}
