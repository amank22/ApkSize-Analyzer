package org.celtric.kotlin.html

fun data(
    //  Mandatory,
    value: String,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("data", content(), AllAttributes(mapOf(
    "value" to value,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun data(value: String, content: String) = data(value) { content }
fun data(value: String, content: Node) = data(value) { content }
fun data(value: String, content: List<Node>) = data(value) { content }
