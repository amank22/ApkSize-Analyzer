package com.gi.apksize.tasks

import com.gi.apksize.models.*
import com.gi.apksize.ui.DiffHtmlGenerator
import com.gi.apksize.ui.HtmlGenerator
import com.gi.apksize.utils.Printer
import com.gi.apksize.utils.compareHolder
import com.gi.apksize.utils.primaryHolder
import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
//import com.gi.apksize.ui.PdfGenerator
import java.io.File

object ApkSizeTask {

    /**
     * Returns true if bundletool's Java API is available on the classpath (full JAR).
     */
    private fun isBundletoolEmbedded(): Boolean = try {
        Class.forName("com.android.tools.build.bundletool.model.AppBundle")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    fun evaluate(analyzerOptions: AnalyzerOptions) {
        val isAab = analyzerOptions.inputFileType() == InputFileType.AAB

        val holder = if (!analyzerOptions.isDiffMode) {
            analyzerOptions.primaryHolder()
        } else {
            analyzerOptions.compareHolder()
        }

        // AAB with lite JAR: check if we can handle it
        if (isAab && !isBundletoolEmbedded()) {
            if (analyzerOptions.bundletoolJarPath.isBlank()) {
                Printer.error(
                    "AAB analysis requires bundletool. Either use the full JAR (apkSize-*-full.jar) " +
                    "or set 'bundletoolJarPath' in config to point to a bundletool JAR."
                )
                return
            }
            val bundletoolJar = java.io.File(analyzerOptions.bundletoolJarPath)
            if (!bundletoolJar.exists()) {
                Printer.error("bundletoolJarPath does not exist: ${analyzerOptions.bundletoolJarPath}")
                return
            }
            Printer.log("Using external bundletool JAR: ${bundletoolJar.absolutePath}")
            evaluateAabViaCliAndApkAnalysis(analyzerOptions, holder)
            return
        }

        val task = when {
            analyzerOptions.isDiffMode -> CompareTask
            isAab -> BundleStatsTask
            else -> SingleStatsTask
        }

        val apkStats = task.process(holder)
        Printer.log("tasks done : $apkStats")
        apkStats.apkName = analyzerOptions.appName
        if (apkStats.inputFileType == null) {
            apkStats.inputFileType = analyzerOptions.inputFileType()
        }
        val outputFolder = holder.outputDir
        writeApkStatsToJsonFile(apkStats, outputFolder)
        Printer.log("writeApkStatsToJsonFile done")
        // Write LOB analysis JSON if present
        val lobAnalysis = apkStats.lobAnalysis
        if (lobAnalysis != null) {
            writeLobAnalysisToJsonFile(lobAnalysis, outputFolder)
            Printer.log("writeLobAnalysisToJsonFile done")
        }
        // Write unmatched details for LOB review
        val unmatchedDetails = apkStats.unmatchedDetails
        if (unmatchedDetails != null) {
            writeUnmatchedDetailsToJsonFile(unmatchedDetails, outputFolder)
            Printer.log("writeUnmatchedDetailsToJsonFile done")
        }
        // Write attributed details for LOB review
        val attributedDetails = apkStats.attributedDetails
        if (attributedDetails != null) {
            writeAttributedDetailsToJsonFile(attributedDetails, outputFolder)
            Printer.log("writeAttributedDetailsToJsonFile done")
        }
        // Write DEX overhead details
        val dexOverheadDetails = apkStats.dexOverheadDetails
        if (dexOverheadDetails != null) {
            writeDexOverheadDetailsToJsonFile(dexOverheadDetails, outputFolder)
            Printer.log("writeDexOverheadDetailsToJsonFile done")
        }
        val html = writeApkStatsToHtmlFile(apkStats, outputFolder, analyzerOptions.isDiffMode)
//        writeApkStatsToPdfFile(html, outputFolder)
        if (!analyzerOptions.isDiffMode && !isAab) {
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

    private fun writeDexOverheadDetailsToJsonFile(details: DexOverheadDetails, outputFolder: File) {
        val reportFile = File(outputFolder, "lob-dex-overhead.json")
        if (reportFile.exists()) {
            reportFile.delete()
            reportFile.createNewFile()
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        reportFile.writeText(gson.toJson(details))
    }

    private fun writeAttributedDetailsToJsonFile(details: AttributedDetails, outputFolder: File) {
        val reportFile = File(outputFolder, "lob-attributed-details.json")
        if (reportFile.exists()) {
            reportFile.delete()
            reportFile.createNewFile()
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        reportFile.writeText(gson.toJson(details))
    }

    private fun writeUnmatchedDetailsToJsonFile(details: UnmatchedDetails, outputFolder: File) {
        val reportFile = File(outputFolder, "lob-unmatched-details.json")
        if (reportFile.exists()) {
            reportFile.delete()
            reportFile.createNewFile()
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        reportFile.writeText(gson.toJson(details))
    }

    private fun writeLobAnalysisToJsonFile(lobAnalysis: LobAnalysisResult, outputFolder: File) {
        val reportFile = File(outputFolder, "lob-analysis.json")
        if (reportFile.exists()) {
            reportFile.delete()
            reportFile.createNewFile()
        }
        val gson = GsonBuilder().setPrettyPrinting().create()
        reportFile.writeText(gson.toJson(lobAnalysis))
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

    // -----------------------------------------------------------------------
    //  Lite JAR fallback: AAB → APK via external bundletool CLI, then analyze
    // -----------------------------------------------------------------------

    /**
     * Handles AAB analysis when bundletool is NOT on the classpath (lite JAR).
     * Uses external bundletool CLI to convert AAB → universal APK, then runs
     * standard APK analysis via [SingleStatsTask].
     */
    private fun evaluateAabViaCliAndApkAnalysis(
        analyzerOptions: AnalyzerOptions,
        holder: DataHolder,
    ) {
        val bundletoolJar = File(analyzerOptions.bundletoolJarPath)
        val aabPath = holder.primaryFile.file.absolutePath

        // 1. Generate universal APK from AAB via bundletool CLI
        val tempApksFile = Files.createTempFile("bundletool-cli-", ".apks").toFile()
        val tempApkFile = Files.createTempFile("bundletool-cli-universal-", ".apk").toFile()
        try {
            Printer.log("Generating universal APK from AAB via external bundletool CLI...")
            val buildApksArgs = mutableListOf(
                "java", "-jar", bundletoolJar.absolutePath,
                "build-apks",
                "--bundle=$aabPath",
                "--output=${tempApksFile.absolutePath}",
                "--overwrite",
                "--mode=universal"
            )

            // Add aapt2 path if available
            val aapt2Path = resolveAapt2ForCli(analyzerOptions)
            if (aapt2Path != null) {
                buildApksArgs.add("--aapt2=$aapt2Path")
            }

            val buildResult = runCliCommand(buildApksArgs)
            if (buildResult != 0) {
                Printer.error("bundletool build-apks failed (exit code $buildResult). Cannot analyze AAB with lite JAR.")
                return
            }

            // 2. Extract universal APK from the .apks zip archive
            extractUniversalApkFromArchive(tempApksFile, tempApkFile)
            Printer.log("Extracted universal APK for analysis.")

            // 3. Run standard APK analysis on the generated APK
            val apkHolder = holder.copy(
                primaryFile = FileHolder(
                    file = tempApkFile,
                    proguardFile = holder.primaryFile.proguardFile
                )
            )
            val apkStats = SingleStatsTask.process(apkHolder)
            apkStats.inputFileType = InputFileType.AAB
            apkStats.isInstallTimeApkAnalysis = true

            Printer.log("tasks done : $apkStats")
            apkStats.apkName = analyzerOptions.appName

            val outputFolder = holder.outputDir
            writeApkStatsToJsonFile(apkStats, outputFolder)
            Printer.log("writeApkStatsToJsonFile done")
            writeApkStatsToHtmlFile(apkStats, outputFolder, isDiffMode = false)

        } finally {
            kotlin.runCatching { tempApksFile.delete() }
            kotlin.runCatching { tempApkFile.delete() }
        }

        val fileTypeLabel = "AAB"
        Printer.log("Analysed $fileTypeLabel (via lite JAR + external bundletool). " +
                "Output at ${analyzerOptions.outputFolderPath}.")
    }

    private fun extractUniversalApkFromArchive(apksFile: File, targetApkFile: File) {
        ZipFile(apksFile).use { zip ->
            val apkEntries = zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .toList()
            if (apkEntries.isEmpty()) {
                error("No APK entries found in generated .apks archive.")
            }
            val entry = apkEntries.firstOrNull { it.name.contains("universal", ignoreCase = true) }
                ?: apkEntries.maxByOrNull { if (it.size >= 0) it.size else it.compressedSize.coerceAtLeast(0) }
                ?: error("Could not select APK entry from .apks archive.")
            zip.getInputStream(entry).use { input ->
                Files.copy(input, targetApkFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun runCliCommand(args: List<String>): Int {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        // Stream output to our logger
        process.inputStream.bufferedReader().forEachLine { line ->
            Printer.log("[bundletool] $line")
        }
        return process.waitFor()
    }

    private fun resolveAapt2ForCli(options: AnalyzerOptions): String? {
        if (options.aapt2Executor.isNotBlank()) {
            val f = File(options.aapt2Executor)
            if (f.exists() && f.canExecute()) return f.absolutePath
        }
        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: return null
        val buildToolsDir = File(sdkRoot, "build-tools")
        if (!buildToolsDir.exists()) return null
        return buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedDescending()
            ?.firstNotNullOfOrNull { dir ->
                val aapt2 = File(dir, "aapt2")
                if (aapt2.exists() && aapt2.canExecute()) aapt2.absolutePath else null
            }
    }

}