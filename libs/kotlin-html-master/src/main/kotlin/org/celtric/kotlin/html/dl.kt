package org.celtric.kotlin.html

fun dl(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("dl", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun dl(content: String) = dl { content }
fun dl(content: Node) = dl { content }
fun dl(content: List<Node>) = dl { content }
