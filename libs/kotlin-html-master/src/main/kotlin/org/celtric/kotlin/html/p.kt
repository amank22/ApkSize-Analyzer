package org.celtric.kotlin.html

fun p(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("p", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun p(content: String) = p { content }
fun p(content: Node) = p { content }
fun p(content: List<Node>) = p { content }
