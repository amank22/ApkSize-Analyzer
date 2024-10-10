package org.celtric.kotlin.html

fun ins(
    // Optional
    cite: String? = null,
    datetime: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("ins", content(), AllAttributes(mapOf(
    "cite" to cite,
    "datetime" to datetime,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun ins(content: String) = ins { content }
fun ins(content: Node) = ins { content }
fun ins(content: List<Node>) = ins { content }
