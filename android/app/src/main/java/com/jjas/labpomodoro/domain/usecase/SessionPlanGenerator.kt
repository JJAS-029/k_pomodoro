package com.jjas.labpomodoro.domain.usecase

import com.jjas.labpomodoro.domain.model.PlanRounding
import com.jjas.labpomodoro.domain.model.PlannedSession
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType

/**
 * Convierte "N horas de trabajo" en la secuencia TRABAJO / CORTO / LARGO (equivale a `generarPlan()`
 * del prototipo). Nunca termina en descanso.
 *
 * Con [PlanRounding.TRIM_LAST] el último pomodoro se acorta para sumar exactamente las horas pedidas;
 * con [PlanRounding.WHOLE_POMODOROS] se redondea hacia arriba como hacía el prototipo (`Math.ceil`).
 */
object SessionPlanGenerator {

    fun generate(config: SessionConfig): List<PlannedSession> {
        val c = config.normalized()
        val totalWorkMinutes = c.totalHours * 60
        val fullPomodoros = totalWorkMinutes / c.workMinutes
        val remainderMinutes = totalWorkMinutes % c.workMinutes

        val workBlocks = buildList {
            repeat(fullPomodoros) { add(c.workMinutes) }
            if (remainderMinutes > 0) {
                add(if (c.rounding == PlanRounding.TRIM_LAST) remainderMinutes else c.workMinutes)
            }
        }

        return buildList {
            workBlocks.forEachIndexed { index, minutes ->
                add(PlannedSession(SessionType.WORK, minutes * 60))
                val number = index + 1
                if (number < workBlocks.size) {
                    add(
                        if (number % c.pomodorosUntilLong == 0) {
                            PlannedSession(SessionType.LONG_BREAK, c.longBreakMinutes * 60)
                        } else {
                            PlannedSession(SessionType.SHORT_BREAK, c.shortBreakMinutes * 60)
                        }
                    )
                }
            }
        }
    }
}
