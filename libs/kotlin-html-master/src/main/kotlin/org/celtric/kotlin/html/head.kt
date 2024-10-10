package org.celtric.kotlin.html

fun head(
    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("head", content(), AllAttributes(emptyMap(), other, data))

fun head(content: String) = head { content }
fun head(content: Node) = head { content }
fun head(content: List<Node>) = head { content }
