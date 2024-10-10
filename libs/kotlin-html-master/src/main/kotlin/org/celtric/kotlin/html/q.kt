package org.celtric.kotlin.html

fun q(
    // Optional
    cite: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("q", content(), AllAttributes(mapOf(
    "cite" to cite,
    "class" to classes,
    "id" to id
), other, data))

fun q(content: String) = q { content }
fun q(content: Node) = q { content }
fun q(content: List<Node>) = q { content }
