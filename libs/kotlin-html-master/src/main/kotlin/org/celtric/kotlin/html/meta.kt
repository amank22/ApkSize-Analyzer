package org.celtric.kotlin.html

fun meta(
    // Optional
    charset: String? = null,
    content: String? = null,
    httpEquiv: String? = null,
    name: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap()
) = EmptyBlockElement("meta", AllAttributes(mapOf(
    "charset" to charset,
    "content" to content,
    "http-equiv" to httpEquiv,
    "name" to name
), other, data))
