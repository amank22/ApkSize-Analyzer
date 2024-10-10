package org.celtric.kotlin.html

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RenderingOptionsTest {

    private val html = div(div(div("foo")))

    @Test fun default_options() {
        val eol = System.lineSeparator()
        assertEquals("<div>$eol    <div>$eol        <div>foo</div>$eol    </div>$eol</div>$eol",
             html.render())
    }

    @Test fun custom_options() {
        assertEquals("..<div>#...<div>#....<div>foo</div>#...</div>#..</div>#",
             html.render(RenderingOptions(".", 2, "#")))
    }
}
