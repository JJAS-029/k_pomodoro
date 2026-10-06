package com.jjas.labpomodoro.domain.model

/**
 * Descubrimiento de cada elemento: cuándo, quién y dónde. El índice es el número atómico − 1.
 *
 * @param year año del descubrimiento o aislamiento reconocido; null si se conoce desde la Antigüedad o la prehistoria.
 * @param era texto para cuando no hay año exacto (por ejemplo "Antigüedad", "Edad Media"); null si hay año.
 * @param discoverers quién(es) lo descubrieron, en español natural ("Humphry Davy", "Marie y Pierre Curie", "Equipo del JINR (Dubná) y LLNL"); "Desconocido" para los antiguos.
 * @param country país (nombre actual en español) donde se hizo el descubrimiento; para colaboraciones, los países separados por " y " (por ejemplo "Rusia y Estados Unidos"); null para los antiguos.
 */
data class ElementDiscovery(
    val year: Int?,
    val era: String?,
    val discoverers: String,
    val country: String?,
)

object ElementDiscoveries {

    operator fun get(atomicNumber: Int): ElementDiscovery = list[atomicNumber - 1]

    private const val UNKNOWN = "Desconocido"
    private const val PREHISTORY = "Prehistoria"
    private const val ANTIQUITY = "Antigüedad"
    private const val GSI_ARMBRUSTER = "Equipo del GSI (Peter Armbruster)"
    private const val GSI_HOFMANN = "Equipo del GSI (Sigurd Hofmann)"
    private const val JINR_LLNL = "Equipo del JINR (Dubná) y LLNL"
    private const val JINR_LBL = "Equipos del JINR (Dubná) y LBL (Berkeley)"
    private const val RUSSIA_USA = "Rusia y Estados Unidos"

