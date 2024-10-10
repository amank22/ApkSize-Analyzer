package org.celtric.kotlin.html

fun h2(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("h2", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun h2(content: String) = h2 { content }
fun h2(content: Node) = h2 { content }
fun h2(content: List<Node>) = h2 { content }
