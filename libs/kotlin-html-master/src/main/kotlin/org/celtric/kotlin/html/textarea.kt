package org.celtric.kotlin.html

fun textarea(
    // Optional
    autofocus: Boolean = false,
    cols: Int? = null,
    disabled: Boolean = false,
    maxlength: Int? = null,
    minlength: Int? = null,
    name: String? = null,
    placeholder: String? = null,
    readonly: Boolean = false,
    required: Boolean = false,
    rows: Int? = null,
    spellcheck: Boolean = false,
    wrap: String? = null,

    // Global
    classes: String? = null,
    id: String? = null,

    // Custom
    other: Attributes = emptyMap(),
    data: Attributes = emptyMap(),

    // Content
    content: () -> Any
) = BlockElement("textarea", content(), AllAttributes(mapOf(
    "autofocus" to autofocus,
    "cols" to cols,
    "disabled" to disabled,
    "maxlength" to maxlength,
    "minlength" to minlength,
    "name" to name,
    "placeholder" to placeholder,
    "readonly" to readonly,
    "required" to required,
    "rows" to rows,
    "spellcheck" to spellcheck,
    "wrap" to wrap,
    "class" to classes,
    "id" to id
), other, data))
