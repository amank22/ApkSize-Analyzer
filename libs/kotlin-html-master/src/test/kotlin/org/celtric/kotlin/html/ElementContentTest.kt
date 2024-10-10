package org.celtric.kotlin.html

import org.junit.jupiter.api.Test

internal class ElementContentTest {

    @Test fun `numbers are automatically cast to text`() {
        span { 1 }.assertRenders("<span>1</span>")
        span { 1L }.assertRenders("<span>1</span>")
        span { 1.0 }.assertRenders("<span>1.0</span>")
        span { 1.0f }.assertRenders("<span>1.0</span>")
    }

    @Test fun `content can be an empty list`() {
        span(emptyList()).assertRenders("<span></span>")
    }
}
