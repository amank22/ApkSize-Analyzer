package org.celtric.kotlin.html

fun nav(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("nav", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun nav(content: String) = nav { content }
fun nav(content: Node) = nav { content }
fun nav(content: List<Node>) = nav { content }
