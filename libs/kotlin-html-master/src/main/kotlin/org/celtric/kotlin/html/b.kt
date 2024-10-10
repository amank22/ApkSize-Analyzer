package org.celtric.kotlin.html

fun b(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("b", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun b(content: String) = b { content }
fun b(content: Node) = b { content }
fun b(content: List<Node>) = b { content }
