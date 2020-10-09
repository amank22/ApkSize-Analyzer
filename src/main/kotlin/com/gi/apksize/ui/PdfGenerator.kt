package com.gi.apksize.ui

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import java.io.File
import java.io.FileOutputStream


object PdfGenerator {

    fun generate(html: String, outputFile : File) {
        val htmlSanitized = sanitizeHtml(html)
        XRLog.setLoggingEnabled(false)
        val builder = PdfRendererBuilder()
        builder.useFastMode()
        builder.withHtmlContent(htmlSanitized, "")
        val output = FileOutputStream(outputFile)
        output.use {
            builder.toStream(output)
            builder.run()
        }
    }

    private fun sanitizeHtml(html: String) : String {
        val identifier = "\" rel=\"stylesheet\">"
        val identifierR = "\" rel=\"stylesheet\"/>"
        val m1 = "charset=\"utf-8\">"
        val m1R = "charset=\"utf-8\"/>"
        val m2 = "name=\"viewport\">"
        val m2R = "name=\"viewport\"/>"
        val h = html.replace(identifier, identifierR)
        val h1 = h.replace(m1, m1R)
        val h2 = h1.replace(m2, m2R)
        return h2
    }

}