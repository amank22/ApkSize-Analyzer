package org.celtric.kotlin.html

fun style(
    // Optional
    type: String? = null,
    media: String? = null,
    nonce: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("style", content(), AllAttributes(mapOf(
    "type" to type,
    "media" to media,
    "nonce" to nonce,
    "title" to title
), other, data))
