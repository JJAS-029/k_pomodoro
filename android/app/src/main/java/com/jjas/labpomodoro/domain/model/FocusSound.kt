package com.jjas.labpomodoro.domain.model

/**
 * Sonidos de fondo para concentrarse. Todos se generan en el teléfono (sin archivos de audio):
 * ruido con distintos "colores" y dos ambientes hechos a partir de ruido filtrado.
 */
enum class FocusSound(val label: String, val description: String) {
    OFF("Sin sonido", ""),
    WHITE("Ruido blanco", "Parejo en todas las frecuencias, como estática: tapa bien las voces."),
    PINK("Ruido rosa", "Más suave en los agudos, como lluvia constante."),
    BROWN("Ruido café", "Grave y profundo, como una cascada lejana o el motor de un avión."),
    RAIN("Lluvia", "Lluvia sobre una ventana, con gotas que caen al azar."),
    WAVES("Olas", "Oleaje que sube y baja lento, para respirar al ritmo."),
}
