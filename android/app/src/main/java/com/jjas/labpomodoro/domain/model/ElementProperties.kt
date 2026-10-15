package com.jjas.labpomodoro.domain.model

/**
 * Estado del elemento a temperatura ambiente; los *_PREDICTED son los superpesados que nadie ha
 * podido observar en cantidad suficiente: su estado es una predicción.
 */
enum class ElementState { SOLID, LIQUID, GAS, SOLID_PREDICTED, LIQUID_PREDICTED, GAS_PREDICTED }

/**
 * Propiedades físicas de un elemento para su ficha.
 *
 * @param atomicMass masa atómica (u); en los sintéticos, la del isótopo más estable.
 * @param electronConfiguration configuración electrónica abreviada ("[He]2s2 2p2").
 * @param configurationPredicted true si la configuración es teórica (superpesados).
 * @param meltingKelvin, boilingKelvin puntos de fusión y ebullición en kelvin; null si no se han medido.
 * @param density g/cm³ (en los gases, a 0 °C y 1 atm); null si no se ha medido.
 * @param electronegativity escala de Pauling; null en los gases nobles ligeros y los que no la tienen medida.
 */
data class ElementProperties(
    val atomicMass: Double,
    val electronConfiguration: String,
    val configurationPredicted: Boolean,
    val state: ElementState,
    val meltingKelvin: Double?,
    val boilingKelvin: Double?,
    val density: Double?,
    val electronegativity: Double?,
)

private typealias P = ElementProperties

/**
 * Datos de la tabla periódica de PubChem (NIH, dominio público). Índice = número atómico − 1.
 * Archivo generado desde https://pubchem.ncbi.nlm.nih.gov/rest/pug/periodictable/CSV.
 */
object ElementPropertiesTable {

    fun of(atomicNumber: Int): ElementProperties = all[atomicNumber - 1]

