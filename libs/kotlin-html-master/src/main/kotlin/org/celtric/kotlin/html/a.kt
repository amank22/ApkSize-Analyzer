package org.celtric.kotlin.html

fun a(
    // Mandatory
    href: String,

    // Optional
    rel: String? = null,
    target: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("a", content(), AllAttributes(mapOf(
    "href" to href,
    "rel" to rel,
    "target" to target,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))

fun a(href: String, content: String) = a(href) { content }
fun a(href: String, content: Node) = a(href) { content }
fun a(href: String, content: List<Node>) = a(href) { content }
