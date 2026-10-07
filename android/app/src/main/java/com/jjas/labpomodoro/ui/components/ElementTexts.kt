package com.jjas.labpomodoro.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.ElementDiscoveries
import com.jjas.labpomodoro.domain.model.ElementDiscovery
import com.jjas.labpomodoro.domain.model.ElementFact

// Textos de los elementos en el idioma actual. Viven en element_data.xml
// (índice = número atómico − 1)

/** Nombre del elemento en el idioma actual, fuera de Compose (notificaciones, widget). */
fun Element.localizedName(context: Context): String =
    context.resources.getStringArray(R.array.element_names)[atomicNumber - 1]

/** Nombre del elemento en el idioma actual. */
@Composable
fun Element.localizedName(): String = stringArrayResource(R.array.element_names)[atomicNumber - 1]

/** Qué es y para qué sirve, para la ficha. */
@Composable
fun elementFact(atomicNumber: Int): ElementFact = ElementFact(
    description = stringArrayResource(R.array.element_fact_descriptions)[atomicNumber - 1],
    uses = stringArrayResource(R.array.element_fact_uses)[atomicNumber - 1],
)

/** Cuándo, quién y dónde se descubrió. Los textos vacíos del recurso se vuelven null. */
@Composable
fun elementDiscovery(atomicNumber: Int): ElementDiscovery {
    val i = atomicNumber - 1
    return ElementDiscovery(
        year = ElementDiscoveries.year(atomicNumber),
        era = stringArrayResource(R.array.element_discovery_eras)[i].ifEmpty { null },
        discoverers = stringArrayResource(R.array.element_discoverers)[i],
        country = stringArrayResource(R.array.element_discovery_countries)[i].ifEmpty { null },
    )
}

/** De dónde sale su color en el recipiente; vacío si no hay dato. */
@Composable
fun elementLookOrigin(atomicNumber: Int): String = stringArrayResource(R.array.element_look_origins)[atomicNumber - 1]
