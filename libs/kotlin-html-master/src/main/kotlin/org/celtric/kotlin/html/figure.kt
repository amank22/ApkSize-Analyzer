package org.celtric.kotlin.html

fun figure(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("figure", content(), AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))

fun figure(content: String) = figure { content }
fun figure(content: Node) = figure { content }
fun figure(content: List<Node>) = figure { content }
