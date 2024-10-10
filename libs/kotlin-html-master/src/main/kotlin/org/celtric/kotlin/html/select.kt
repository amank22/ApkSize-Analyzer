package org.celtric.kotlin.html

fun select(
    // Optional
    autofocus: Boolean = false,
    disabled: Boolean = false,
    multiple: Boolean = false,
    name: String? = null,
    required: Boolean = false,
    size: Int? = null,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("select", content(), AllAttributes(mapOf(
    "autofocus" to autofocus,
    "disabled" to disabled,
    "multiple" to multiple,
    "name" to name,
    "required" to required,
    "size" to size,
    "class" to classes,
    "id" to id
), other, data))
