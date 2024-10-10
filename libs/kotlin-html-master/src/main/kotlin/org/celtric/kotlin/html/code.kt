package org.celtric.kotlin.html

fun code(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("code", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun code(content: String) = code { content }
fun code(content: Node) = code { content }
fun code(content: List<Node>) = code { content }
