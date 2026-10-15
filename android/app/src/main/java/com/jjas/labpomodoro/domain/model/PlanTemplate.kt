package com.jjas.labpomodoro.domain.model

/** Un plan guardado para iniciarlo con un toque: duraciones y, si se eligió, la etiqueta. */
data class PlanTemplate(val name: String, val config: SessionConfig, val tagId: Long? = null) {

    companion object {
        const val MAX_TEMPLATES = 8
        const val MAX_NAME = 24

        /** Una plantilla por línea, campos separados por tabulador (el nombre no puede llevarlos). */
        fun encode(templates: List<PlanTemplate>): String = templates.joinToString("\n") { t ->
            val c = t.config
            listOf(
                t.name.replace('\t', ' ').replace('\n', ' ').take(MAX_NAME),
                c.totalHours, c.workMinutes, c.shortBreakMinutes, c.longBreakMinutes, c.pomodorosUntilLong,
                c.rounding.name, t.tagId ?: "",
            ).joinToString("\t")
        }

        /** Lo que no se entienda se ignora (por ejemplo, una versión futura con más campos). */
        fun decode(text: String): List<PlanTemplate> = text.lines().mapNotNull { line ->
            val f = line.split('\t')
            if (f.size < 7) return@mapNotNull null
            runCatching {
                PlanTemplate(
                    name = f[0],
                    config = SessionConfig(
                        totalHours = f[1].toInt(),
                        workMinutes = f[2].toInt(),
                        shortBreakMinutes = f[3].toInt(),
                        longBreakMinutes = f[4].toInt(),
                        pomodorosUntilLong = f[5].toInt(),
                        rounding = PlanRounding.valueOf(f[6]),
                    ).normalized(),
                    tagId = f.getOrNull(7)?.toLongOrNull(),
                )
            }.getOrNull()
        }
    }
}
