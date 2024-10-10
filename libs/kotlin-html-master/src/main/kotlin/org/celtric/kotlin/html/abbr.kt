package org.celtric.kotlin.html

fun abbr(
    // Mandatory
    title: String,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("abbr", content(), AllAttributes(mapOf(
    "title" to title,
    "class" to classes,
    "id" to id
), other, data))

fun abbr(title: String, content: String) = abbr(title) { content }
fun abbr(title: String, content: Node) = abbr(title) { content }
fun abbr(title: String, content: List<Node>) = abbr(title) { content }
