package org.celtric.kotlin.html

fun ul(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Mandatory
    content: () -> Any
) = BlockElement("ul", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun ul(content: String) = ul { content }
fun ul(content: Node) = ul { content }
fun ul(content: List<Node>) = ul { content }
