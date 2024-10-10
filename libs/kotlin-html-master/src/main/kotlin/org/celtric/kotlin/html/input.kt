package org.celtric.kotlin.html

fun input(
    // Mandatory
    type: String,

    // Optional
    accept: String? = null,
    autocomplete: String? = null,
    autofocus: Boolean = false,
    capture: Boolean = false,
    checked: Boolean = false,
    disabled: Boolean = false,
    list: String? = null,
    max: String? = null,
    maxlength: Int? = null,
    min: String? = null,
    minlength: Int? = null,
    multiple: Boolean = false,
    name: String? = null,
    pattern: String? = null,
    placeholder: String? = null,
    readonly: Boolean = false,
    required: Boolean = false,
    size: Int? = null,
    step: String? = null,
    value: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,
    title: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap()
) = EmptyBlockElement("input", AllAttributes(mapOf(
    "type" to type,
    "accept" to accept,
    "autocomplete" to autocomplete,
    "autofocus" to autofocus,
    "capture" to capture,
    "checked" to checked,
    "disabled" to disabled,
    "list" to list,
    "max" to max,
    "maxlength" to maxlength,
    "min" to min,
    "minlength" to minlength,
    "multiple" to multiple,
    "name" to name,
    "pattern" to pattern,
    "placeholder" to placeholder,
    "readonly" to readonly,
    "required" to required,
    "size" to size,
    "step" to step,
    "value" to value,
    "class" to classes,
    "id" to id,
    "title" to title
), other, data))
