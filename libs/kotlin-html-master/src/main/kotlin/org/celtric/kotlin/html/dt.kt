package org.celtric.kotlin.html

fun dt(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("dt", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun dt(content: String) = dt { content }
fun dt(content: Node) = dt { content }
fun dt(content: List<Node>) = dt { content }
