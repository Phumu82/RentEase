package com.rentease.app.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the multilingual requirement (English / isiZulu / Tshivenda) on the
 * source resources themselves: every string exists in every language, format
 * placeholders line up, nothing is empty, and no user-facing text is hardcoded
 * in Compose `Text("...")` calls. Gradle runs unit tests with the module
 * directory (app/) as the working directory.
 */
class LocalizationResourcesTest {

    private fun resDir(): File = File("src/main/res").let { if (it.exists()) it else File("app/src/main/res") }

    private fun load(folder: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File(resDir(), "$folder/strings.xml"))
        val nodes = doc.getElementsByTagName("string")
        return (0 until nodes.length).associate { i ->
            val node = nodes.item(i)
            node.attributes.getNamedItem("name").nodeValue to node.textContent
        }
    }

    private val placeholder = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[sdfxXeEgGcb]")
    private fun placeholders(s: String) = placeholder.findAll(s).map { it.value }.sorted().toList()

    @Test
    fun `isiZulu and Tshivenda define exactly the same string keys as English`() {
        val en = load("values").keys
        assertEquals("isiZulu keys differ from English", en, load("values-zu").keys)
        assertEquals("Tshivenda keys differ from English", en, load("values-ve").keys)
    }

    @Test
    fun `no translation is blank`() {
        for (folder in listOf("values", "values-zu", "values-ve")) {
            load(folder).forEach { (key, value) -> assertTrue("$folder/$key is blank", value.isNotBlank()) }
        }
    }

    @Test
    fun `format placeholders match the English string in every language`() {
        val en = load("values")
        for (folder in listOf("values-zu", "values-ve")) {
            load(folder).forEach { (key, value) ->
                assertEquals("$folder/$key placeholders", placeholders(en.getValue(key)), placeholders(value))
            }
        }
    }

    @Test
    fun `no Compose Text call uses a hardcoded letter literal`() {
        val src = File("src/main/java").let { if (it.exists()) it else File("app/src/main/java") }
        val literalText = Regex("""\bText\(\s*(text\s*=\s*)?"[A-Za-z]""")
        val offenders = src.walkTopDown().filter { it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { literalText.containsMatchIn(it.value) }
                    .map { "${file.name}:${it.index + 1}" }
            }.toList()
        assertTrue("Hardcoded user-facing Text literals: $offenders", offenders.isEmpty())
    }
}
