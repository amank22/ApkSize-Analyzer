package org.celtric.kotlin.html

fun param(
    // Mandatory
    name: String,
    value: String,

    // Custom
    other: Attributes = emptyMap()
) = EmptyBlockElement("param", AllAttributes(mapOf(
    "name" to name,
    "value" to value
), other, emptyMap()))
