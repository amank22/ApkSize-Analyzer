package org.celtric.kotlin.html

import org.junit.jupiter.api.Assertions

val testRenderingOptions = RenderingOptions(lineSeparator = "\n")

fun Node.assertRenders(expected: String) {
    toList().assertRenders(expected)
}

fun List<Node>.assertRenders(expected: String) {
    Assertions.assertEquals(expected, render(testRenderingOptions))
}

fun Node.assertRendersMultiline(expected: String) {
    toList().assertRendersMultiline(expected)
}

fun List<Node>.assertRendersMultiline(expected: String) {
    Assertions.assertEquals(expected.trimIndent() + "\n", render(testRenderingOptions))
}

fun List<Node>.assertRendersSameAs(other: List<Node>) {
    Assertions.assertEquals(other.render(testRenderingOptions), render(testRenderingOptions))
}
