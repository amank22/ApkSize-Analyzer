package org.celtric.kotlin.html

fun label(
    // Optional
    `for`: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("label", content(), AllAttributes(mapOf(
    "for" to `for`,
    "class" to classes,
    "id" to id
), other, data))

fun label(content: String) = label { content }
fun label(content: Node) = label { content }
fun label(content: List<Node>) = label { content }
