package org.celtric.kotlin.html

fun doctype(type: String) = DocumentType(type)
fun text(content: String) = Text(content)
fun text(content: () -> String) = Text(content())
