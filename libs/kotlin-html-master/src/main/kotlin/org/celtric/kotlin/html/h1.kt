package org.celtric.kotlin.html

fun h1(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("h1", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun h1(content: String) = h1 { content }
fun h1(content: Node) = h1 { content }
fun h1(content: List<Node>) = h1 { content }
