package org.celtric.kotlin.html

fun cite(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("cite", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun cite(content: String) = cite { content }
fun cite(content: Node) = cite { content }
fun cite(content: List<Node>) = cite { content }
