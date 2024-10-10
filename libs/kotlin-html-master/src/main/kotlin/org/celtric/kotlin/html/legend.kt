package org.celtric.kotlin.html

fun legend(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("legend", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun legend(content: String) = legend { content }
fun legend(content: Node) = legend { content }
fun legend(content: List<Node>) = legend { content }
