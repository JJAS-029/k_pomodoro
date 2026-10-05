package com.jjas.labpomodoro.domain.model

/**
 * Qué es cada elemento y para qué sirve en la vida real, para la ficha de la tabla periódica.
 * El índice es el número atómico − 1.
 */
data class ElementFact(
    /** Qué es: aspecto, origen o un dato curioso. */
    val description: String,
    /** Para qué sirve en la vida diaria o en la ciencia. */
    val uses: String,
)

object ElementFacts {

    operator fun get(atomicNumber: Int): ElementFact = facts[atomicNumber - 1]

    private val facts: List<ElementFact> = listOf(
        // 1 H · Hidrógeno
        ElementFact(
            description = "El elemento más abundante del universo: forma cerca del 75 % de la materia normal y alimenta a las estrellas.",
            uses = "Se usa para fabricar amoníaco para fertilizantes, refinar petróleo y como combustible limpio en celdas de hidrógeno.",
        ),
        // 2 He · Helio
        ElementFact(
            description = "Gas noble más ligero que el aire; se descubrió primero en la luz del Sol y años después en la Tierra.",
            uses = "Infla globos, enfría los imanes de los equipos de resonancia magnética y protege el metal al soldar.",
        ),
        // 3 Li · Litio
        ElementFact(
            description = "El metal más ligero de todos: es tan poco denso que flota en el agua, aunque reacciona con ella.",
            uses = "Es el corazón de las baterías de celulares, laptops y autos eléctricos; también se usa en medicamentos psiquiátricos.",
        ),
        // 4 Be · Berilio
        ElementFact(
            description = "Metal gris, muy ligero y rígido; su polvo es tóxico si se inhala.",
            uses = "Forma los espejos del telescopio espacial James Webb y se usa en resortes, herramientas antichispa y piezas aeroespaciales.",
        ),
        // 5 B · Boro
        ElementFact(
            description = "Metaloide oscuro y muy duro; su compuesto más famoso, el bórax, se usa desde hace siglos.",
            uses = "Está en el vidrio que resiste el calor (borosilicato), la fibra de vidrio, detergentes y los imanes de neodimio.",
        ),
        // 6 C · Carbono
        ElementFact(
            description = "La base de toda la vida conocida; según cómo se ordenan sus átomos puede ser grafito blando o diamante.",
            uses = "Está en la puntilla de los lápices, los diamantes, el acero, los combustibles y la fibra de carbono.",
        ),
        // 7 N · Nitrógeno
        ElementFact(
            description = "Gas incoloro y sin olor que forma cerca del 78 % del aire que respiras.",
            uses = "Se usa en fertilizantes, para conservar alimentos empacados y, líquido a -196 °C, para congelar muestras.",
        ),
        // 8 O · Oxígeno
        ElementFact(
            description = "Gas que forma el 21 % del aire y es el elemento más abundante de la corteza terrestre.",
            uses = "Lo respiramos casi todos los seres vivos; se usa en hospitales, en la fabricación de acero y en cohetes.",
        ),
        // 9 F · Flúor
        ElementFact(
            description = "Gas amarillo pálido y el elemento más reactivo de todos: reacciona con casi cualquier sustancia.",
            uses = "El fluoruro de la pasta dental protege tus dientes; también forma el teflón de los sartenes antiadherentes.",
        ),
        // 10 Ne · Neón
        ElementFact(
            description = "Gas noble que brilla con un intenso color rojo anaranjado cuando pasa electricidad por él.",
            uses = "Da nombre y color a los clásicos letreros luminosos de neón; también se usa en algunos láseres.",
        ),
        // 11 Na · Sodio
        ElementFact(
            description = "Metal tan blando que se corta con un cuchillo y reacciona violentamente con el agua.",
            uses = "Junto con el cloro forma la sal de mesa; sus lámparas dan la luz amarilla de algunos alumbrados públicos.",
        ),
        // 12 Mg · Magnesio
        ElementFact(
            description = "Metal ligero que arde con una luz blanca muy brillante; está en el centro de la clorofila de las plantas.",
            uses = "Se usa en aleaciones ligeras para autos y laptops, en bengalas y fuegos artificiales, y como suplemento.",
        ),
        // 13 Al · Aluminio
        ElementFact(
            description = "El metal más abundante de la corteza terrestre; en el siglo XIX llegó a valer más que el oro.",
            uses = "Está en latas de refresco, papel aluminio, ventanas, aviones y cables eléctricos; además se recicla muy bien.",
        ),
        // 14 Si · Silicio
        ElementFact(
            description = "Metaloide gris brillante; después del oxígeno, es el elemento más abundante de la corteza terrestre.",
            uses = "Es la base de los chips de computadoras y celulares y de los paneles solares; como sílice, forma el vidrio.",
        ),
        // 15 P · Fósforo
        ElementFact(
            description = "Su forma blanca brilla en la oscuridad y arde sola en el aire; también es parte de tu ADN y tus huesos.",
            uses = "Se usa en fertilizantes, en la tira para encender cerillos y en algunos detergentes y refrescos de cola.",
        ),
        // 16 S · Azufre
        ElementFact(
            description = "Sólido amarillo que se encuentra cerca de los volcanes; algunos de sus compuestos huelen a huevo podrido.",
            uses = "Se usa para fabricar ácido sulfúrico, vulcanizar el hule de las llantas, en la pólvora y en fungicidas.",
        ),
        // 17 Cl · Cloro
        ElementFact(
            description = "Gas amarillo verdoso y tóxico; unido al sodio se vuelve la inofensiva sal de mesa.",
            uses = "Desinfecta el agua potable y las albercas, está en los blanqueadores y se usa para fabricar PVC.",
        ),
        // 18 Ar · Argón
        ElementFact(
            description = "Gas noble que forma casi el 1 % del aire; su nombre viene del griego 'perezoso' porque casi no reacciona.",
            uses = "Llena los focos incandescentes, protege la soldadura y aísla el espacio entre los vidrios de ventanas dobles.",
        ),
        // 19 K · Potasio
        ElementFact(
            description = "Metal blando que arde en el agua con llama lila; tus nervios y músculos lo necesitan para funcionar.",
            uses = "Es clave en los fertilizantes (potasa); lo obtienes al comer plátanos, papas y frijoles.",
        ),
        // 20 Ca · Calcio
        ElementFact(
            description = "Metal plateado y reactivo; es el mineral más abundante de tu cuerpo, sobre todo en huesos y dientes.",
            uses = "Está en el cemento, la cal, el yeso y el gis; también en la leche y en suplementos para los huesos.",
        ),
        // 21 Sc · Escandio
        ElementFact(
            description = "Metal ligero y escaso cuya existencia predijo Mendeléyev antes de que se descubriera.",
            uses = "Pequeñas cantidades refuerzan el aluminio de cuadros de bicicleta, bates de béisbol y piezas de aviones.",
        ),
        // 22 Ti · Titanio
        ElementFact(
            description = "Metal tan fuerte como muchos aceros pero mucho más ligero, y muy resistente a la corrosión.",
            uses = "Se usa en aviones, implantes dentales y prótesis; su óxido es el pigmento blanco de pinturas y bloqueadores.",
        ),
        // 23 V · Vanadio
        ElementFact(
            description = "Metal gris y duro; su nombre viene de Vanadís, diosa nórdica de la belleza, por sus compuestos de colores.",
            uses = "Endurece el acero de herramientas y resortes; también se usa en baterías de flujo para guardar energía.",
        ),
        // 24 Cr · Cromo
        ElementFact(
            description = "Metal brillante y duro; pequeñas cantidades dan el verde a la esmeralda y el rojo al rubí.",
            uses = "Da el acabado brillante del cromado y hace que el acero inoxidable no se oxide.",
        ),
        // 25 Mn · Manganeso
        ElementFact(
            description = "Metal duro y quebradizo; plantas y animales lo necesitan en pequeñas cantidades.",
            uses = "Se agrega al acero para hacerlo más resistente y está en las pilas alcalinas comunes.",
        ),
        // 26 Fe · Hierro
        ElementFact(
            description = "El metal más usado por la humanidad; forma gran parte del núcleo de la Tierra.",
            uses = "Es la base del acero de edificios, autos y puentes; en tu sangre, la hemoglobina lo usa para llevar oxígeno.",
        ),
        // 27 Co · Cobalto
        ElementFact(
            description = "Metal duro y magnético; sus compuestos dan el intenso azul cobalto a vidrios y cerámica.",
            uses = "Está en baterías de litio, en superaleaciones para turbinas de avión y forma parte de la vitamina B12.",
        ),
        // 28 Ni · Níquel
        ElementFact(
            description = "Metal plateado y magnético, muy resistente a la corrosión; abunda en los meteoritos de hierro.",
            uses = "Se usa en el acero inoxidable, en baterías recargables y en muchas monedas.",
        ),
        // 29 Cu · Cobre
        ElementFact(
            description = "Metal rojizo usado desde hace más de 10 000 años; con el tiempo se pone verde, como la Estatua de la Libertad.",
            uses = "Está en casi todos los cables eléctricos, en tuberías y en aleaciones como el bronce y el latón.",
        ),
        // 30 Zn · Zinc
        ElementFact(
            description = "Metal gris azulado; tu cuerpo lo necesita para el sistema inmune y para sanar heridas.",
            uses = "Protege el acero de la oxidación (galvanizado) y está en pilas, en el latón y en bloqueadores solares.",
        ),
        // 31 Ga · Galio
        ElementFact(
            description = "Metal que se derrite en la palma de tu mano: su punto de fusión es de apenas unos 30 °C.",
            uses = "Con arsénico o nitrógeno forma LED, láseres y chips de alta frecuencia para celulares.",
        ),
        // 32 Ge · Germanio
        ElementFact(
            description = "Metaloide gris brillante predicho por Mendeléyev; fue clave en los primeros transistores.",
            uses = "Se usa en fibra óptica, lentes de cámaras infrarrojas y de visión nocturna, y en celdas solares de satélites.",
        ),
        // 33 As · Arsénico
        ElementFact(
            description = "Metaloide gris famoso como veneno en la historia y en las novelas de misterio.",
            uses = "Forma el arseniuro de galio de algunos chips y LED; antes protegía la madera, pero hoy se limita por tóxico.",
        ),
        // 34 Se · Selenio
        ElementFact(
            description = "No metal que conduce mejor la electricidad cuando le da la luz; su nombre viene de Selene, la Luna.",
            uses = "Quita el tono verdoso al vidrio, está en champús anticaspa y es un nutriente esencial en pequeñas dosis.",
        ),
        // 35 Br · Bromo
        ElementFact(
            description = "Uno de los dos únicos elementos líquidos a temperatura ambiente: rojo oscuro y de olor penetrante.",
            uses = "Se usa en retardantes de fuego, en desinfectantes para albercas y en algunos medicamentos.",
        ),
        // 36 Kr · Kriptón
        ElementFact(
            description = "Gas noble raro; su nombre significa 'oculto' en griego, y no, no viene del planeta de Superman.",
            uses = "Se usa en algunas lámparas de alto rendimiento, flashes fotográficos y ventanas aislantes.",
        ),
        // 37 Rb · Rubidio
        ElementFact(
            description = "Metal muy blando y reactivo que se enciende solo al contacto con el aire.",
            uses = "Se usa en relojes atómicos compactos, como algunos de los satélites GPS, y en investigación.",
        ),
        // 38 Sr · Estroncio
        ElementFact(
            description = "Metal blando y plateado que tiñe las llamas de un rojo intenso.",
            uses = "Da el rojo a fuegos artificiales y bengalas de emergencia; también se usa en imanes de ferrita.",
        ),
        // 39 Y · Itrio
        ElementFact(
            description = "Metal plateado nombrado por Ytterby, un pueblo sueco que dio nombre a cuatro elementos.",
            uses = "Se usa en LED blancos, láseres médicos (YAG) y superconductores; antes daba el rojo a las teles de tubo.",
        ),
        // 40 Zr · Circonio
        ElementFact(
            description = "Metal muy resistente a la corrosión; su óxido, la zirconia cúbica, imita a los diamantes.",
            uses = "Recubre el combustible de los reactores nucleares y se usa en coronas dentales y cuchillos de cerámica.",
        ),
        // 41 Nb · Niobio
        ElementFact(
            description = "Metal gris y suave que se vuelve superconductor cuando se enfría muchísimo.",
            uses = "Forma imanes superconductores de resonancias magnéticas y del Gran Colisionador de Hadrones, y refuerza aceros.",
        ),
        // 42 Mo · Molibdeno
        ElementFact(
            description = "Metal con uno de los puntos de fusión más altos (unos 2600 °C); muchas enzimas lo necesitan.",
            uses = "Endurece el acero de herramientas, tuberías y motores; como disulfuro, sirve de lubricante.",
        ),
        // 43 Tc · Tecnecio
        ElementFact(
            description = "El elemento más ligero sin ninguna forma estable; fue el primero creado artificialmente, en 1937.",
            uses = "Un isótopo suyo se usa en millones de estudios médicos al año para ver huesos, corazón y otros órganos.",
        ),
        // 44 Ru · Rutenio
        ElementFact(
            description = "Metal duro y raro de la familia del platino que casi no se oxida.",
            uses = "Se usa en contactos eléctricos, en discos duros, como catalizador y para endurecer el platino.",
        ),
        // 45 Rh · Rodio
        ElementFact(
            description = "Uno de los metales más raros y caros del mundo; es muy brillante y reflejante.",
            uses = "Está en los convertidores catalíticos de los autos para reducir gases tóxicos y recubre joyería.",
        ),
        // 46 Pd · Paladio
        ElementFact(
            description = "Metal plateado capaz de absorber cientos de veces su propio volumen de hidrógeno.",
            uses = "Se usa en convertidores catalíticos de autos, electrónica, odontología y joyería de oro blanco.",
        ),
        // 47 Ag · Plata
        ElementFact(
            description = "El mejor conductor de electricidad y calor de todos los elementos.",
            uses = "Se usa en joyería, monedas, espejos, contactos electrónicos y paneles solares; también en apósitos antibacterianos.",
        ),
        // 48 Cd · Cadmio
        ElementFact(
            description = "Metal blando, azulado y muy tóxico que se acumula en el cuerpo.",
            uses = "Se usó en pilas de níquel-cadmio y pigmentos amarillos; hoy está en algunos paneles solares de película delgada.",
        ),
        // 49 In · Indio
        ElementFact(
            description = "Metal tan blando que se raya con la uña y que cruje, como un pequeño grito, al doblarlo.",
            uses = "Unido al estaño (ITO) hace que las pantallas táctiles conduzcan electricidad sin dejar de ser transparentes.",
        ),
        // 50 Sn · Estaño
        ElementFact(
            description = "Metal plateado y maleable conocido desde la Edad del Bronce; también cruje al doblarlo.",
            uses = "Se usa en la soldadura de la electrónica, para recubrir latas de acero (hojalata) y en el bronce.",
        ),
        // 51 Sb · Antimonio
        ElementFact(
            description = "Metaloide gris brillante; en el antiguo Egipto su mineral se usaba para delinear los ojos.",
            uses = "Se usa como retardante de fuego en plásticos y telas, y para endurecer el plomo de las baterías de auto.",
        ),
        // 52 Te · Telurio
        ElementFact(
            description = "Metaloide raro y frágil; quien lo manipula puede terminar con un aliento que huele a ajo.",
            uses = "Se usa en paneles solares de película delgada (teluro de cadmio) y en dispositivos termoeléctricos.",
        ),
        // 53 I · Yodo
        ElementFact(
            description = "Sólido negro brillante que, al calentarse, se convierte directamente en un vapor violeta.",
            uses = "Se agrega a la sal de mesa para cuidar la tiroides y se usa como antiséptico para heridas.",
        ),
        // 54 Xe · Xenón
        ElementFact(
            description = "Gas noble pesado y raro que brilla con una luz blanca azulada en lámparas de descarga.",
            uses = "Está en faros de autos, flashes y proyectores de cine; también es anestésico y propulsa satélites con motores iónicos.",
        ),
        // 55 Cs · Cesio
        ElementFact(
            description = "Metal dorado tan blando que se derrite a 28 °C y explota al contacto con el agua.",
            uses = "Sus relojes atómicos definen el segundo oficial; también se usa en fluidos para perforar pozos petroleros.",
        ),
        // 56 Ba · Bario
        ElementFact(
            description = "Metal plateado y reactivo que tiñe las llamas de color verde.",
            uses = "Da el verde a los fuegos artificiales; el sulfato de bario se bebe para ver el aparato digestivo en rayos X.",
        ),
        // 57 La · Lantano
        ElementFact(
            description = "Metal blando y plateado que inicia la serie de los lantánidos, parte de las llamadas tierras raras.",
            uses = "Se usa en lentes de cámaras de alta calidad, en baterías de autos híbridos y en piedras de encendedor.",
        ),
        // 58 Ce · Cerio
        ElementFact(
            description = "La tierra rara más abundante; al rasparlo suelta chispas.",
            uses = "Forma las piedras de los encendedores, pule vidrio y lentes, y ayuda en los convertidores catalíticos.",
        ),
        // 59 Pr · Praseodimio
        ElementFact(
            description = "Metal plateado de las tierras raras; sus compuestos tienen un bonito color verde.",
            uses = "Se usa junto al neodimio en imanes potentes, en gafas para sopladores de vidrio y para dar color amarillo al vidrio.",
        ),
        // 60 Nd · Neodimio
        ElementFact(
            description = "Metal de las tierras raras que forma los imanes permanentes más potentes que existen.",
            uses = "Sus imanes están en audífonos, bocinas, discos duros, motores de autos eléctricos y aerogeneradores.",
        ),
        // 61 Pm · Prometio
        ElementFact(
            description = "Lantánido radiactivo casi inexistente en la naturaleza; lo que se usa se obtiene en reactores nucleares.",
            uses = "Se ha usado en pintura luminosa y en pequeñas baterías nucleares para marcapasos y naves espaciales.",
        ),
        // 62 Sm · Samario
        ElementFact(
            description = "Metal de las tierras raras que debe su nombre a la samarskita, el mineral donde se encontró.",
            uses = "Sus imanes de samario-cobalto aguantan altas temperaturas; también se usa para aliviar el dolor del cáncer de hueso.",
        ),
        // 63 Eu · Europio
        ElementFact(
            description = "La tierra rara más reactiva; su nombre viene de Europa.",
            uses = "Da brillo rojo y azul a pantallas y focos ahorradores, y su tinta fluorescente protege los billetes de euro.",
        ),
        // 64 Gd · Gadolinio
        ElementFact(
            description = "Metal de las tierras raras que solo es magnético por debajo de unos 20 °C, casi a temperatura ambiente.",
            uses = "Se inyecta como medio de contraste en resonancias magnéticas y absorbe neutrones en reactores nucleares.",
        ),
        // 65 Tb · Terbio
        ElementFact(
            description = "Metal plateado de las tierras raras, otro de los elementos nombrados por el pueblo sueco de Ytterby.",
            uses = "Da el color verde a pantallas y focos fluorescentes, y se usa en materiales que cambian de forma con un imán.",
        ),
        // 66 Dy · Disprosio
        ElementFact(
            description = "Su nombre viene del griego 'difícil de conseguir', porque costó mucho trabajo aislarlo.",
            uses = "Se añade a los imanes de neodimio para que aguanten el calor en motores de autos eléctricos y aerogeneradores.",
        ),
        // 67 Ho · Holmio
        ElementFact(
            description = "Tiene uno de los magnetismos atómicos más fuertes; su nombre viene de Holmia, Estocolmo en latín.",
            uses = "Se usa en láseres médicos para romper cálculos renales y en imanes de laboratorio muy potentes.",
        ),
        // 68 Er · Erbio
        ElementFact(
            description = "Tierra rara de compuestos rosados; también debe su nombre al pueblo sueco de Ytterby.",
            uses = "Amplifica la señal en la fibra óptica de internet, da color rosa al vidrio y se usa en láseres dermatológicos.",
        ),
        // 69 Tm · Tulio
        ElementFact(
            description = "Una de las tierras raras menos abundantes; su nombre viene de Thule, antiguo nombre del norte de Europa.",
            uses = "Se usa en equipos portátiles de rayos X y en láseres médicos y quirúrgicos.",
        ),
        // 70 Yb · Iterbio
        ElementFact(
            description = "Metal blando y plateado de las tierras raras, el cuarto elemento nombrado por Ytterby.",
            uses = "Se usa en algunos de los relojes atómicos más precisos del mundo y en láseres industriales.",
        ),
        // 71 Lu · Lutecio
        ElementFact(
            description = "El último de los lantánidos; su nombre viene de Lutecia, el antiguo nombre de París.",
            uses = "Se usa en detectores de tomografía PET y, en forma radiactiva, en terapias contra algunos cánceres.",
        ),
        // 72 Hf · Hafnio
        ElementFact(
            description = "Metal brillante que siempre aparece junto al circonio y es muy difícil de separar de él.",
            uses = "Absorbe neutrones en las barras de control nucleares y es parte de los chips de procesadores modernos.",
        ),
        // 73 Ta · Tantalio
        ElementFact(
            description = "Metal gris azulado muy resistente a la corrosión; su nombre viene de Tántalo, personaje de la mitología griega.",
            uses = "Sus diminutos capacitores están en celulares y computadoras; también se usa en implantes quirúrgicos.",
        ),
        // 74 W · Wolframio
        ElementFact(
            description = "El metal con el punto de fusión más alto de todos: unos 3400 °C. También se le llama tungsteno.",
            uses = "Forma el filamento de los focos clásicos, las bolitas de los bolígrafos, brocas y herramientas de corte.",
        ),
        // 75 Re · Renio
        ElementFact(
            description = "Uno de los elementos más raros de la corteza terrestre y el último elemento estable en descubrirse (1925).",
            uses = "Se usa en superaleaciones para turbinas de avión y como catalizador para hacer gasolina de alto octanaje.",
        ),
        // 76 Os · Osmio
        ElementFact(
            description = "El elemento natural más denso: un litro de osmio pesaría más de 22 kg.",
            uses = "Se usa en puntas de plumas fuente y contactos eléctricos, y sus compuestos tiñen muestras para microscopios.",
        ),
        // 77 Ir · Iridio
        ElementFact(
            description = "Metal muy resistente a la corrosión; una capa rica en iridio marca el impacto que extinguió a los dinosaurios.",
            uses = "Se usa en bujías de alto rendimiento, puntas de plumas fuente y crisoles para fabricar cristales.",
        ),
        // 78 Pt · Platino
        ElementFact(
            description = "Metal precioso, muy denso y casi inalterable; es más raro que el oro.",
            uses = "Está en convertidores catalíticos, joyería, electrodos, marcapasos y medicamentos contra el cáncer (cisplatino).",
        ),
        // 79 Au · Oro
        ElementFact(
            description = "Metal amarillo que nunca se oxida; se forma en explosiones y choques de estrellas.",
            uses = "Se usa en joyería, monedas y reservas de los bancos, y en los contactos de celulares y computadoras.",
        ),
        // 80 Hg · Mercurio
        ElementFact(
            description = "El único metal líquido a temperatura ambiente; es plateado y sus vapores son tóxicos.",
            uses = "Se usaba en termómetros y barómetros; aún está en algunos focos ahorradores, pero su uso se limita por tóxico.",
        ),
        // 81 Tl · Talio
        ElementFact(
            description = "Metal blando y muy tóxico, apodado 'el veneno de los envenenadores' porque no tiene sabor ni color.",
            uses = "Se usa en algunos estudios de imagen del corazón y en electrónica; antes se usaba como veneno para ratas.",
        ),
        // 82 Pb · Plomo
        ElementFact(
            description = "Metal pesado y blando que los romanos usaban en tuberías; es tóxico, sobre todo para los niños.",
            uses = "Está en las baterías de los autos y en los mandiles que protegen de los rayos X; ya no se usa en gasolina.",
        ),
        // 83 Bi · Bismuto
        ElementFact(
            description = "Metal frágil que forma cristales escalonados con colores de arcoíris; es poco tóxico pese a ser tan pesado.",
            uses = "Es el ingrediente activo de algunos medicamentos para el estómago y reemplaza al plomo en soldaduras.",
        ),
        // 84 Po · Polonio
        ElementFact(
            description = "Elemento muy radiactivo descubierto por Marie y Pierre Curie, nombrado en honor a Polonia, patria de Marie.",
            uses = "Se usa en dispositivos que eliminan la estática en fábricas y fue fuente de calor en algunas naves espaciales.",
        ),
        // 85 At · Astato
        ElementFact(
            description = "Uno de los elementos naturales más raros: en toda la corteza terrestre hay menos de un gramo a la vez.",
            uses = "Solo se usa en investigación, sobre todo para estudiar terapias contra el cáncer con partículas alfa.",
        ),
        // 86 Rn · Radón
        ElementFact(
            description = "Gas noble radiactivo, invisible y sin olor, que sale de ciertos suelos y puede acumularse en sótanos.",
            uses = "Se mide en casas por su riesgo para la salud; se ha usado en radioterapia y para estudiar aguas subterráneas.",
        ),
        // 87 Fr · Francio
        ElementFact(
            description = "Descubierto en Francia en 1939; es tan inestable que en la corteza terrestre hay apenas unos gramos a la vez.",
            uses = "Solo se usa en investigación, para estudiar con láseres la estructura de los átomos.",
        ),
        // 88 Ra · Radio
        ElementFact(
            description = "Brilla débilmente en la oscuridad; lo descubrieron los Curie y enfermó a las obreras conocidas como 'chicas del radio'.",
            uses = "Antes se usaba en pinturas luminosas de relojes; hoy un isótopo se usa para tratar el cáncer de próstata.",
        ),
        // 89 Ac · Actinio
        ElementFact(
            description = "Metal radiactivo que brilla con luz azul pálida en la oscuridad; da nombre a la serie de los actínidos.",
            uses = "El actinio-225 se estudia en terapias contra el cáncer; también sirve como fuente de neutrones en investigación.",
        ),
        // 90 Th · Torio
        ElementFact(
            description = "Metal radiactivo unas tres veces más abundante que el uranio; su nombre viene de Thor, dios nórdico del trueno.",
            uses = "Se usó en camisas de lámparas de gas y lentes de cámaras; hoy se estudia como combustible nuclear.",
        ),
        // 91 Pa · Protactinio
        ElementFact(
            description = "Metal radiactivo muy raro y tóxico; su nombre significa 'antes del actinio' porque se transforma en él.",
            uses = "Solo se usa en investigación, por ejemplo para fechar sedimentos marinos de miles de años.",
        ),
        // 92 U · Uranio
        ElementFact(
            description = "Metal radiactivo y muy denso, nombrado por el planeta Urano, que se había descubierto pocos años antes.",
            uses = "Es el combustible de las centrales nucleares, que generan cerca del 9 % de la electricidad del mundo.",
        ),
        // 93 Np · Neptunio
        ElementFact(
            description = "El primer elemento más pesado que el uranio en crearse (1940); se llama así por Neptuno, el planeta tras Urano.",
            uses = "Se usa para producir plutonio-238 para naves espaciales y en detectores de neutrones.",
        ),
        // 94 Pu · Plutonio
        ElementFact(
            description = "Metal radiactivo nombrado por Plutón; cambia de forma y densidad con la temperatura como pocos metales.",
            uses = "Se usa en reactores y armas nucleares; el plutonio-238 da energía a sondas como las Voyager y el rover Curiosity.",
        ),
        // 95 Am · Americio
        ElementFact(
            description = "Elemento sintético obtenido en reactores; se llama así por América, igual que el europio por Europa.",
            uses = "Una pizca de americio-241 está en muchos detectores de humo caseros y ayuda a salvar vidas.",
        ),
        // 96 Cm · Curio
        ElementFact(
            description = "Elemento sintético nombrado en honor a Marie y Pierre Curie; brilla en la oscuridad por su radiactividad.",
            uses = "Es la fuente de partículas alfa de instrumentos de los rovers en Marte que analizan rocas.",
        ),
        // 97 Bk · Berkelio
        ElementFact(
            description = "Elemento sintético creado en 1949 en Berkeley, California, ciudad de la que toma su nombre.",
            uses = "Solo se usa en investigación, sobre todo como blanco para fabricar elementos más pesados, como el teneso.",
        ),
        // 98 Cf · Californio
        ElementFact(
            description = "Creado en 1950 en la Universidad de California; un solo microgramo emite millones de neutrones por segundo.",
            uses = "Sus neutrones ayudan a arrancar reactores nucleares, buscar petróleo y oro, y detectar explosivos.",
        ),
        // 99 Es · Einstenio
        ElementFact(
            description = "Se descubrió en 1952 en los restos de la primera bomba de hidrógeno; su nombre honra a Albert Einstein.",
            uses = "Solo se usa en investigación: se ha producido en cantidades tan pequeñas que apenas se pueden ver.",
        ),
        // 100 Fm · Fermio
        ElementFact(
            description = "También apareció en los restos de la primera bomba de hidrógeno; su nombre honra a Enrico Fermi.",
            uses = "No tiene usos prácticos: es el elemento más pesado que puede formarse en un reactor y solo se estudia.",
        ),
        // 101 Md · Mendelevio
        ElementFact(
            description = "Honra a Mendeléyev, creador de la tabla periódica; en 1955 se lograron crear apenas 17 átomos.",
            uses = "Solo se usa en investigación, para entender cómo se comportan los átomos muy pesados.",
        ),
        // 102 No · Nobelio
        ElementFact(
            description = "Nombrado por Alfred Nobel; su descubrimiento lo disputaron equipos de Suecia, Estados Unidos y la Unión Soviética.",
            uses = "No tiene usos fuera del laboratorio; sus átomos duran desde segundos hasta poco menos de una hora.",
        ),
        // 103 Lr · Lawrencio
        ElementFact(
            description = "El último de los actínidos, nombrado por Ernest Lawrence, inventor del ciclotrón.",
            uses = "Solo existe en laboratorios, donde se crea átomo por átomo para estudiar su química.",
        ),
        // 104 Rf · Rutherfordio
        ElementFact(
            description = "El primer elemento superpesado tras los actínidos; honra a Ernest Rutherford, descubridor del núcleo atómico.",
            uses = "Solo se usa en investigación; su forma más estable dura apenas unas horas antes de desintegrarse.",
        ),
        // 105 Db · Dubnio
        ElementFact(
            description = "Nombrado por Dubná, la ciudad rusa sede de un famoso laboratorio de física nuclear.",
            uses = "Se crea de unos pocos átomos a la vez y solo sirve para explorar los límites de la tabla periódica.",
        ),
        // 106 Sg · Seaborgio
        ElementFact(
            description = "Honra a Glenn Seaborg, la primera persona que tuvo un elemento con su nombre estando aún con vida.",
            uses = "Solo se usa en investigación; los químicos han estudiado sus compuestos con apenas unos cuantos átomos.",
        ),
        // 107 Bh · Bohrio
        ElementFact(
            description = "Creado en Alemania en 1981 y nombrado por Niels Bohr, pionero de la física atómica.",
            uses = "Sus átomos duran segundos o, a lo mucho, alrededor de un minuto; solo sirven para la ciencia básica.",
        ),
        // 108 Hs · Hasio
        ElementFact(
            description = "Nombrado por Hesse, el estado alemán donde se creó por primera vez en 1984.",
            uses = "Solo se usa en investigación; con unos pocos átomos se logró estudiar uno de sus óxidos.",
        ),
        // 109 Mt · Meitnerio
        ElementFact(
            description = "Honra a Lise Meitner, física que ayudó a explicar la fisión nuclear; se creó en Alemania en 1982.",
            uses = "Sin usos prácticos: se ha producido apenas un puñado de átomos para la investigación.",
        ),
        // 110 Ds · Darmstatio
        ElementFact(
            description = "Nombrado por Darmstadt, la ciudad alemana donde se creó en 1994.",
            uses = "Solo existe unos segundos en el laboratorio y se usa para explorar la física de los núcleos superpesados.",
        ),
        // 111 Rg · Roentgenio
        ElementFact(
            description = "Nombrado por Wilhelm Röntgen, descubridor de los rayos X; se creó en Alemania en 1994.",
            uses = "Solo se usa en investigación; se cree que químicamente se parecería al oro, pero aún no se ha comprobado.",
        ),
        // 112 Cn · Copernicio
        ElementFact(
            description = "Honra a Nicolás Copérnico; los cálculos sugieren que podría ser líquido a temperatura ambiente, como el mercurio.",
            uses = "Solo se usa en investigación; con unos pocos átomos se comprobó que es muy volátil.",
        ),
        // 113 Nh · Nihonio
        ElementFact(
            description = "El primer elemento descubierto en Asia: lo creó un equipo japonés y su nombre viene de Nihon, 'Japón'.",
            uses = "Solo se usa en investigación; al equipo japonés le tomó nueve años obtener tres átomos.",
        ),
        // 114 Fl · Flerovio
        ElementFact(
            description = "Nombrado por el Laboratorio Flerov de Rusia; se esperaba que fuera más estable, pero sus átomos duran segundos.",
            uses = "Solo existe en el laboratorio, donde se estudia si se comporta como un metal o casi como un gas noble.",
        ),
        // 115 Mc · Moscovio
        ElementFact(
            description = "Creado por científicos rusos y estadounidenses; su nombre honra a la región de Moscú.",
            uses = "Sus átomos duran fracciones de segundo, así que solo sirve para investigar los núcleos superpesados.",
        ),
        // 116 Lv · Livermorio
        ElementFact(
            description = "Nombrado por el Laboratorio Lawrence Livermore de California, que colaboró en su creación en Rusia.",
            uses = "No tiene uso práctico: se han observado unas pocas decenas de átomos que viven milisegundos.",
        ),
        // 117 Ts · Teneso
        ElementFact(
            description = "Su nombre viene de Tennessee: para crearlo se necesitó berkelio producido en ese estado de Estados Unidos.",
            uses = "Solo se usa en investigación: es uno de los elementos más recientes y se conocen poquísimos átomos.",
        ),
        // 118 Og · Oganesón
        ElementFact(
            description = "El elemento más pesado conocido; honra a Yuri Oganessian y solo se han detectado unos cinco átomos.",
            uses = "Solo sirve para la ciencia: cada átomo dura menos de un milisegundo antes de desintegrarse.",
        ),
    )

    init {
        check(facts.size == PeriodicTable.SIZE) { "Faltan datos: ${facts.size} de ${PeriodicTable.SIZE}" }
    }
}
