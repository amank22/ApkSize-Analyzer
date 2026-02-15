package com.gi.apksize.tasks

import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.device.DeviceSpecParser
import com.android.tools.build.bundletool.device.ModuleMatcher
import com.android.tools.build.bundletool.model.AppBundle
import com.android.tools.build.bundletool.model.ModuleDeliveryType
import com.gi.apksize.models.*
import com.gi.apksize.processors.*
import com.gi.apksize.utils.Printer
import com.google.common.collect.ImmutableSet
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * Orchestrates AAB analysis by running bundle-specific processors in parallel.
 * Parses the AAB once via [AppBundle.buildFromZip] and shares the result
 * across all processors via [BundleHolder].
 */
object BundleStatsTask : Task {
    private data class GeneratedInstallTimeApk(
        val apkFile: File,
        val includedModules: Set<String>,
        val usedDeviceSpec: Boolean,
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun process(dataHolder: DataHolder): ApkStats {
        val apkStats = ApkStats()
        apkStats.inputFileType = InputFileType.AAB
        val analyzerOptions = dataHolder.analyzerOptions

        val aabFile = dataHolder.primaryFile.file
        Printer.log("Opening AAB file: ${aabFile.absolutePath}")

        val zipFile = ZipFile(aabFile)
        val appBundle = AppBundle.buildFromZip(zipFile)
        val bundleHolder = BundleHolder(appBundle, zipFile)

        Printer.log("AAB parsed: ${appBundle.modules.size} modules found")

        val generatedInstallTimeApk = if (analyzerOptions.useInstallTimeApkForAabAnalysis) {
            kotlin.runCatching {
                generateInstallTimeApk(dataHolder, appBundle)
            }.onFailure {
                Printer.log("Failed to generate install-time APK, falling back to bundle-based analysis: ${it.message}")
            }.getOrNull()
        } else null

        val installTimeDataHolder = generatedInstallTimeApk?.let { generated ->
            dataHolder.copy(
                primaryFile = FileHolder(
                    file = generated.apkFile,
                    proguardFile = dataHolder.primaryFile.proguardFile
                )
            )
        }
        val runApkProcessors = installTimeDataHolder != null
        apkStats.isInstallTimeApkAnalysis = runApkProcessors
        if (runApkProcessors) {
            val generated = generatedInstallTimeApk!!
            val mode = if (generated.usedDeviceSpec) "device-specific" else "generic"
            Printer.log(
                "Using generated install-time APK for analysis ($mode). " +
                        "Included modules: ${generated.includedModules.joinToString(", ")}"
            )
        }

        // Load LOB mappings if configured
        val lobContext = if (analyzerOptions.moduleMappingsPath.isNotBlank()) {
            kotlin.runCatching {
                val path = analyzerOptions.getPath(analyzerOptions.moduleMappingsPath)
                LobContext.load(
                    path = path,
                    isAab = !runApkProcessors,
                    appPackagePrefixes = analyzerOptions.appPackagePrefix,
                )
            }.onFailure {
                Printer.log("Failed to load LOB mappings: ${it.message}")
            }.getOrNull()
        } else null

        try {
            val numberOfProcessors = Runtime.getRuntime().availableProcessors()
            val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)

            val processors: List<List<Processor<*>>> = buildList {
                if (runApkProcessors) {
                    // Group 1: Keep AAB module breakdown + run APK-level size/file analysis.
                    add(
                        listOf(
                            BundleSizeProcessor(bundleHolder),
                            BasicSizeProcessor(false),
                            ApkGeneralFileProcessor(lobContext)
                        )
                    )
                    // Group 2: Resource analysis still comes from bundle metadata/resources.
                    add(listOf(BundleResourceProcessor(bundleHolder)))
                    // Group 3: DEX analysis from generated install-time APK.
                    add(listOf(DexFileProcessor(DexProcessorHolder(isCompareFile = false, needAppPackages = true), lobContext)))
                } else {
                    // Fallback to original bundle-based processors.
                    add(listOf(BundleSizeProcessor(bundleHolder), BundleFileProcessor(bundleHolder, lobContext)))
                    add(listOf(BundleResourceProcessor(bundleHolder)))
                    add(listOf(BundleDexProcessor(bundleHolder, lobContext)))
                }
                // Group 4: Metadata extraction
                add(listOf(BundleMetadataProcessor(bundleHolder)))
                // Group 5: Estimated device download sizes (standalone, no BundleHolder needed)
                add(listOf(BundleEstimatedSizeProcessor()))
            }

            processors.forEach { listOfProcess ->
                Printer.log("listOfProcess = " + listOfProcess.size)
                pool.submit {
                    listOfProcess.forEach { process ->
                        Printer.log(process.preMsg())
                        kotlin.runCatching {
                            val targetDataHolder = resolveDataHolderForProcess(
                                process = process,
                                aabDataHolder = dataHolder,
                                installTimeApkDataHolder = installTimeDataHolder
                            )
                            runProcess(process, targetDataHolder, apkStats)
                        }.onFailure {
                            it.printStackTrace()
                            Printer.log(process.failedMsg(it))
                        }.onSuccess {
                            Printer.log(process.postMsg())
                        }
                        Printer.log("process = ${process.name} done")
                    }
                }
            }

            pool.shutdown()
            try {
                pool.awaitTermination(analyzerOptions.executionTimeOut, TimeUnit.MINUTES)
            } catch (e: InterruptedException) {
                Printer.log(
                    "Execution timed-out. Try adjusting executionTimeOut value in config.json. " +
                            "Current is ${analyzerOptions.executionTimeOut} minutes"
                )
            }
        } finally {
            bundleHolder.close()
            generatedInstallTimeApk?.let { generated ->
                kotlin.runCatching {
                    Files.deleteIfExists(generated.apkFile.toPath())
                }.onFailure {
                    Printer.log("Failed to cleanup generated install-time APK: ${it.message}")
                }
            }
        }

        // Finalize LOB analysis after all processors complete
        if (lobContext != null) {
            apkStats.lobAnalysis = lobContext.buildResult()
            apkStats.unmatchedDetails = lobContext.unmatchedDetails
            apkStats.attributedDetails = lobContext.attributedDetails
            apkStats.dexOverheadDetails = lobContext.dexOverheadDetails
        }

        return apkStats
    }

