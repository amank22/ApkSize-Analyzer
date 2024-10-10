package org.celtric.kotlin.html

fun details(
    // Optional
    open: Boolean = false,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("details", content(), AllAttributes(mapOf(
    "open" to open,
    "class" to classes,
    "id" to id
), other, data))

fun details(content: String) = details { content }
fun details(content: Node) = details { content }
fun details(content: List<Node>) = details { content }
