package org.celtric.kotlin.html

fun h6(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("h6", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun h6(content: String) = h6 { content }
fun h6(content: Node) = h6 { content }
fun h6(content: List<Node>) = h6 { content }
