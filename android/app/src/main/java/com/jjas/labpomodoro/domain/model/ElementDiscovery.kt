package com.jjas.labpomodoro.domain.model

/**
 * Descubrimiento de cada elemento: cuándo, quién y dónde. Los textos (época, descubridores y país)
 * viven en los recursos `element_discovery_eras`, `element_discoverers` y
 * `element_discovery_countries` para poder traducirlos; aquí solo quedan los años.
 *
 * @param year año del descubrimiento o aislamiento reconocido; null si se conoce desde la Antigüedad o la prehistoria.
 * @param era texto para cuando no hay año exacto (por ejemplo "Antigüedad", "Edad Media"); null si hay año.
 * @param discoverers quién(es) lo descubrieron ("Humphry Davy", "Marie y Pierre Curie"); "Desconocido" para los antiguos.
 * @param country país (nombre actual) donde se hizo el descubrimiento; para colaboraciones, los países juntos
 *   (por ejemplo "Rusia y Estados Unidos"); null para los antiguos.
 */
data class ElementDiscovery(
    val year: Int?,
    val era: String?,
    val discoverers: String,
    val country: String?,
)

object ElementDiscoveries {

    /** Año del descubrimiento; null si se conoce desde la Antigüedad o la prehistoria. */
    fun year(atomicNumber: Int): Int? = years[atomicNumber - 1]

    // Índice = número atómico − 1
    private val years: List<Int?> = listOf(
        1766, 1868, 1817, 1798, 1808, null, 1772, 1774, 1886, 1898, // 1–10
        1807, 1755, 1825, 1824, 1669, null, 1774, 1894, 1807, 1808, // 11–20
        1879, 1791, 1801, 1797, 1774, null, 1735, 1751, null, 1746, // 21–30
        1875, 1886, 1250, 1817, 1826, 1898, 1861, 1790, 1794, 1789, // 31–40
        1801, 1778, 1937, 1844, 1803, 1802, null, 1817, 1863, null, // 41–50
        null, 1782, 1811, 1898, 1860, 1772, 1839, 1803, 1885, 1885, // 51–60
        1945, 1879, 1901, 1880, 1843, 1886, 1878, 1843, 1879, 1878, // 61–70
        1907, 1923, 1802, 1783, 1925, 1803, 1803, 1748, null, null, // 71–80
        1861, null, 1753, 1898, 1940, 1900, 1939, 1898, 1899, 1829, // 81–90
        1913, 1789, 1940, 1940, 1944, 1944, 1949, 1950, 1952, 1952, // 91–100
        1955, 1966, 1961, 1964, 1968, 1974, 1981, 1984, 1982, 1994, // 101–110
        1994, 1996, 2004, 1999, 2003, 2000, 2010, 2002, // 111–118
    )

    init {
        check(years.size == PeriodicTable.SIZE)
    }
}
