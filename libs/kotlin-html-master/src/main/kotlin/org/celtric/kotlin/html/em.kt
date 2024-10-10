package org.celtric.kotlin.html

fun em(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("em", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun em(content: String) = em { content }
fun em(content: Node) = em { content }
fun em(content: List<Node>) = em { content }
