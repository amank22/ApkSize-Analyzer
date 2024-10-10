package org.celtric.kotlin.html

fun dialog(
    // Optional
    open: Boolean = false,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("dialog", content(), AllAttributes(mapOf(
    "open" to open,
    "class" to classes,
    "id" to id
), other, data))

fun dialog(content: String) = dialog { content }
fun dialog(content: Node) = dialog { content }
fun dialog(content: List<Node>) = dialog { content }
