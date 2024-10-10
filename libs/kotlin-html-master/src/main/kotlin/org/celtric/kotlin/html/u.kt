package org.celtric.kotlin.html

fun u(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("u", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun u(content: String) = u { content }
fun u(content: Node) = u { content }
fun u(content: List<Node>) = u { content }
