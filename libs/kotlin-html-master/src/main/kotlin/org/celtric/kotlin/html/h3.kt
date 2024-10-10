package org.celtric.kotlin.html

fun h3(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("h3", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun h3(content: String) = h3 { content }
fun h3(content: Node) = h3 { content }
fun h3(content: List<Node>) = h3 { content }
