package org.celtric.kotlin.html

fun sub(
    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("sub", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun sub(content: String) = sub { content }
fun sub(content: Node) = sub { content }
fun sub(content: List<Node>) = sub { content }
