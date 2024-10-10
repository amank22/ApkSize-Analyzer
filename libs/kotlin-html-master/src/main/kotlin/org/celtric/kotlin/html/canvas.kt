package org.celtric.kotlin.html

fun canvas(
    // Mandatory
    width: Int,
    height: Int,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("canvas", content(), AllAttributes(mapOf(
    "width" to width,
    "height" to height,
    "class" to classes,
    "id" to id
), other, data))
