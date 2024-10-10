package org.celtric.kotlin.html

import org.junit.jupiter.api.Test

internal class AttributesTest {

    @Test fun global_attributes() {
        span(classes = "class1 class2", id = "unique-id", title = "A title") { "Content" }
            .assertRenders("""<span class="class1 class2" id="unique-id" title="A title">Content</span>""")
    }

    @Test fun other_attributes() {
        span(other = mapOf("a" to "b", "c" to "d")) { "Content" }
            .assertRenders("""<span a="b" c="d">Content</span>""")
    }

    @Test fun data_attributes() {
        span(data = mapOf("a" to "b", "c" to "d")) { "Content" }
            .assertRenders("""<span data-a="b" data-c="d">Content</span>""")
    }

    @Test fun boolean_attributes() {
        span { "Content" }
            .assertRenders("<span>Content</span>")

        span(other = mapOf("foo" to false)) { "Content" }
            .assertRenders("<span>Content</span>")

        span(other = mapOf("foo" to true)) { "Content" }
            .assertRenders("<span foo>Content</span>")
    }

    @Test fun empty_attributes() {
        span(classes = "", other = mapOf(), data = mapOf()) {}
            .assertRenders("<span></span>")
    }
}
