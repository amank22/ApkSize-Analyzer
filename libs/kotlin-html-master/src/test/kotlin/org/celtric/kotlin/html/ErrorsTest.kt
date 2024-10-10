package org.celtric.kotlin.html

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ErrorsTest {

    @Test fun `offers useful message when invalid content is passed`() {
        assertThrows(HTMLException::class.java, { span { listOf(listOf("Foo")) } })
            .let { assertEquals("Content must be String, Number, Node or List<Node>, SingletonList<SingletonList<String>> given.", it.message) }
    }
}
