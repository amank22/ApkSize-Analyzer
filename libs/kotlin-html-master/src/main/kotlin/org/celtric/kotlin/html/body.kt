package org.celtric.kotlin.html

fun body(
    // Optional
    lang: String? = null,

    // Global
    classes: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("body", content(), AllAttributes(mapOf(
    "class" to classes,
    "lang" to lang
), other, data))

fun body(content: String) = body { content }
fun body(content: Node) = body { content }
fun body(content: List<Node>) = body { content }
