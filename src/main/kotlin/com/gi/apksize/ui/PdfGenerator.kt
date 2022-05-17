package com.gi.apksize.ui
//
//import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
//import com.openhtmltopdf.util.Diagnostic
//import com.openhtmltopdf.util.XRLog
//import com.openhtmltopdf.util.XRLogger
//import java.io.File
//import java.io.FileOutputStream
//import java.util.logging.Level
//
//
//object PdfGenerator {
//
//    fun generate(html: String, outputFile : File) {
//        val htmlSanitized = sanitizeHtml(html)
//        XRLog.setLoggerImpl(PdfLogger())
//        XRLog.setLoggingEnabled(false)
//        val builder = PdfRendererBuilder()
//        builder.useFastMode()
//        builder.withHtmlContent(htmlSanitized, "")
//        val output = FileOutputStream(outputFile)
//        output.use {
//            builder.toStream(output)
//            builder.run()
//        }
//    }
//
//    private class PdfLogger : XRLogger {
//        override fun log(where: String?, level: Level?, msg: String?) {
//            println("$where : $level = $msg")
//        }
//
//        override fun log(where: String?, level: Level?, msg: String?, th: Throwable?) {
//            println("$where : $level = $msg \n ${th?.localizedMessage}")
//        }
//
//        override fun setLevel(logger: String?, level: Level?) {
//
//        }
//
//        override fun isLogLevelEnabled(diagnostic: Diagnostic?): Boolean {
//            return false
//        }
//
//    }
//
//    private fun sanitizeHtml(html: String) : String {
//        val identifier = "\" rel=\"stylesheet\">"
//        val identifierR = "\" rel=\"stylesheet\"/>"
//        val m1 = "charset=\"utf-8\">"
//        val m1R = "charset=\"utf-8\"/>"
//        val m2 = "name=\"viewport\">"
//        val m2R = "name=\"viewport\"/>"
//        val h = html.replace(identifier, identifierR)
//        val h1 = h.replace(m1, m1R)
//        val h2 = h1.replace(m2, m2R)
//        return h2
//    }
//
//}