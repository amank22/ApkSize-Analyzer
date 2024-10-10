package org.celtric.kotlin.html

fun caption(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("caption", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun caption(content: String) = caption { content }
fun caption(content: Node) = caption { content }
fun caption(content: List<Node>) = caption { content }
