package com.gi.apksize.tasks

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.processors.ApkFileProcessor
import com.gi.apksize.ui.HtmlGenerator
import com.gi.apksize.ui.PdfGenerator
import org.jetbrains.kotlin.utils.strings.substringWithContext
import java.io.File

object ApkSizeTask {

    fun evaluateSize(
        isAbsolutePaths: Boolean, input: String, output: String, proguard: String?,
        analyzerOptions: AnalyzerOptions
    ) {
        val proguardFilePath = if (proguard.isNullOrBlank()) null else getPath(isAbsolutePaths, proguard)
        val releaseApkPath = getPath(isAbsolutePaths, input)
        val outputDirectory = getPath(isAbsolutePaths, output)

        val releaseApkFile = File(releaseApkPath)
        val outputFolder = File(outputDirectory)
        val proguardMappingFile = if (proguardFilePath.isNullOrBlank()) null else File(proguardFilePath)
        if (!releaseApkFile.exists()) {
            throw Exception("Apk file does not exists")
        }
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }
        val apkStats = ApkFileProcessor.apkStudioAnalyzerTools(releaseApkFile, proguardMappingFile, analyzerOptions)
        apkStats.apkName = analyzerOptions.appName
        writeAaptStatsToJsonFile(apkStats, outputFolder)
        writeApkStatsToJsonFile(apkStats, outputFolder)
        val html = writeApkStatsToHtmlFile(apkStats, outputFolder)
        writeApkStatsToPdfFile(html, outputFolder)
    }

    private fun getPath(isAbsolutePaths: Boolean, path: String): String {
        return if (isAbsolutePaths) {
            path
        } else {
            val currentPath = System.getProperty("user.dir")
            val updatedPath = if (!path.startsWith("/")) {
                "$path/"
            } else path
            currentPath + updatedPath
        }
    }

    private fun writeAaptStatsToJsonFile(apkStats: ApkStats, outputFolder: File) {
        val data = apkStats.aaptData?:return
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

    private fun writeApkStatsToHtmlFile(apkStats: ApkStats, outputFolder: File) : String {
        val apkSizeReportFile = File(outputFolder, "index.html")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
        val html = HtmlGenerator.getHtml(apkStats)
        apkSizeReportFile.writeText(html)
        return html
    }

    private fun writeApkStatsToPdfFile(html: String, outputFolder: File) {
        val apkSizeReportFile = File(outputFolder, "report.pdf")
        if (apkSizeReportFile.exists()) {
            apkSizeReportFile.delete()
            apkSizeReportFile.createNewFile()
        }
        PdfGenerator.generate(html, apkSizeReportFile)
    }

}