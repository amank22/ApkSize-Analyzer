package org.celtric.kotlin.html

fun img(
    // Mandatory
    src: String,
    alt: String,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap()
) = EmptyInlineElement("img", AllAttributes(mapOf(
    "src" to src,
    "alt" to alt,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))
