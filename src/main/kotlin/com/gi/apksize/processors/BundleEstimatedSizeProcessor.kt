package com.gi.apksize.processors

import com.android.bundle.Commands.BuildApksResult
import com.android.bundle.Devices.DeviceSpec
import com.android.bundle.SizesOuterClass.Breakdown
import com.android.bundle.SizesOuterClass.Sizes
import com.android.tools.build.bundletool.androidtools.Aapt2Command
import com.android.tools.build.bundletool.commands.BuildApksCommand
import com.android.tools.build.bundletool.device.ApkMatcher
import com.android.tools.build.bundletool.model.utils.ResultUtils
import com.android.tools.build.bundletool.size.ApkBreakdownGenerator
import com.gi.apksize.models.*
import com.gi.apksize.utils.Printer
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

/**
 * Estimates real-world download sizes for different device configurations.
 *
 * Pipeline:
 * 1. Uses bundletool's [BuildApksCommand] to generate split APKs from the AAB (unsigned).
 * 2. For each device tier (low / mid / high-end), finds the matching APK splits.
 * 3. Uses [ApkBreakdownGenerator] to calculate per-component compressed sizes for each APK.
 * 4. Sums the breakdowns to produce estimated download sizes that approximate Play Store numbers.
 */
class BundleEstimatedSizeProcessor : SimpleProcessor() {

    override val name: String = "Bundle Estimated Sizes"

    /**
     * Device tier definitions.
     * Each tier models a representative device configuration.
     */
    private data class DeviceTier(
        val configName: String,
        val abi: String,
        val screenDensityDpi: Int,
        val sdkVersion: Int,
        val supportedLocales: List<String> = listOf("en")
    )

    companion object {
        private val DEVICE_TIERS = listOf(
            DeviceTier(
                configName = "Low-end",
                abi = "armeabi-v7a",
                screenDensityDpi = 240,  // hdpi
                sdkVersion = 26
            ),
            DeviceTier(
                configName = "Mid-end",
                abi = "arm64-v8a",
                screenDensityDpi = 480,  // xxhdpi
                sdkVersion = 30
            ),
            DeviceTier(
                configName = "High-end",
                abi = "arm64-v8a",
                screenDensityDpi = 640,  // xxxhdpi
                sdkVersion = 34
            )
        )
    }

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val aabPath = dataHolder.primaryFile.file.toPath()
        val tempApksPath = Files.createTempFile("bundle-estimated-", ".apks")

