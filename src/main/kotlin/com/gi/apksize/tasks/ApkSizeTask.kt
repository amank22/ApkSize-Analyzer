package com.gi.apksize.tasks

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.ui.DiffHtmlGenerator
import com.gi.apksize.ui.HtmlGenerator
import com.gi.apksize.utils.Printer
import com.gi.apksize.utils.compareHolder
import com.gi.apksize.utils.primaryHolder
//import com.gi.apksize.ui.PdfGenerator
import java.io.File

object ApkSizeTask {

    fun evaluate(analyzerOptions: AnalyzerOptions) {
        val holder = if (!analyzerOptions.isDiffMode) {
            analyzerOptions.primaryHolder()
        } else {
            analyzerOptions.compareHolder()
        }
        val task = if (!analyzerOptions.isDiffMode) {
            SingleStatsTask
        } else {
            CompareTask
        }
        val apkStats = task.process(holder)
        Printer.log("tasks done : $apkStats")
        apkStats.apkName = analyzerOptions.appName
        val outputFolder = holder.outputDir
        writeApkStatsToJsonFile(apkStats, outputFolder)
        Printer.log("writeApkStatsToJsonFile done")
        val html = writeApkStatsToHtmlFile(apkStats, outputFolder, analyzerOptions.isDiffMode)
//        writeApkStatsToPdfFile(html, outputFolder)
        if (!analyzerOptions.isDiffMode) {
            writeAaptStatsToJsonFile(apkStats, outputFolder)
        }
    }

    private fun writeAaptStatsToJsonFile(apkStats: ApkStats, outputFolder: File) {
        val data = apkStats.aaptData ?: return
        val apkSizeReportFile = File(outputFolder, "aapt_stats.txt")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
        apkSizeReportFile.writeText(data)
        apkStats.aaptData = null
    }

    private fun writeApkStatsToJsonFile(apkStats: ApkStats, outputFolder: File) {
        val apkSizeReportFile = File(outputFolder, "apksize.json")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
        apkSizeReportFile.writeText(apkStats.json())
    }

    private fun writeApkStatsToHtmlFile(
        apkStats: ApkStats, outputFolder: File,
        isDiffMode: Boolean
    ): String {
        val apkSizeReportFile = File(outputFolder, "index.html")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
        val html = if (isDiffMode) {
            DiffHtmlGenerator.getHtml(apkStats)
        } else {
            HtmlGenerator.getHtml(apkStats)
        }
        apkSizeReportFile.writeText(html)
        return html
    }

    private fun writeApkStatsToPdfFile(html: String, outputFolder: File) {
        val apkSizeReportFile = File(outputFolder, "report.pdf")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
//        PdfGenerator.generate(html, apkSizeReportFile)
    }

}