package org.celtric.kotlin.html

fun progress(
    // Mandatory
    value: String,
    max: String,

    // Custom
    other: Attributes = emptyMap()
) = EmptyBlockElement("progress", AllAttributes(mapOf(
    "value" to value,
    "max" to max
), other, emptyMap()))
