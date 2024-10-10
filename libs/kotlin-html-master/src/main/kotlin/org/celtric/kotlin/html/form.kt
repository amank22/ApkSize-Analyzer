package org.celtric.kotlin.html

fun form(
    // Optional
    action: String? = null,
    method: String? = null,
    acceptCharset: String? = null,
    autocomplete: String? = null,
    enctype: String? = null,
    name: String? = null,
    novalidate : Boolean = false,
    target: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("form", content(), AllAttributes(mapOf(
    "action" to action,
    "method" to method,
    "accept-charset" to acceptCharset,
    "autocomplete" to autocomplete,
    "enctype" to enctype,
    "name" to name,
    "novalidate " to novalidate ,
    "target" to target,
    "class" to classes,
    "id" to id
), other, data))
