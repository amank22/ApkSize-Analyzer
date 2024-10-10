package org.celtric.kotlin.html

fun i(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("i", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun i(content: String) = i { content }
fun i(content: Node) = i { content }
fun i(content: List<Node>) = i { content }
