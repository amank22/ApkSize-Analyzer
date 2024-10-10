package org.celtric.kotlin.html

fun thead(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("thead", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun thead(content: String) = thead { content }
fun thead(content: Node) = thead { content }
fun thead(content: List<Node>) = thead { content }
