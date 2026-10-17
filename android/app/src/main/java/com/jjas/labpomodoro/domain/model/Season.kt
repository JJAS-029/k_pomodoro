package com.jjas.labpomodoro.domain.model

import java.time.LocalDate
import java.time.MonthDay

/**
 * Temporadas: unos días al año la app se viste de fiesta (paleta de líquidos y algo que cae detrás
 * del recipiente). [from] y [to] son inclusivos; si [to] es antes que [from], cruza el año.
 */
enum class Season(val from: MonthDay, val to: MonthDay, val palette: LabPalette) {
    /** Día de Muertos: cempasúchil, papel picado y pétalos que caen. */
    MUERTOS(MonthDay.of(10, 25), MonthDay.of(11, 3), LabPalette.SEASON_MUERTOS),

    /** Invierno: líquidos helados, rojo y verde, y nieve. */
    WINTER(MonthDay.of(12, 12), MonthDay.of(1, 6), LabPalette.SEASON_WINTER),
    ;

    fun contains(date: LocalDate): Boolean {
        val day = MonthDay.from(date)
        return if (from <= to) day in from..to else day >= from || day <= to
    }

    companion object {
        fun on(date: LocalDate): Season? = entries.firstOrNull { it.contains(date) }
    }
}
