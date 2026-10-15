package com.jjas.labpomodoro.domain.model

/** "[He]2s2 2p2" → "[He] 2s² 2p²": los electrones de cada orbital van como superíndice. */
fun ElementProperties.prettyConfiguration(): String {
    val superscripts = "⁰¹²³⁴⁵⁶⁷⁸⁹"
    return Regex("([spdf])(\\d+)").replace(electronConfiguration) { m ->
        m.groupValues[1] + m.groupValues[2].map { superscripts[it - '0'] }.joinToString("")
    }.replace("]", "] ")
}

/** Kelvin → grados Celsius. */
fun kelvinToCelsius(kelvin: Double): Double = kelvin - 273.15

val ElementState.isGas: Boolean get() = this == ElementState.GAS || this == ElementState.GAS_PREDICTED
