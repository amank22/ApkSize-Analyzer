package org.celtric.kotlin.html

fun footer(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("footer", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun footer(content: String) = footer { content }
fun footer(content: Node) = footer { content }
fun footer(content: List<Node>) = footer { content }
