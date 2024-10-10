package org.celtric.kotlin.html

fun h5(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("h5", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun h5(content: String) = h5 { content }
fun h5(content: Node) = h5 { content }
fun h5(content: List<Node>) = h5 { content }
