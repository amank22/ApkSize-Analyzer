package org.celtric.kotlin.html

fun article(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("article", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun article(content: String) = article { content }
fun article(content: Node) = article { content }
fun article(content: List<Node>) = article { content }