    private val all: List<ElementProperties> = listOf(
        P(1.0080, "1s1", false, ElementState.GAS, 13.81, 20.28, 0.00008988, 2.2), // 1 H
        P(4.00260, "1s2", false, ElementState.GAS, 0.95, 4.22, 0.0001785, null), // 2 He
        P(7.0, "[He]2s1", false, ElementState.SOLID, 453.65, 1615.0, 0.534, 0.98), // 3 Li
        P(9.012183, "[He]2s2", false, ElementState.SOLID, 1560.0, 2744.0, 1.85, 1.57), // 4 Be
        P(10.81, "[He]2s2 2p1", false, ElementState.SOLID, 2348.0, 4273.0, 2.37, 2.04), // 5 B
        P(12.011, "[He]2s2 2p2", false, ElementState.SOLID, 3823.0, 4098.0, 2.2670, 2.55), // 6 C
        P(14.007, "[He]2s2 2p3", false, ElementState.GAS, 63.15, 77.36, 0.0012506, 3.04), // 7 N
        P(15.999, "[He]2s2 2p4", false, ElementState.GAS, 54.36, 90.2, 0.001429, 3.44), // 8 O
        P(18.99840316, "[He]2s2 2p5", false, ElementState.GAS, 53.53, 85.03, 0.001696, 3.98), // 9 F
        P(20.180, "[He]2s2 2p6", false, ElementState.GAS, 24.56, 27.07, 0.0008999, null), // 10 Ne
        P(22.9897693, "[Ne]3s1", false, ElementState.SOLID, 370.95, 1156.0, 0.97, 0.93), // 11 Na
        P(24.305, "[Ne]3s2", false, ElementState.SOLID, 923.0, 1363.0, 1.74, 1.31), // 12 Mg
        P(26.981538, "[Ne]3s2 3p1", false, ElementState.SOLID, 933.437, 2792.0, 2.70, 1.61), // 13 Al
        P(28.085, "[Ne]3s2 3p2", false, ElementState.SOLID, 1687.0, 3538.0, 2.3296, 1.9), // 14 Si
        P(30.97376200, "[Ne]3s2 3p3", false, ElementState.SOLID, 317.3, 553.65, 1.82, 2.19), // 15 P
        P(32.07, "[Ne]3s2 3p4", false, ElementState.SOLID, 388.36, 717.75, 2.067, 2.58), // 16 S
        P(35.45, "[Ne]3s2 3p5", false, ElementState.GAS, 171.65, 239.11, 0.003214, 3.16), // 17 Cl
        P(39.9, "[Ne]3s2 3p6", false, ElementState.GAS, 83.8, 87.3, 0.0017837, null), // 18 Ar
        P(39.0983, "[Ar]4s1", false, ElementState.SOLID, 336.53, 1032.0, 0.89, 0.82), // 19 K
        P(40.08, "[Ar]4s2", false, ElementState.SOLID, 1115.0, 1757.0, 1.54, 1.0), // 20 Ca
        P(44.95591, "[Ar]4s2 3d1", false, ElementState.SOLID, 1814.0, 3109.0, 2.99, 1.36), // 21 Sc
        P(47.867, "[Ar]4s2 3d2", false, ElementState.SOLID, 1941.0, 3560.0, 4.5, 1.54), // 22 Ti
        P(50.9415, "[Ar]4s2 3d3", false, ElementState.SOLID, 2183.0, 3680.0, 6.0, 1.63), // 23 V
        P(51.996, "[Ar]3d5 4s1", false, ElementState.SOLID, 2180.0, 2944.0, 7.15, 1.66), // 24 Cr
        P(54.93804, "[Ar]4s2 3d5", false, ElementState.SOLID, 1519.0, 2334.0, 7.3, 1.55), // 25 Mn
        P(55.84, "[Ar]4s2 3d6", false, ElementState.SOLID, 1811.0, 3134.0, 7.874, 1.83), // 26 Fe
        P(58.93319, "[Ar]4s2 3d7", false, ElementState.SOLID, 1768.0, 3200.0, 8.86, 1.88), // 27 Co
        P(58.693, "[Ar]4s2 3d8", false, ElementState.SOLID, 1728.0, 3186.0, 8.912, 1.91), // 28 Ni
        P(63.55, "[Ar]4s1 3d10", false, ElementState.SOLID, 1357.77, 2835.0, 8.933, 1.9), // 29 Cu
        P(65.4, "[Ar]4s2 3d10", false, ElementState.SOLID, 692.68, 1180.0, 7.134, 1.65), // 30 Zn
        P(69.723, "[Ar]4s2 3d10 4p1", false, ElementState.SOLID, 302.91, 2477.0, 5.91, 1.81), // 31 Ga
        P(72.63, "[Ar]4s2 3d10 4p2", false, ElementState.SOLID, 1211.4, 3106.0, 5.323, 2.01), // 32 Ge
        P(74.92159, "[Ar]4s2 3d10 4p3", false, ElementState.SOLID, 1090.0, 887.0, 5.776, 2.18), // 33 As
        P(78.97, "[Ar]4s2 3d10 4p4", false, ElementState.SOLID, 493.65, 958.0, 4.809, 2.55), // 34 Se
        P(79.90, "[Ar]4s2 3d10 4p5", false, ElementState.LIQUID, 265.95, 331.95, 3.11, 2.96), // 35 Br
        P(83.80, "[Ar]4s2 3d10 4p6", false, ElementState.GAS, 115.79, 119.93, 0.003733, 3.0), // 36 Kr
        P(85.468, "[Kr]5s1", false, ElementState.SOLID, 312.46, 961.0, 1.53, 0.82), // 37 Rb
        P(87.62, "[Kr]5s2", false, ElementState.SOLID, 1050.0, 1655.0, 2.64, 0.95), // 38 Sr
        P(88.90584, "[Kr]5s2 4d1", false, ElementState.SOLID, 1795.0, 3618.0, 4.47, 1.22), // 39 Y
        P(91.22, "[Kr]5s2 4d2", false, ElementState.SOLID, 2128.0, 4682.0, 6.52, 1.33), // 40 Zr
        P(92.90637, "[Kr]5s1 4d4", false, ElementState.SOLID, 2750.0, 5017.0, 8.57, 1.6), // 41 Nb
        P(95.95, "[Kr]5s1 4d5", false, ElementState.SOLID, 2896.0, 4912.0, 10.2, 2.16), // 42 Mo
        P(96.90636, "[Kr]5s2 4d5", false, ElementState.SOLID, 2430.0, 4538.0, 11.0, 1.9), // 43 Tc
        P(101.1, "[Kr]5s1 4d7", false, ElementState.SOLID, 2607.0, 4423.0, 12.1, 2.2), // 44 Ru
        P(102.9055, "[Kr]5s1 4d8", false, ElementState.SOLID, 2237.0, 3968.0, 12.4, 2.28), // 45 Rh
        P(106.42, "[Kr]4d10", false, ElementState.SOLID, 1828.05, 3236.0, 12.0, 2.2), // 46 Pd
        P(107.868, "[Kr]5s1 4d10", false, ElementState.SOLID, 1234.93, 2435.0, 10.501, 1.93), // 47 Ag
        P(112.41, "[Kr]5s2 4d10", false, ElementState.SOLID, 594.22, 1040.0, 8.69, 1.69), // 48 Cd
        P(114.818, "[Kr]5s2 4d10 5p1", false, ElementState.SOLID, 429.75, 2345.0, 7.31, 1.78), // 49 In
        P(118.71, "[Kr]5s2 4d10 5p2", false, ElementState.SOLID, 505.08, 2875.0, 7.287, 1.96), // 50 Sn
        P(121.760, "[Kr]5s2 4d10 5p3", false, ElementState.SOLID, 903.78, 1860.0, 6.685, 2.05), // 51 Sb
        P(127.6, "[Kr]5s2 4d10 5p4", false, ElementState.SOLID, 722.66, 1261.0, 6.232, 2.1), // 52 Te
        P(126.9045, "[Kr]5s2 4d10 5p5", false, ElementState.SOLID, 386.85, 457.55, 4.93, 2.66), // 53 I
        P(131.29, "[Kr]5s2 4d10 5p6", false, ElementState.GAS, 161.36, 165.03, 0.005887, 2.6), // 54 Xe
        P(132.9054520, "[Xe]6s1", false, ElementState.SOLID, 301.59, 944.0, 1.93, 0.79), // 55 Cs
        P(137.33, "[Xe]6s2", false, ElementState.SOLID, 1000.0, 2170.0, 3.62, 0.89), // 56 Ba
        P(138.9055, "[Xe]6s2 5d1", false, ElementState.SOLID, 1191.0, 3737.0, 6.15, 1.1), // 57 La
        P(140.116, "[Xe]6s2 4f1 5d1", false, ElementState.SOLID, 1071.0, 3697.0, 6.770, 1.12), // 58 Ce
        P(140.90766, "[Xe]6s2 4f3", false, ElementState.SOLID, 1204.0, 3793.0, 6.77, 1.13), // 59 Pr
        P(144.24, "[Xe]6s2 4f4", false, ElementState.SOLID, 1294.0, 3347.0, 7.01, 1.14), // 60 Nd
        P(144.91276, "[Xe]6s2 4f5", false, ElementState.SOLID, 1315.0, 3273.0, 7.26, null), // 61 Pm
        P(150.4, "[Xe]6s2 4f6", false, ElementState.SOLID, 1347.0, 2067.0, 7.52, 1.17), // 62 Sm
        P(151.964, "[Xe]6s2 4f7", false, ElementState.SOLID, 1095.0, 1802.0, 5.24, null), // 63 Eu
        P(157.25, "[Xe]6s2 4f7 5d1", false, ElementState.SOLID, 1586.0, 3546.0, 7.90, 1.2), // 64 Gd
        P(158.92535, "[Xe]6s2 4f9", false, ElementState.SOLID, 1629.0, 3503.0, 8.23, null), // 65 Tb
        P(162.500, "[Xe]6s2 4f10", false, ElementState.SOLID, 1685.0, 2840.0, 8.55, 1.22), // 66 Dy
        P(164.93033, "[Xe]6s2 4f11", false, ElementState.SOLID, 1747.0, 2973.0, 8.80, 1.23), // 67 Ho
        P(167.26, "[Xe]6s2 4f12", false, ElementState.SOLID, 1802.0, 3141.0, 9.07, 1.24), // 68 Er
        P(168.93422, "[Xe]6s2 4f13", false, ElementState.SOLID, 1818.0, 2223.0, 9.32, 1.25), // 69 Tm
        P(173.05, "[Xe]6s2 4f14", false, ElementState.SOLID, 1092.0, 1469.0, 6.90, null), // 70 Yb
        P(174.9667, "[Xe]6s2 4f14 5d1", false, ElementState.SOLID, 1936.0, 3675.0, 9.84, 1.27), // 71 Lu
        P(178.49, "[Xe]6s2 4f14 5d2", false, ElementState.SOLID, 2506.0, 4876.0, 13.3, 1.3), // 72 Hf
        P(180.9479, "[Xe]6s2 4f14 5d3", false, ElementState.SOLID, 3290.0, 5731.0, 16.4, 1.5), // 73 Ta
        P(183.84, "[Xe]6s2 4f14 5d4", false, ElementState.SOLID, 3695.0, 5828.0, 19.3, 2.36), // 74 W
        P(186.207, "[Xe]6s2 4f14 5d5", false, ElementState.SOLID, 3459.0, 5869.0, 20.8, 1.9), // 75 Re
        P(190.2, "[Xe]6s2 4f14 5d6", false, ElementState.SOLID, 3306.0, 5285.0, 22.57, 2.2), // 76 Os
        P(192.22, "[Xe]6s2 4f14 5d7", false, ElementState.SOLID, 2719.0, 4701.0, 22.42, 2.2), // 77 Ir
        P(195.08, "[Xe]6s1 4f14 5d9", false, ElementState.SOLID, 2041.55, 4098.0, 21.46, 2.28), // 78 Pt
        P(196.96657, "[Xe]6s1 4f14 5d10", false, ElementState.SOLID, 1337.33, 3129.0, 19.282, 2.54), // 79 Au
        P(200.59, "[Xe]6s2 4f14 5d10", false, ElementState.LIQUID, 234.32, 629.88, 13.5336, 2.0), // 80 Hg
        P(204.383, "[Xe]6s2 4f14 5d10 6p1", false, ElementState.SOLID, 577.0, 1746.0, 11.8, 1.62), // 81 Tl
        P(207.0, "[Xe]6s2 4f14 5d10 6p2", false, ElementState.SOLID, 600.61, 2022.0, 11.342, 2.33), // 82 Pb
        P(208.98040, "[Xe]6s2 4f14 5d10 6p3", false, ElementState.SOLID, 544.55, 1837.0, 9.807, 2.02), // 83 Bi
        P(208.98243, "[Xe]6s2 4f14 5d10 6p4", false, ElementState.SOLID, 527.0, 1235.0, 9.32, 2.0), // 84 Po
        P(209.98715, "[Xe]6s2 4f14 5d10 6p5", false, ElementState.SOLID, 575.0, null, 7.0, 2.2), // 85 At
        P(222.01758, "[Xe]6s2 4f14 5d10 6p6", false, ElementState.GAS, 202.0, 211.45, 0.00973, null), // 86 Rn
        P(223.01973, "[Rn]7s1", false, ElementState.SOLID, 300.0, null, null, 0.7), // 87 Fr
        P(226.02541, "[Rn]7s2", false, ElementState.SOLID, 973.0, 1413.0, 5.0, 0.9), // 88 Ra
        P(227.02775, "[Rn]7s2 6d1", false, ElementState.SOLID, 1324.0, 3471.0, 10.07, 1.1), // 89 Ac
        P(232.038, "[Rn]7s2 6d2", false, ElementState.SOLID, 2023.0, 5061.0, 11.72, 1.3), // 90 Th
        P(231.03588, "[Rn]7s2 5f2 6d1", false, ElementState.SOLID, 1845.0, null, 15.37, 1.5), // 91 Pa
        P(238.0289, "[Rn]7s2 5f3 6d1", false, ElementState.SOLID, 1408.0, 4404.0, 18.95, 1.38), // 92 U
        P(237.048172, "[Rn]7s2 5f4 6d1", false, ElementState.SOLID, 917.0, 4175.0, 20.25, 1.36), // 93 Np
        P(244.06420, "[Rn]7s2 5f6", false, ElementState.SOLID, 913.0, 3501.0, 19.84, 1.28), // 94 Pu
        P(243.061380, "[Rn]7s2 5f7", false, ElementState.SOLID, 1449.0, 2284.0, 13.69, 1.3), // 95 Am
        P(247.07035, "[Rn]7s2 5f7 6d1", false, ElementState.SOLID, 1618.0, 3400.0, 13.51, 1.3), // 96 Cm
        P(247.07031, "[Rn]7s2 5f9", false, ElementState.SOLID, 1323.0, null, 14.0, 1.3), // 97 Bk
        P(251.07959, "[Rn]7s2 5f10", false, ElementState.SOLID, 1173.0, null, null, 1.3), // 98 Cf
        P(252.0830, "[Rn]7s2 5f11", false, ElementState.SOLID, 1133.0, null, null, 1.3), // 99 Es
        P(257.09511, "[Rn]5f12 7s2", false, ElementState.SOLID, 1800.0, null, null, 1.3), // 100 Fm
        P(258.09843, "[Rn]7s2 5f13", false, ElementState.SOLID, 1100.0, null, null, 1.3), // 101 Md
        P(259.10100, "[Rn]7s2 5f14", false, ElementState.SOLID, 1100.0, null, null, 1.3), // 102 No
        P(266.120, "[Rn]7s2 5f14 6d1", false, ElementState.SOLID, 1900.0, null, null, 1.3), // 103 Lr
        P(267.122, "[Rn]7s2 5f14 6d2", false, ElementState.SOLID, null, null, null, null), // 104 Rf
        P(268.126, "[Rn]7s2 5f14 6d3", false, ElementState.SOLID, null, null, null, null), // 105 Db
        P(269.128, "[Rn]7s2 5f14 6d4", false, ElementState.SOLID, null, null, null, null), // 106 Sg
        P(270.133, "[Rn]7s2 5f14 6d5", false, ElementState.SOLID, null, null, null, null), // 107 Bh
        P(269.1336, "[Rn]7s2 5f14 6d6", false, ElementState.SOLID, null, null, null, null), // 108 Hs
        P(277.154, "[Rn]7s2 5f14 6d7 (calculated)", false, ElementState.SOLID, null, null, null, null), // 109 Mt
        P(282.166, "[Rn]7s2 5f14 6d8", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 110 Ds
        P(282.169, "[Rn]7s2 5f14 6d9", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 111 Rg
        P(286.179, "[Rn]7s2 5f14 6d10", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 112 Cn
        P(286.182, "[Rn]5f14 6d10 7s2 7p1", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 113 Nh
        P(290.192, "[Rn]7s2 7p2 5f14 6d10", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 114 Fl
        P(290.196, "[Rn]7s2 7p3 5f14 6d10", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 115 Mc
        P(293.205, "[Rn]7s2 7p4 5f14 6d10", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 116 Lv
        P(294.211, "[Rn]7s2 7p5 5f14 6d10", true, ElementState.SOLID_PREDICTED, null, null, null, null), // 117 Ts
        P(295.216, "[Rn]7s2 7p6 5f14 6d10", true, ElementState.GAS_PREDICTED, null, null, null, null), // 118 Og
    )
}
