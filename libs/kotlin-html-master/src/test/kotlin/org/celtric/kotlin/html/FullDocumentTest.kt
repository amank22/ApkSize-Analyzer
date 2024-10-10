package org.celtric.kotlin.html

import org.junit.jupiter.api.Test

internal class FullDocumentTest {

    @Test fun full_document() {
        (doctype("html") + html {
            head {
                title("Document title") +
                meta(charset = "utf-8") +
                link(href = "css/style.css", rel = "stylesheet") +
                script(type = "text/javascript", src = "js/script.js")
            } +
            body {
                div(classes = "container") {
                    h1("A title") +
                    p(classes = "introduction") {
                        "A paragraph"
                    } +
                    ul {
                        li(a("http://www.example.com/", "A link")) +
                        li(a("http://www.example.com/", "A second link"))
                    } +
                    span { "Inline element"} +
                    div { "Block element"}
                }
            }
        }).assertRendersMultiline("""
            <!DOCTYPE html>
            <html>
                <head>
                    <title>Document title</title>
                    <meta charset="utf-8">
                    <link href="css/style.css" rel="stylesheet">
                    <script type="text/javascript" src="js/script.js"></script>
                </head>
                <body>
                    <div class="container">
                        <h1>A title</h1>
                        <p class="introduction">A paragraph</p>
                        <ul>
                            <li><a href="http://www.example.com/">A link</a></li>
                            <li><a href="http://www.example.com/">A second link</a></li>
                        </ul>
                        <span>Inline element</span>
                        <div>Block element</div>
                    </div>
                </body>
            </html>
        """)
    }
}
