package com.gi.apksize.tasks

import com.gi.apksize.models.*
import com.gi.apksize.ui.DiffHtmlGenerator
import com.gi.apksize.ui.HtmlGenerator
import com.gi.apksize.utils.Aapt2Resolver
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
        writeAllOutputFiles(apkStats, outputFolder, analyzerOptions.isDiffMode)
        if (!analyzerOptions.isDiffMode && !isAab) {
            writeAaptStatsToJsonFile(apkStats, outputFolder)
        }
    }

    /**
     * Writes all standard output files: apksize.json, LOB analysis JSONs, and index.html.
     */
    private fun writeAllOutputFiles(apkStats: ApkStats, outputFolder: File, isDiffMode: Boolean) {
        writeApkStatsToJsonFile(apkStats, outputFolder)
        Printer.log("writeApkStatsToJsonFile done")

        apkStats.lobAnalysis?.let {
            writeLobAnalysisToJsonFile(it, outputFolder)
            Printer.log("writeLobAnalysisToJsonFile done")
        }
        apkStats.unmatchedDetails?.let {
            writeUnmatchedDetailsToJsonFile(it, outputFolder)
            Printer.log("writeUnmatchedDetailsToJsonFile done")
        }
        apkStats.attributedDetails?.let {
            writeAttributedDetailsToJsonFile(it, outputFolder)
            Printer.log("writeAttributedDetailsToJsonFile done")
        }
        apkStats.dexOverheadDetails?.let {
            writeDexOverheadDetailsToJsonFile(it, outputFolder)
            Printer.log("writeDexOverheadDetailsToJsonFile done")
        }

        writeApkStatsToHtmlFile(apkStats, outputFolder, isDiffMode)
        Printer.log("writeApkStatsToHtmlFile done")
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
     * Mirrors [BundleStatsTask] behaviour as closely as possible:
     *  - When [AnalyzerOptions.useInstallTimeApkForAabAnalysis] is true, discovers
     *    install-time modules from the AAB and passes `--modules=…` so the generated
     *    universal APK only contains those modules (matching the full-JAR path).
     *  - Otherwise builds a universal APK from all modules.
     *
     * Limitations vs full JAR: bundle-level processors (module size breakdown,
     * resource analysis, metadata extraction, estimated download sizes) cannot run
     * because they require the bundletool Java API.
     */
    private fun evaluateAabViaCliAndApkAnalysis(
        analyzerOptions: AnalyzerOptions,
        holder: DataHolder,
    ) {
        val bundletoolJar = File(analyzerOptions.bundletoolJarPath)
        val aabPath = holder.primaryFile.file.absolutePath

        val tempApksFile = Files.createTempFile("bundletool-cli-", ".apks").toFile()
        val tempApkFile = Files.createTempFile("bundletool-cli-universal-", ".apk").toFile()
        try {
            // 1. Determine which modules to include
            val installTimeModules = if (analyzerOptions.useInstallTimeApkForAabAnalysis) {
                kotlin.runCatching {
                    resolveInstallTimeModulesViaCli(bundletoolJar, aabPath)
                }.onFailure {
                    Printer.log("Failed to discover install-time modules, including all: ${it.message}")
                }.getOrNull()
            } else null

            val filteredModules = !installTimeModules.isNullOrEmpty()

            // 2. Build universal APK via bundletool CLI
            val buildApksArgs = mutableListOf(
                "java", "-jar", bundletoolJar.absolutePath,
                "build-apks",
                "--bundle=$aabPath",
                "--output=${tempApksFile.absolutePath}",
                "--overwrite",
                "--mode=universal"
            )
            if (filteredModules) {
                buildApksArgs.add("--modules=${installTimeModules!!.joinToString(",")}")
                Printer.log("Building install-time universal APK with modules: ${installTimeModules.joinToString(", ")}")
            } else {
                Printer.log("Building universal APK with all modules.")
            }
            val aapt2Path = Aapt2Resolver.resolve(analyzerOptions)
            if (aapt2Path != null) {
                buildApksArgs.add("--aapt2=$aapt2Path")
            }

            var buildResult = runCliCommand(buildApksArgs)

            // If module-filtered build fails, retry with all modules as fallback
            if (buildResult != 0 && filteredModules) {
                Printer.log("Module-filtered build failed (exit $buildResult), retrying with all modules...")
                kotlin.runCatching { tempApksFile.delete() }
                val retryArgs = buildApksArgs.filterNot { it.startsWith("--modules=") }.toMutableList()
                buildResult = runCliCommand(retryArgs)
            }
            if (buildResult != 0) {
                Printer.error("bundletool build-apks failed (exit code $buildResult). Cannot analyze AAB with lite JAR.")
                return
            }

            // 3. Extract universal APK from .apks archive
            extractUniversalApkFromArchive(tempApksFile, tempApkFile)
            Printer.log("Extracted universal APK for analysis.")

            // 4. Run standard APK analysis
            val apkHolder = holder.copy(
                primaryFile = FileHolder(
                    file = tempApkFile,
                    proguardFile = holder.primaryFile.proguardFile
                )
            )
            val apkStats = SingleStatsTask.process(apkHolder)
            apkStats.inputFileType = InputFileType.AAB
            apkStats.isInstallTimeApkAnalysis = filteredModules
            apkStats.apkName = analyzerOptions.appName

            Printer.log("tasks done : $apkStats")

            val outputFolder = holder.outputDir
            writeAllOutputFiles(apkStats, outputFolder, isDiffMode = false)
        } finally {
            kotlin.runCatching { tempApksFile.delete() }
            kotlin.runCatching { tempApkFile.delete() }
        }

        Printer.log(
            "Analysed AAB (via lite JAR + external bundletool). " +
                    "Output at ${analyzerOptions.outputFolderPath}."
        )
    }

    // -----------------------------------------------------------------------
    //  Install-time module discovery via CLI
    // -----------------------------------------------------------------------

    /**
     * Discovers install-time modules from an AAB by enumerating its module
     * directories, then using `bundletool dump manifest` per module to check
     * delivery type and fusing — mirroring [BundleStatsTask.resolveInstallTimeModules].
     *
     * Conditional-delivery modules are excluded (matching full-JAR behaviour
     * when no device-spec is provided).
     */
    private fun resolveInstallTimeModulesViaCli(
        bundletoolJar: File,
        aabPath: String,
    ): Set<String>? {
        val moduleNames = discoverModulesFromAab(aabPath)
        if (moduleNames.isEmpty()) {
            Printer.log("No modules discovered from AAB.")
            return null
        }
        Printer.log("Discovered ${moduleNames.size} modules in AAB: ${moduleNames.joinToString(", ")}")

        val installTimeModules = linkedSetOf<String>()

        for (moduleName in moduleNames) {
            if (moduleName == "base") {
                installTimeModules.add(moduleName)
                continue
            }

            val manifestXml = dumpModuleManifest(bundletoolJar, aabPath, moduleName)
            if (manifestXml == null) {
                Printer.log("Module '$moduleName': could not dump manifest, skipping")
                continue
            }

            val hasInstallTimeDelivery = manifestXml.contains("<dist:install-time")
            val hasConditionalDelivery = manifestXml.contains("<dist:conditions")
            val hasOnDemandDelivery = manifestXml.contains("<dist:on-demand")
            val isIncludedInFusing = manifestXml.contains("dist:include=\"true\"")
                    || manifestXml.contains("dist:include='true'")

            val isUnconditionalInstallTime =
                hasInstallTimeDelivery && !hasConditionalDelivery && !hasOnDemandDelivery

            if (isUnconditionalInstallTime && isIncludedInFusing) {
                installTimeModules.add(moduleName)
                Printer.log("Module '$moduleName': install-time, fusing=true → included")
            } else {
                Printer.log(
                    "Module '$moduleName': excluded " +
                            "(installTime=$hasInstallTimeDelivery, conditional=$hasConditionalDelivery, " +
                            "onDemand=$hasOnDemandDelivery, fusing=$isIncludedInFusing)"
                )
            }
        }

        return installTimeModules.ifEmpty { null }
    }

    /**
     * Enumerates module directories inside an AAB zip (top-level dirs that are
     * not metadata entries like `META-INF/` or root proto files).
     */
    private fun discoverModulesFromAab(aabPath: String): Set<String> {
        return ZipFile(File(aabPath)).use { zip ->
            zip.entries().asSequence()
                .map { it.name.substringBefore('/') }
                .filter { name ->
                    name.isNotEmpty() && !name.endsWith(".pb") && !name.startsWith("META-INF")
                            && !name.contains('.')
                }
                .toSortedSet()
        }
    }

    private fun dumpModuleManifest(bundletoolJar: File, aabPath: String, moduleName: String): String? {
        return kotlin.runCatching {
            runCliCommandAndCapture(
                listOf(
                    "java", "-jar", bundletoolJar.absolutePath,
                    "dump", "manifest",
                    "--bundle=$aabPath",
                    "--module=$moduleName"
                )
            )
        }.onFailure {
            Printer.log("bundletool dump manifest failed for '$moduleName': ${it.message}")
        }.getOrNull()
    }

    // -----------------------------------------------------------------------
    //  CLI helpers
    // -----------------------------------------------------------------------

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
            Printer.log("Extracted APK entry: ${entry.name}")
        }
    }

    /** Runs a CLI command, streaming stdout/stderr to [Printer], and returns exit code. */
    private fun runCliCommand(args: List<String>): Int {
        val process = ProcessBuilder(args)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().forEachLine { line ->
            Printer.log("[bundletool] $line")
        }
        return process.waitFor()
    }

    /** Runs a CLI command and captures stdout. Returns null on non-zero exit. */
    private fun runCliCommandAndCapture(args: List<String>): String? {
        val process = ProcessBuilder(args)
            .redirectErrorStream(false)
            .start()
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            Printer.log("CLI command failed (exit $exitCode): ${args.joinToString(" ")}")
            if (stderr.isNotBlank()) Printer.log("stderr: $stderr")
            return null
        }
        return stdout
    }

}