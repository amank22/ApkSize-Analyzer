package com.gi.apksize.tasks

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.processors.ApkFileProcessor
import com.gi.apksize.processors.DiffProcessor
import com.gi.apksize.ui.DiffHtmlGenerator
import com.gi.apksize.ui.HtmlGenerator
import com.gi.apksize.ui.PdfGenerator
import org.jetbrains.kotlin.util.removeSuffixIfPresent
import java.io.File

object ApkSizeTask {

    fun evaluate(analyzerOptions: AnalyzerOptions) {
        val proguardFilePath = if (analyzerOptions.inputFileProguardPath.isBlank())
            null
        else analyzerOptions.getPath(analyzerOptions.inputFileProguardPath)
        val releaseApkPath = analyzerOptions.getPath(analyzerOptions.inputFilePath)
        val outputDirectory = if (analyzerOptions.isDiffMode) {
            analyzerOptions.getPath(analyzerOptions.outputFolderPath)
                .removeSuffixIfPresent("/") + "/diffs/"
        } else {
            analyzerOptions.getPath(analyzerOptions.outputFolderPath)
        }

        val releaseApkFile = File(releaseApkPath)
        val outputFolder = File(outputDirectory)
        val proguardMappingFile = if (proguardFilePath.isNullOrBlank()) null else File(proguardFilePath)
        if (!releaseApkFile.exists()) {
            throw Exception("Apk file does not exists on $releaseApkPath")
        }
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }
        val apkStats = if (!analyzerOptions.isDiffMode) {
            ApkFileProcessor.apkStudioAnalyzerTools(releaseApkFile, proguardMappingFile, analyzerOptions)
        } else {
            diffTask(analyzerOptions, releaseApkFile, proguardMappingFile)
        }
        apkStats.apkName = analyzerOptions.appName
        writeApkStatsToJsonFile(apkStats, outputFolder)
        val html = writeApkStatsToHtmlFile(apkStats, outputFolder, analyzerOptions.isDiffMode)
        writeApkStatsToPdfFile(html, outputFolder)
        if (!analyzerOptions.isDiffMode) {
            writeAaptStatsToJsonFile(apkStats, outputFolder)
        }
    }

    private fun diffTask(
        analyzerOptions: AnalyzerOptions,
        releaseApkFile: File,
        proguardMappingFile: File?
    ): ApkStats {
        val compareProguardFilePath = if (analyzerOptions.compareFileProguardPath.isBlank())
            null
        else analyzerOptions.getPath(analyzerOptions.compareFileProguardPath)
        val compareApkPath = analyzerOptions.getPath(analyzerOptions.compareFilePath)
        val compareApkFile = File(compareApkPath)
        val compareProguardMappingFile =
            if (compareProguardFilePath.isNullOrBlank()) null else File(compareProguardFilePath)
        if (!compareApkFile.exists()) {
            throw Exception("Apk file does not exists on $compareApkPath")
        }
        return DiffProcessor.calculate(
            releaseApkFile, proguardMappingFile,
            compareApkFile, compareProguardMappingFile, analyzerOptions
        )
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
        PdfGenerator.generate(html, apkSizeReportFile)
    }

}