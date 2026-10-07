package com.jjas.labpomodoro.domain.model

/**
 * Qué es cada elemento y para qué sirve en la vida real, para la ficha de la tabla periódica.
 * Los textos viven en los recursos `element_fact_descriptions` y `element_fact_uses`
 * (índice = número atómico − 1).
 */
data class ElementFact(
    /** Qué es: aspecto, origen o un dato curioso. */
    val description: String,
    /** Para qué sirve en la vida diaria o en la ciencia. */
    val uses: String,
)
