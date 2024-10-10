package org.celtric.kotlin.html

fun section(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("section", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun section(content: String) = section { content }
fun section(content: Node) = section { content }
fun section(content: List<Node>) = section { content }
