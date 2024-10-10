package org.celtric.kotlin.html

fun datalist(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("datalist", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun datalist(content: String) = datalist { content }
fun datalist(content: Node) = datalist { content }
fun datalist(content: List<Node>) = datalist { content }