    /**
     * Selects the data holder that should be used by a processor.
     * Bundle processors consume the original AAB holder, while APK processors consume the
     * generated install-time APK holder when available.
     */
    private fun resolveDataHolderForProcess(
        process: Processor<*>,
        aabDataHolder: DataHolder,
        installTimeApkDataHolder: DataHolder?,
    ): DataHolder {
        if (installTimeApkDataHolder == null) return aabDataHolder
        return when (process) {
            is BasicSizeProcessor,
            is ApkGeneralFileProcessor,
            is DexFileProcessor -> installTimeApkDataHolder
            else -> aabDataHolder
        }
    }

    /**
     * Builds a temporary install-time APK from AAB and returns its file handle.
     * The APK includes base module and install-time feature modules. If device-spec is provided,
     * conditional install-time modules are included only when they match the spec.
     */
    private fun generateInstallTimeApk(
        dataHolder: DataHolder,
        appBundle: AppBundle,
    ): GeneratedInstallTimeApk {
        val analyzerOptions = dataHolder.analyzerOptions
        val aapt2Path = resolveAapt2Path(analyzerOptions)
            ?: error("aapt2 not found. Set aapt2Executor in config or ANDROID_HOME/ANDROID_SDK_ROOT.")

        val moduleMatcher = buildModuleMatcher(analyzerOptions)
        val installTimeModules = resolveInstallTimeModules(appBundle, moduleMatcher)
        if (installTimeModules.isEmpty()) {
            error("No install-time modules found in the bundle.")
        }

        val tempApksPath = Files.createTempFile("bundle-install-time-", ".apks")
        try {
            val buildCommand = BuildApksCommand.builder()
                .setBundlePath(dataHolder.primaryFile.file.toPath())
                .setOutputFile(tempApksPath)
                .setOverwriteOutput(true)
                .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2Path))
                .setApkBuildMode(BuildApksCommand.ApkBuildMode.UNIVERSAL)
                .setModules(ImmutableSet.copyOf(installTimeModules))
                .build()

            Printer.log("Generating install-time universal APK from AAB...")
            buildCommand.execute()

