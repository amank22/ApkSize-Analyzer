package org.celtric.kotlin.html

fun main(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("main", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun main(content: String) = main { content }
fun main(content: Node) = main { content }
fun main(content: List<Node>) = main { content }
