package org.celtric.kotlin.html

fun time(
    // Optional
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
) = InlineElement("time", content(), AllAttributes(mapOf(
    "datetime" to datetime,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun time(content: String) = time { content }
fun time(content: Node) = time { content }
fun time(content: List<Node>) = time { content }
