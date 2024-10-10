package org.celtric.kotlin.html

fun mark(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("mark", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun mark(content: String) = mark { content }
fun mark(content: Node) = mark { content }
fun mark(content: List<Node>) = mark { content }
