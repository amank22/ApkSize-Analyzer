package org.celtric.kotlin.html

fun optgroup(
    // Mandatory
    label: String,

    // Optional
    disabled: Boolean = false,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("optgroup", content(), AllAttributes(mapOf(
    "label" to label,
    "disabled" to disabled,
    "class" to classes,
    "id" to id
), other, data))

fun optgroup(label: String, content: String) = optgroup(label) { content }
fun optgroup(label: String, content: Node) = optgroup(label) { content }
fun optgroup(label: String, content: List<Node>) = optgroup(label) { content }