        try {
            // Resolve aapt2 path (required by BuildApksCommand)
            val aapt2Path = resolveAapt2Path(dataHolder.analyzerOptions)
            if (aapt2Path == null) {
                Printer.log("WARNING: aapt2 not found. Skipping estimated download size calculation.")
                Printer.log("Set aapt2Executor in config or ANDROID_HOME environment variable.")
                return
            }
            Printer.log("Using aapt2 at: $aapt2Path")

            // Step 1: Generate split APKs from the AAB (unsigned)
            Printer.log("Generating split APKs from AAB for size estimation...")
            val builder = BuildApksCommand.builder()
                .setBundlePath(aabPath)
                .setOutputFile(tempApksPath)
                .setOverwriteOutput(true)
                .setAapt2Command(Aapt2Command.createFromExecutablePath(aapt2Path))
            builder.build().execute()
            Printer.log("Split APKs generated at: $tempApksPath")

            // Step 2: Read the table of contents
            val buildApksResult: BuildApksResult = ResultUtils.readTableOfContents(tempApksPath)
            Printer.log("TOC read: ${buildApksResult.variantCount} variants")

            // Step 3: For each device tier, find matching APKs and calculate breakdowns
            val breakdownGenerator = ApkBreakdownGenerator()
            val estimatedSizes = mutableListOf<EstimatedDeviceSize>()

            for (tier in DEVICE_TIERS) {
                try {
                    val estimatedSize = calculateForTier(
                        tier, buildApksResult, tempApksPath, breakdownGenerator
                    )
                    estimatedSizes.add(estimatedSize)
                    Printer.log(
                        "${tier.configName}: download=${estimatedSize.totalDownloadBytes / 1048576} MB, " +
                                "disk=${estimatedSize.totalDiskBytes / 1048576} MB"
                    )
                } catch (e: Exception) {
                    Printer.log("Failed to estimate size for ${tier.configName}: ${e.message}")
                    e.printStackTrace()
                }
            }

            apkStats.estimatedDeviceSizes = estimatedSizes
            Printer.log("Estimated download sizes calculated for ${estimatedSizes.size} device tiers")

        } catch (e: Exception) {
            Printer.log("Failed to generate estimated sizes: ${e.message}")
            e.printStackTrace()
        } finally {
            // Cleanup temp .apks file
            try {
                Files.deleteIfExists(tempApksPath)
                Printer.log("Cleaned up temp .apks file")
            } catch (e: Exception) {
                Printer.log("Warning: could not delete temp .apks: ${e.message}")
            }
        }
    }

    /**
     * Calculates estimated download sizes for a single device tier.
     */
    private fun calculateForTier(
        tier: DeviceTier,
        buildApksResult: BuildApksResult,
        apksArchivePath: Path,
        breakdownGenerator: ApkBreakdownGenerator
    ): EstimatedDeviceSize {
        // Build DeviceSpec for this tier
        val deviceSpec = DeviceSpec.newBuilder()
            .addSupportedAbis(tier.abi)
            .setScreenDensity(tier.screenDensityDpi)
            .setSdkVersion(tier.sdkVersion)
            .addAllSupportedLocales(tier.supportedLocales)
            .build()

        // Find matching APKs for this device
        val apkMatcher = ApkMatcher(deviceSpec)
        val matchingApks = apkMatcher.getMatchingApks(buildApksResult)

        Printer.log("${tier.configName}: ${matchingApks.size} matching APKs found")

        // Extract each matching APK from the archive and calculate its breakdown
        val tempDir = Files.createTempDirectory("bundle-est-apks-")
        try {
            ZipFile(apksArchivePath.toFile()).use { apksZip ->
                var totalBreakdown = emptyBreakdown()

                for (generatedApk in matchingApks) {
                    val apkPathInZip = generatedApk.path.toString()
                    val zipEntry = apksZip.getEntry(apkPathInZip)
                        ?: continue

                    // Extract APK to temp directory
                    val tempApkPath = tempDir.resolve(
                        apkPathInZip.replace("/", "_").replace("\\", "_")
                    )
                    apksZip.getInputStream(zipEntry).use { input: InputStream ->
                        Files.copy(input, tempApkPath, StandardCopyOption.REPLACE_EXISTING)
                    }

                    // Calculate breakdown for this APK
                    val breakdown = breakdownGenerator.calculateBreakdown(tempApkPath)
                    totalBreakdown = sumBreakdowns(totalBreakdown, breakdown)

                    // Cleanup temp APK immediately
                    Files.deleteIfExists(tempApkPath)
                }

                return EstimatedDeviceSize(
                    configName = tier.configName,
                    abi = tier.abi,
                    screenDensityDpi = tier.screenDensityDpi,
                    sdkVersion = tier.sdkVersion,
                    totalDownloadBytes = totalBreakdown.total.downloadSize,
                    totalDiskBytes = totalBreakdown.total.diskSize,
                    dexDownloadBytes = totalBreakdown.dex.downloadSize,
                    resourcesDownloadBytes = totalBreakdown.resources.downloadSize,
                    assetsDownloadBytes = totalBreakdown.assets.downloadSize,
                    nativeLibsDownloadBytes = totalBreakdown.nativeLibs.downloadSize,
                    otherDownloadBytes = totalBreakdown.other.downloadSize
                )
            }
        } finally {
            // Cleanup temp directory
            try {
                Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            } catch (e: Exception) {
                Printer.log("Warning: could not fully clean temp dir: ${e.message}")
            }
        }
    }

    /**
     * Creates an empty Breakdown proto with all sizes set to zero.
     */
    private fun emptyBreakdown(): Breakdown {
        val zeroSizes = Sizes.newBuilder().setDiskSize(0).setDownloadSize(0).build()
        return Breakdown.newBuilder()
            .setTotal(zeroSizes)
            .setDex(zeroSizes)
            .setResources(zeroSizes)
            .setAssets(zeroSizes)
            .setNativeLibs(zeroSizes)
            .setOther(zeroSizes)
            .build()
    }

    /**
     * Sums two Breakdown protos component-wise.
     */
    private fun sumBreakdowns(a: Breakdown, b: Breakdown): Breakdown {
        return Breakdown.newBuilder()
            .setTotal(addSizes(a.total, b.total))
            .setDex(addSizes(a.dex, b.dex))
            .setResources(addSizes(a.resources, b.resources))
            .setAssets(addSizes(a.assets, b.assets))
            .setNativeLibs(addSizes(a.nativeLibs, b.nativeLibs))
            .setOther(addSizes(a.other, b.other))
            .build()
    }

    /**
     * Adds two Sizes protos.
     */
    private fun addSizes(a: Sizes, b: Sizes): Sizes {
        return Sizes.newBuilder()
            .setDiskSize(a.diskSize + b.diskSize)
            .setDownloadSize(a.downloadSize + b.downloadSize)
            .build()
    }

    /**
     * Resolves the aapt2 executable path.
     * Priority: 1) config aapt2Executor, 2) ANDROID_HOME env, 3) common macOS/Linux paths.
     */
    private fun resolveAapt2Path(options: AnalyzerOptions): Path? {
        // 1. From config
        if (options.aapt2Executor.isNotBlank()) {
            val configPath = File(options.aapt2Executor)
            if (configPath.exists() && configPath.canExecute()) {
                return configPath.toPath()
            }
            Printer.log("Configured aapt2Executor '${options.aapt2Executor}' not found or not executable")
        }

        // 2. From ANDROID_HOME or ANDROID_SDK_ROOT environment variable
        val sdkRoot = System.getenv("ANDROID_HOME")
            ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot != null) {
            val aapt2 = findAapt2InSdk(File(sdkRoot))
            if (aapt2 != null) return aapt2
        }

        // 3. Common paths
        val commonPaths = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk",  // macOS
            "${System.getProperty("user.home")}/Android/Sdk",          // Linux
            "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Local\\Android\\Sdk"  // Windows
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
     * Finds the latest aapt2 binary in an Android SDK's build-tools directory.
     */
    private fun findAapt2InSdk(sdkDir: File): Path? {
        val buildToolsDir = File(sdkDir, "build-tools")
        if (!buildToolsDir.exists()) return null

        // Pick the latest version directory that contains aapt2
        return buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedDescending()
            ?.firstNotNullOfOrNull { versionDir ->
                val aapt2 = File(versionDir, "aapt2")
                if (aapt2.exists() && aapt2.canExecute()) aapt2.toPath() else null
            }
    }
}
