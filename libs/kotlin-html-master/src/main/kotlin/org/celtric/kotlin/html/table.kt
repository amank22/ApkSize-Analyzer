package org.celtric.kotlin.html

fun table(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("table", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun table(content: String) = table { content }
fun table(content: Node) = table { content }
fun table(content: List<Node>) = table { content }
