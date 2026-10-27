package com.jjas.labpomodoro.timer

/**
 * Enlace del timer con una sala de estudio: mientras se está en una sala, el plan lo manda ella
 * (empezar, pausar y continuar pasan por la sala y llegan de vuelta con [TimerEngine.follow]).
 * Lo usan igual la pantalla, la notificación y el widget.
 */
interface RoomLink {
    /** Pedir que empiece el plan. Devuelve true si la sala se encarga. */
    suspend fun onStart(): Boolean

    /** Pedir pausa. Devuelve true si la sala se encarga (o si no se permite: solo el anfitrión pausa). */
    suspend fun onPause(): Boolean

    /** Pedir que continúe. Igual que [onPause]. */
    suspend fun onResume(): Boolean

    /** Detener el plan es salir de la sala (o cerrarla, si es tuya); luego el timer se detiene. */
    suspend fun onStop()
}
