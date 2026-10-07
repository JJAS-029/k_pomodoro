package com.jjas.labpomodoro.domain.model

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element as XmlElement

/**
 * Lee los `string-array` de `element_data.xml` directo del archivo, para revisar los textos de los
 * elementos en pruebas unitarias sin depender de los recursos de Android.
 */
object ElementDataXml {

    /** @param folder carpeta de recursos: "values" (inglés) o "values-es". */
    fun arrays(folder: String): Map<String, List<String>> {
        val file = listOf("src/main/res", "app/src/main/res", "android/app/src/main/res")
            .map { File(it, "$folder/element_data.xml") }
            .firstOrNull { it.exists() }
            ?: error("No se encontró $folder/element_data.xml desde ${File(".").absolutePath}")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string-array")
        return (0 until nodes.length).associate { i ->
            val array = nodes.item(i) as XmlElement
            val items = array.getElementsByTagName("item")
            array.getAttribute("name") to (0 until items.length).map { items.item(it).textContent.trim() }
        }
    }

    val spanish: Map<String, List<String>> by lazy { arrays("values-es") }
    val english: Map<String, List<String>> by lazy { arrays("values") }
}
