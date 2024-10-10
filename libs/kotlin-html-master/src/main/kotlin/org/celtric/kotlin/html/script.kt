package org.celtric.kotlin.html

fun script(
    // Optional
    type: String? = null,
    src: String? = null,
    async: Boolean = false,
    crossorigin: String? = null,
    defer: Boolean = false,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any = {}
) = BlockElement("script", content(), AllAttributes(mapOf(
    "type" to type,
    "src" to src,
    "async" to async,
    "crossorigin" to crossorigin,
    "defer" to defer,
    "class" to classes,
    "id" to id
), other, data))
