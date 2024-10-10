package org.celtric.kotlin.html

fun wbr(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("wbr", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun wbr(content: String) = wbr { content }
fun wbr(content: Node) = wbr { content }
fun wbr(content: List<Node>) = wbr { content }
