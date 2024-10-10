package org.celtric.kotlin.html

fun s(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("s", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun s(content: String) = s { content }
fun s(content: Node) = s { content }
fun s(content: List<Node>) = s { content }
