package org.celtric.kotlin.html

fun bdo(
    // Mandatory
    dir: String,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = InlineElement("bdo", content(), AllAttributes(mapOf(
    "dir" to dir,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))
