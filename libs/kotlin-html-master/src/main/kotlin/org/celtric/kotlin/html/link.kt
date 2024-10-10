package org.celtric.kotlin.html

fun link(
    // Optional
    crossorigin: String? = null,
    href: String? = null,
    media: String? = null,
    rel: String? = null,
    sizes: String? = null,
    title: String? = null,
    type: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap()
) = EmptyBlockElement("link", AllAttributes(mapOf(
    "crossorigin" to crossorigin,
    "href" to href,
    "media" to media,
    "rel" to rel,
    "sizes" to sizes,
    "title" to title,
    "type" to type
), other, data))
