package org.celtric.kotlin.html

fun output(
    // Optional
    name: String? = null,
    `for`: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("output", content(), AllAttributes(mapOf(
    "name" to name,
    "for" to `for`,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun output(content: String) = output { content }
fun output(content: Node) = output { content }
fun output(content: List<Node>) = output { content }
