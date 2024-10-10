package org.celtric.kotlin.html

fun span(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("span", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun span(content: String) = span { content }
fun span(content: Node) = span { content }
fun span(content: List<Node>) = span { content }
