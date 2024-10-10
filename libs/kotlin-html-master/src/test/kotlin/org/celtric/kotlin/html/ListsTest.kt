package org.celtric.kotlin.html

import org.junit.jupiter.api.Test

internal class ListsTest {

    @Test fun list_of_nodes() {
        (p("foo") + p("bar")).assertRendersMultiline("""
            <p>foo</p>
            <p>bar</p>
        """)
    }

    @Test fun from_list_transformation() {
        val people = listOf("Gael", "Laura", "Ricard")

        ul {
            people.map { li { it } }
        }.assertRendersMultiline("""
            <ul>
                <li>Gael</li>
                <li>Laura</li>
                <li>Ricard</li>
            </ul>
        """)
    }

    @Test fun from_list_transformations() {
        val people = listOf("Gael", "Laura", "Ricard")

        ul {
            people.map { li { people.map { em { it } } } }
        }.assertRendersMultiline("""
            <ul>
                <li><em>Gael</em><em>Laura</em><em>Ricard</em></li>
                <li><em>Gael</em><em>Laura</em><em>Ricard</em></li>
                <li><em>Gael</em><em>Laura</em><em>Ricard</em></li>
            </ul>
        """)
    }

    @Test fun `the way lists are grouped when combined is irrelevant`() {
        val elem = li("Element")
        val reference = elem + elem + elem + elem

        ((elem) + (elem + elem + elem)).assertRendersSameAs(reference)
        ((elem + elem) + (elem + elem)).assertRendersSameAs(reference)
        ((elem + elem + elem) + (elem)).assertRendersSameAs(reference)
    }
}