    private val list: List<ElementDiscovery> = listOf(
        // 1 H · Hidrógeno
        ElementDiscovery(1766, null, "Henry Cavendish", "Reino Unido"),
        // 2 He · Helio
        ElementDiscovery(1868, null, "Pierre Janssen y Norman Lockyer", "India y Reino Unido"),
        // 3 Li · Litio
        ElementDiscovery(1817, null, "Johan August Arfwedson", "Suecia"),
        // 4 Be · Berilio
        ElementDiscovery(1798, null, "Louis-Nicolas Vauquelin", "Francia"),
        // 5 B · Boro
        ElementDiscovery(1808, null, "Gay-Lussac, Thénard y Humphry Davy", "Francia y Reino Unido"),
        // 6 C · Carbono
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 7 N · Nitrógeno
        ElementDiscovery(1772, null, "Daniel Rutherford", "Reino Unido"),
        // 8 O · Oxígeno
        ElementDiscovery(1774, null, "Joseph Priestley y Carl Wilhelm Scheele", "Reino Unido y Suecia"),
        // 9 F · Flúor
        ElementDiscovery(1886, null, "Henri Moissan", "Francia"),
        // 10 Ne · Neón
        ElementDiscovery(1898, null, "William Ramsay y Morris Travers", "Reino Unido"),
        // 11 Na · Sodio
        ElementDiscovery(1807, null, "Humphry Davy", "Reino Unido"),
        // 12 Mg · Magnesio
        ElementDiscovery(1755, null, "Joseph Black", "Reino Unido"),
        // 13 Al · Aluminio
        ElementDiscovery(1825, null, "Hans Christian Ørsted", "Dinamarca"),
        // 14 Si · Silicio
        ElementDiscovery(1824, null, "Jöns Jacob Berzelius", "Suecia"),
        // 15 P · Fósforo
        ElementDiscovery(1669, null, "Hennig Brand", "Alemania"),
        // 16 S · Azufre
        ElementDiscovery(null, ANTIQUITY, UNKNOWN, null),
        // 17 Cl · Cloro
        ElementDiscovery(1774, null, "Carl Wilhelm Scheele", "Suecia"),
        // 18 Ar · Argón
        ElementDiscovery(1894, null, "Lord Rayleigh y William Ramsay", "Reino Unido"),
        // 19 K · Potasio
        ElementDiscovery(1807, null, "Humphry Davy", "Reino Unido"),
        // 20 Ca · Calcio
        ElementDiscovery(1808, null, "Humphry Davy", "Reino Unido"),
        // 21 Sc · Escandio
        ElementDiscovery(1879, null, "Lars Fredrik Nilson", "Suecia"),
        // 22 Ti · Titanio
        ElementDiscovery(1791, null, "William Gregor", "Reino Unido"),
        // 23 V · Vanadio
        ElementDiscovery(1801, null, "Andrés Manuel del Río", "México"),
        // 24 Cr · Cromo
        ElementDiscovery(1797, null, "Louis-Nicolas Vauquelin", "Francia"),
        // 25 Mn · Manganeso
        ElementDiscovery(1774, null, "Johan Gottlieb Gahn", "Suecia"),
        // 26 Fe · Hierro
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 27 Co · Cobalto
        ElementDiscovery(1735, null, "Georg Brandt", "Suecia"),
        // 28 Ni · Níquel
        ElementDiscovery(1751, null, "Axel Fredrik Cronstedt", "Suecia"),
        // 29 Cu · Cobre
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 30 Zn · Zinc
        ElementDiscovery(1746, null, "Andreas Sigismund Marggraf", "Alemania"),
        // 31 Ga · Galio
        ElementDiscovery(1875, null, "Paul-Émile Lecoq de Boisbaudran", "Francia"),
        // 32 Ge · Germanio
        ElementDiscovery(1886, null, "Clemens Winkler", "Alemania"),
        // 33 As · Arsénico
        ElementDiscovery(1250, null, "Alberto Magno", "Alemania"),
        // 34 Se · Selenio
        ElementDiscovery(1817, null, "Jöns Jacob Berzelius y Johan Gottlieb Gahn", "Suecia"),
        // 35 Br · Bromo
        ElementDiscovery(1826, null, "Antoine-Jérôme Balard", "Francia"),
        // 36 Kr · Kriptón
        ElementDiscovery(1898, null, "William Ramsay y Morris Travers", "Reino Unido"),
        // 37 Rb · Rubidio
        ElementDiscovery(1861, null, "Robert Bunsen y Gustav Kirchhoff", "Alemania"),
        // 38 Sr · Estroncio
        ElementDiscovery(1790, null, "Adair Crawford", "Reino Unido"),
        // 39 Y · Itrio
        ElementDiscovery(1794, null, "Johan Gadolin", "Finlandia"),
        // 40 Zr · Circonio
        ElementDiscovery(1789, null, "Martin Heinrich Klaproth", "Alemania"),
        // 41 Nb · Niobio
        ElementDiscovery(1801, null, "Charles Hatchett", "Reino Unido"),
        // 42 Mo · Molibdeno
        ElementDiscovery(1778, null, "Carl Wilhelm Scheele", "Suecia"),
        // 43 Tc · Tecnecio
        ElementDiscovery(1937, null, "Carlo Perrier y Emilio Segrè", "Italia"),
        // 44 Ru · Rutenio
        ElementDiscovery(1844, null, "Karl Ernst Claus", "Rusia"),
        // 45 Rh · Rodio
        ElementDiscovery(1803, null, "William Hyde Wollaston", "Reino Unido"),
        // 46 Pd · Paladio
        ElementDiscovery(1802, null, "William Hyde Wollaston", "Reino Unido"),
        // 47 Ag · Plata
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 48 Cd · Cadmio
        ElementDiscovery(1817, null, "Friedrich Stromeyer", "Alemania"),
        // 49 In · Indio
        ElementDiscovery(1863, null, "Ferdinand Reich y Theodor Richter", "Alemania"),
        // 50 Sn · Estaño
        ElementDiscovery(null, ANTIQUITY, UNKNOWN, null),
        // 51 Sb · Antimonio
        ElementDiscovery(null, ANTIQUITY, UNKNOWN, null),
        // 52 Te · Telurio
        ElementDiscovery(1782, null, "Franz-Joseph Müller von Reichenstein", "Rumanía"),
        // 53 I · Yodo
        ElementDiscovery(1811, null, "Bernard Courtois", "Francia"),
        // 54 Xe · Xenón
        ElementDiscovery(1898, null, "William Ramsay y Morris Travers", "Reino Unido"),
        // 55 Cs · Cesio
        ElementDiscovery(1860, null, "Robert Bunsen y Gustav Kirchhoff", "Alemania"),
        // 56 Ba · Bario
        ElementDiscovery(1772, null, "Carl Wilhelm Scheele", "Suecia"),
        // 57 La · Lantano
        ElementDiscovery(1839, null, "Carl Gustaf Mosander", "Suecia"),
        // 58 Ce · Cerio
        ElementDiscovery(1803, null, "Berzelius, Hisinger y Klaproth", "Suecia y Alemania"),
        // 59 Pr · Praseodimio
        ElementDiscovery(1885, null, "Carl Auer von Welsbach", "Austria"),
        // 60 Nd · Neodimio
        ElementDiscovery(1885, null, "Carl Auer von Welsbach", "Austria"),
        // 61 Pm · Prometio
        ElementDiscovery(1945, null, "Marinsky, Glendenin y Coryell", "Estados Unidos"),
        // 62 Sm · Samario
        ElementDiscovery(1879, null, "Paul-Émile Lecoq de Boisbaudran", "Francia"),
        // 63 Eu · Europio
        ElementDiscovery(1901, null, "Eugène-Anatole Demarçay", "Francia"),
        // 64 Gd · Gadolinio
        ElementDiscovery(1880, null, "Jean Charles Galissard de Marignac", "Suiza"),
        // 65 Tb · Terbio
        ElementDiscovery(1843, null, "Carl Gustaf Mosander", "Suecia"),
        // 66 Dy · Disprosio
        ElementDiscovery(1886, null, "Paul-Émile Lecoq de Boisbaudran", "Francia"),
        // 67 Ho · Holmio
        ElementDiscovery(1878, null, "Marc Delafontaine y Jacques-Louis Soret", "Suiza"),
        // 68 Er · Erbio
        ElementDiscovery(1843, null, "Carl Gustaf Mosander", "Suecia"),
        // 69 Tm · Tulio
        ElementDiscovery(1879, null, "Per Teodor Cleve", "Suecia"),
        // 70 Yb · Iterbio
        ElementDiscovery(1878, null, "Jean Charles Galissard de Marignac", "Suiza"),
        // 71 Lu · Lutecio
        ElementDiscovery(1907, null, "Georges Urbain", "Francia"),
        // 72 Hf · Hafnio
        ElementDiscovery(1923, null, "Dirk Coster y George de Hevesy", "Dinamarca"),
        // 73 Ta · Tantalio
        ElementDiscovery(1802, null, "Anders Gustaf Ekeberg", "Suecia"),
        // 74 W · Wolframio
        ElementDiscovery(1783, null, "Juan José y Fausto Elhuyar", "España"),
        // 75 Re · Renio
        ElementDiscovery(1925, null, "Walter Noddack, Ida Tacke y Otto Berg", "Alemania"),
        // 76 Os · Osmio
        ElementDiscovery(1803, null, "Smithson Tennant", "Reino Unido"),
        // 77 Ir · Iridio
        ElementDiscovery(1803, null, "Smithson Tennant", "Reino Unido"),
        // 78 Pt · Platino
        ElementDiscovery(1748, null, "Antonio de Ulloa", "Colombia"),
        // 79 Au · Oro
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 80 Hg · Mercurio
        ElementDiscovery(null, ANTIQUITY, UNKNOWN, null),
        // 81 Tl · Talio
        ElementDiscovery(1861, null, "William Crookes", "Reino Unido"),
        // 82 Pb · Plomo
        ElementDiscovery(null, PREHISTORY, UNKNOWN, null),
        // 83 Bi · Bismuto
        ElementDiscovery(1753, null, "Claude François Geoffroy", "Francia"),
        // 84 Po · Polonio
        ElementDiscovery(1898, null, "Marie y Pierre Curie", "Francia"),
        // 85 At · Astato
        ElementDiscovery(1940, null, "Corson, MacKenzie y Segrè", "Estados Unidos"),
        // 86 Rn · Radón
        ElementDiscovery(1900, null, "Friedrich Ernst Dorn", "Alemania"),
        // 87 Fr · Francio
        ElementDiscovery(1939, null, "Marguerite Perey", "Francia"),
        // 88 Ra · Radio
        ElementDiscovery(1898, null, "Marie y Pierre Curie", "Francia"),
        // 89 Ac · Actinio
        ElementDiscovery(1899, null, "André-Louis Debierne", "Francia"),
        // 90 Th · Torio
        ElementDiscovery(1829, null, "Jöns Jacob Berzelius", "Suecia"),
        // 91 Pa · Protactinio
        ElementDiscovery(1913, null, "Kasimir Fajans y Oswald Göhring", "Alemania"),
        // 92 U · Uranio
        ElementDiscovery(1789, null, "Martin Heinrich Klaproth", "Alemania"),
        // 93 Np · Neptunio
        ElementDiscovery(1940, null, "Edwin McMillan y Philip Abelson", "Estados Unidos"),
        // 94 Pu · Plutonio
        ElementDiscovery(1940, null, "Seaborg, McMillan, Kennedy y Wahl", "Estados Unidos"),
        // 95 Am · Americio
        ElementDiscovery(1944, null, "Seaborg, James, Morgan y Ghiorso", "Estados Unidos"),
        // 96 Cm · Curio
        ElementDiscovery(1944, null, "Seaborg, James y Ghiorso", "Estados Unidos"),
        // 97 Bk · Berkelio
        ElementDiscovery(1949, null, "Thompson, Ghiorso y Seaborg", "Estados Unidos"),
        // 98 Cf · Californio
        ElementDiscovery(1950, null, "Thompson, Street, Ghiorso y Seaborg", "Estados Unidos"),
        // 99 Es · Einstenio
        ElementDiscovery(1952, null, "Equipo de Albert Ghiorso (prueba Ivy Mike)", "Estados Unidos"),
        // 100 Fm · Fermio
        ElementDiscovery(1952, null, "Equipo de Albert Ghiorso (prueba Ivy Mike)", "Estados Unidos"),
        // 101 Md · Mendelevio
        ElementDiscovery(1955, null, "Ghiorso, Harvey, Choppin, Thompson y Seaborg", "Estados Unidos"),
        // 102 No · Nobelio
        ElementDiscovery(1966, null, "Equipo del JINR (Dubná)", "Rusia"),
        // 103 Lr · Lawrencio
        ElementDiscovery(1961, null, "Equipo de Albert Ghiorso (Berkeley)", "Estados Unidos"),
        // 104 Rf · Rutherfordio
        ElementDiscovery(1964, null, JINR_LBL, RUSSIA_USA),
        // 105 Db · Dubnio
        ElementDiscovery(1968, null, JINR_LBL, RUSSIA_USA),
        // 106 Sg · Seaborgio
        ElementDiscovery(1974, null, "Equipo de Albert Ghiorso (LBL, Berkeley)", "Estados Unidos"),
        // 107 Bh · Bohrio
        ElementDiscovery(1981, null, GSI_ARMBRUSTER, "Alemania"),
        // 108 Hs · Hasio
        ElementDiscovery(1984, null, GSI_ARMBRUSTER, "Alemania"),
        // 109 Mt · Meitnerio
        ElementDiscovery(1982, null, GSI_ARMBRUSTER, "Alemania"),
        // 110 Ds · Darmstatio
        ElementDiscovery(1994, null, GSI_HOFMANN, "Alemania"),
        // 111 Rg · Roentgenio
        ElementDiscovery(1994, null, GSI_HOFMANN, "Alemania"),
        // 112 Cn · Copernicio
        ElementDiscovery(1996, null, GSI_HOFMANN, "Alemania"),
        // 113 Nh · Nihonio
        ElementDiscovery(2004, null, "Equipo del RIKEN (Kosuke Morita)", "Japón"),
        // 114 Fl · Flerovio
        ElementDiscovery(1999, null, JINR_LLNL, RUSSIA_USA),
        // 115 Mc · Moscovio
        ElementDiscovery(2003, null, JINR_LLNL, RUSSIA_USA),
        // 116 Lv · Livermorio
        ElementDiscovery(2000, null, JINR_LLNL, RUSSIA_USA),
        // 117 Ts · Teneso
        ElementDiscovery(2010, null, "JINR, ORNL, Vanderbilt y LLNL", RUSSIA_USA),
        // 118 Og · Oganesón
        ElementDiscovery(2002, null, JINR_LLNL, RUSSIA_USA),
    )

    init {
        check(list.size == PeriodicTable.SIZE)
    }
}
