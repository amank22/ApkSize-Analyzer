package org.celtric.kotlin.html

fun div(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("div", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun div(content: String) = div { content }
fun div(content: Node) = div { content }
fun div(content: List<Node>) = div { content }
