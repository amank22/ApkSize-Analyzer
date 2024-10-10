package org.celtric.kotlin.html

fun html(
    // Optional
    xmlns: String? = null,

    // Global
    classes: String? = null,
    dir: String? = null,
    lang: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("html", content(), AllAttributes(mapOf(
    "xmlns" to xmlns,
    "class" to classes,
    "dir" to dir,
    "lang" to lang
), other, data))

fun html(content: String) = html { content }
fun html(content: Node) = html { content }
fun html(content: List<Node>) = html { content }
