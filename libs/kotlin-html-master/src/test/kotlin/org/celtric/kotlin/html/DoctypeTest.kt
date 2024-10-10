package org.celtric.kotlin.html

import org.junit.jupiter.api.Test

internal class DoctypeTest {

    @Test fun doctype() {
        doctype("html").assertRenders("<!DOCTYPE html>\n")
    }
}