            val apkFile = extractUniversalApk(tempApksPath)
            return GeneratedInstallTimeApk(
                apkFile = apkFile,
                includedModules = installTimeModules,
                usedDeviceSpec = moduleMatcher != null
            )
        } finally {
            kotlin.runCatching {
                Files.deleteIfExists(tempApksPath)
            }.onFailure {
                Printer.log("Warning: failed to cleanup temp .apks archive: ${it.message}")
            }
        }
    }

    /**
     * Resolves the install-time feature modules to include in generated APK.
     * Includes base + always install-time modules; includes conditional modules only if
     * a matching device-spec is configured and the module matches its targeting.
     */
    private fun resolveInstallTimeModules(
        appBundle: AppBundle,
        moduleMatcher: ModuleMatcher?,
    ): Set<String> {
        val modulesByName = appBundle.modules.entries.associate { it.key.name to it.value }
        val selectedSeed = linkedSetOf<String>()

        for ((moduleName, module) in modulesByName) {
            if (module.isBaseModule) {
                selectedSeed.add(moduleName)
                continue
            }
            val include = when (module.deliveryType) {
                ModuleDeliveryType.ALWAYS_INITIAL_INSTALL -> true
                ModuleDeliveryType.CONDITIONAL_INITIAL_INSTALL ->
                    moduleMatcher?.matchesModuleTargeting(module.moduleMetadata.targeting) == true
                else -> false
            }
            if (!include) continue
            if (!module.isIncludedInFusing) {
                Printer.log("Skipping install-time module '$moduleName' as it is not included in fusing.")
                continue
            }
            selectedSeed.add(moduleName)
        }

        val selectedWithDependencies = linkedSetOf<String>()
        fun includeWithDependencies(moduleName: String) {
            if (!selectedWithDependencies.add(moduleName)) return
            val module = modulesByName[moduleName] ?: return
            module.dependencies.forEach { dependency ->
                includeWithDependencies(dependency)
            }
        }
        selectedSeed.forEach(::includeWithDependencies)

        return selectedWithDependencies
    }

    /**
     * Extracts generated universal APK from the .apks archive into a temp file.
     */
    private fun extractUniversalApk(apksArchivePath: Path): File {
        val tempApkPath = Files.createTempFile("bundle-install-time-", ".apk")
        ZipFile(apksArchivePath.toFile()).use { apksZip ->
            val apkEntries = mutableListOf<java.util.zip.ZipEntry>()
            val entries = apksZip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (!entry.isDirectory && entry.name.endsWith(".apk", ignoreCase = true)) {
                    apkEntries.add(entry)
                }
            }
            if (apkEntries.isEmpty()) {
                error("No APK entries found in generated .apks archive.")
            }

            val selectedEntry = apkEntries.firstOrNull { it.name.contains("universal", ignoreCase = true) }
                ?: apkEntries.maxByOrNull { entry ->
                    if (entry.size >= 0L) entry.size else entry.compressedSize.coerceAtLeast(0L)
                }
                ?: error("Could not select generated APK entry from .apks archive.")

            apksZip.getInputStream(selectedEntry).use { input ->
                Files.copy(input, tempApkPath, StandardCopyOption.REPLACE_EXISTING)
            }
            Printer.log("Extracted install-time APK: ${selectedEntry.name}")
        }
        return tempApkPath.toFile()
    }

    /**
     * Builds [ModuleMatcher] from configured device-spec path, if provided.
     */
    private fun buildModuleMatcher(options: AnalyzerOptions): ModuleMatcher? {
        if (options.aabDeviceSpecPath.isBlank()) return null
        val resolvedPath = options.getPath(options.aabDeviceSpecPath)
        val specFile = File(resolvedPath)
        if (!specFile.exists()) {
            Printer.log("Configured aabDeviceSpecPath does not exist: $resolvedPath. Ignoring device-specific matching.")
            return null
        }
        return kotlin.runCatching {
            val deviceSpec = DeviceSpecParser.parsePartialDeviceSpec(specFile.toPath())
            ModuleMatcher(deviceSpec)
        }.onFailure {
            Printer.log("Failed to parse aabDeviceSpecPath '$resolvedPath': ${it.message}. Ignoring device-specific matching.")
        }.getOrNull()
    }

    /**
     * Resolves the aapt2 executable path.
     * Priority: 1) config aapt2Executor, 2) ANDROID_HOME/ANDROID_SDK_ROOT, 3) common SDK paths.
     */
    private fun resolveAapt2Path(options: AnalyzerOptions): Path? {
        if (options.aapt2Executor.isNotBlank()) {
            val configPath = File(options.aapt2Executor)
            if (configPath.exists() && configPath.canExecute()) {
                return configPath.toPath()
            }
            Printer.log("Configured aapt2Executor '${options.aapt2Executor}' not found or not executable")
        }

        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot != null) {
            val aapt2 = findAapt2InSdk(File(sdkRoot))
            if (aapt2 != null) return aapt2
        }

        val commonPaths = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk",
            "${System.getProperty("user.home")}/Android/Sdk",
            "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Local\\Android\\Sdk"
        )
        for (path in commonPaths) {
            val sdkDir = File(path)
            if (sdkDir.exists()) {
                val aapt2 = findAapt2InSdk(sdkDir)
                if (aapt2 != null) return aapt2
            }
        }
        return null
    }

    /**
     * Finds the latest executable aapt2 in SDK build-tools.
     */
    private fun findAapt2InSdk(sdkDir: File): Path? {
        val buildToolsDir = File(sdkDir, "build-tools")
        if (!buildToolsDir.exists()) return null
        return buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedDescending()
            ?.firstNotNullOfOrNull { versionDir ->
                val aapt2 = File(versionDir, "aapt2")
                if (aapt2.exists() && aapt2.canExecute()) aapt2.toPath() else null
            }
    }

    private fun runProcess(process: Processor<*>, dataHolder: DataHolder, apkStats: ApkStats) {
        when (process) {
            is SimpleProcessor -> {
                process.process(dataHolder, apkStats)
            }
            else -> throw Exception("Process ${process.name} is not configured to run")
        }
    }
}
