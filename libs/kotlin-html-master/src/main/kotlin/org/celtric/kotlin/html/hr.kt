package org.celtric.kotlin.html

fun hr(
    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap()
) = EmptyBlockElement("hr", AllAttributes(mapOf(
    "class" to classes,
    "id" to id
), other, data))
