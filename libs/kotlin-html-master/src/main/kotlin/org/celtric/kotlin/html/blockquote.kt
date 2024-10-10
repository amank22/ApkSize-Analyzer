package org.celtric.kotlin.html

fun blockquote(
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
) = BlockElement("blockquote", content(), AllAttributes(mapOf(
    "cite" to cite,
    "class" to classes,
    "id" to id
), other, data))

fun blockquote(content: String) = blockquote { content }
fun blockquote(content: Node) = blockquote { content }
fun blockquote(content: List<Node>) = blockquote { content }
