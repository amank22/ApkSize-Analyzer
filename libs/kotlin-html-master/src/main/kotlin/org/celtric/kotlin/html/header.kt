package org.celtric.kotlin.html

fun header(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("header", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun header(content: String) = header { content }
fun header(content: Node) = header { content }
fun header(content: List<Node>) = header { content }
