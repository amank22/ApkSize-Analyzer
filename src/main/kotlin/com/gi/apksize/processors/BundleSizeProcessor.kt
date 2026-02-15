package com.gi.apksize.processors

import com.gi.apksize.models.*
import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Constants
import com.gi.apksize.utils.Printer
import com.android.tools.build.bundletool.model.AppBundle
import com.android.tools.build.bundletool.model.BundleModule
import com.android.tools.build.bundletool.model.utils.GZipUtils
import java.nio.file.Files
import java.util.zip.ZipFile

/**
 * Calculates raw file size and estimated download (GZip) size for an AAB file.
 * Also computes per-module size breakdown by component (DEX, resources, assets, native libs, other).
 */
class BundleSizeProcessor(private val bundleHolder: BundleHolder) : SimpleProcessor() {

    override val name: String = "Bundle Sizes"

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val aabPath = dataHolder.primaryFile.file.toPath()
        val appBundle = bundleHolder.appBundle

        // Raw file size
        val rawSize = Files.size(aabPath)
        apkStats.apkSize = rawSize
        apkStats.apkSizeInMb = ApkSizeHelpers.roundOffDecimal(rawSize / Constants.BYTE_TO_MB_DIVIDER)

        // Estimated download size (GZip compressed)
        try {
            val downloadSize = GZipUtils.calculateGzipCompressedSize(aabPath)
            apkStats.downloadSize = downloadSize
            apkStats.downloadSizeInMb = ApkSizeHelpers.roundOffDecimal(downloadSize / Constants.BYTE_TO_MB_DIVIDER)
        } catch (e: Exception) {
            Printer.log("Failed to calculate GZip download size: ${e.message}")
        }

        // Build compressed-size lookup from the AAB ZIP entries
        // AAB ZIP paths are like "base/dex/classes.dex", "module/lib/arm64-v8a/libfoo.so"
        val compressedSizeMap = mutableMapOf<String, Long>()
        try {
            ZipFile(dataHolder.primaryFile.file).use { zip ->
                for (entry in zip.entries()) {
                    compressedSizeMap[entry.name] = entry.compressedSize.coerceAtLeast(0L)
                }
            }
        } catch (e: Exception) {
            Printer.log("Could not read AAB ZIP for compressed sizes: ${e.message}")
        }

        // Per-module breakdown (two-pass: first compute sizes, then percentages)
        val rawModules = appBundle.modules.map { (moduleName, module) ->
            calculateModuleBreakdown(moduleName.name, module, compressedSizeMap, 0L)
        }
        val totalCompressedSize = rawModules.sumOf { it.totalSize }
        // Second pass: recalculate with correct total for percentage
        val moduleInfoList = appBundle.modules.map { (moduleName, module) ->
            calculateModuleBreakdown(moduleName.name, module, compressedSizeMap, totalCompressedSize)
        }
        apkStats.bundleModules = moduleInfoList.sortedByDescending { it.totalSize }
        Printer.log("Analyzed ${moduleInfoList.size} modules in AAB")
    }

    /**
     * Calculates per-component size breakdown for a single module.
     * Uses compressed (in-ZIP) sizes so numbers reflect what's actually stored in the AAB.
     * Classifies entries by path prefix: dex/, res/, assets/, lib/, or other.
     */
    private fun calculateModuleBreakdown(
        moduleName: String,
        module: BundleModule,
        compressedSizeMap: Map<String, Long>,
        totalAabSize: Long
    ): BundleModuleInfo {
        var dexSize = 0L
        var resourcesSize = 0L
        var assetsSize = 0L
        var nativeLibsSize = 0L
        var otherSize = 0L
        var fileCount = 0
        val abiSet = mutableSetOf<String>()

        for (entry in module.entries) {
            fileCount++
            val path = entry.path.toString()
            // ZIP entry path = "moduleName/entryPath" (e.g. "base/dex/classes.dex")
            val zipPath = "$moduleName/$path"
            val size = compressedSizeMap[zipPath] ?: 0L
            when {
                path.startsWith("dex/") || path.endsWith(".dex") -> dexSize += size
                path.startsWith("res/") || path == "resources.pb" -> resourcesSize += size
                path.startsWith("assets/") -> assetsSize += size
                path.startsWith("lib/") -> {
                    nativeLibsSize += size
                    // Extract ABI from path like "lib/arm64-v8a/libfoo.so"
                    val parts = path.split("/")
                    if (parts.size >= 2) {
                        abiSet.add(parts[1])
                    }
                }
                else -> otherSize += size
            }
        }

        val deliveryType = try {
            module.deliveryType.name.lowercase().replace('_', '-')
        } catch (e: Exception) {
            "unknown"
        }

        val moduleType = try {
            when (module.moduleType) {
                BundleModule.ModuleType.FEATURE_MODULE -> "Feature"
                BundleModule.ModuleType.ASSET_MODULE -> "Asset"
                BundleModule.ModuleType.ML_MODULE -> "ML"
                BundleModule.ModuleType.SDK_DEPENDENCY_MODULE -> "SDK Dependency"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }

        val isBase = module.isBaseModule
        val (deliveryLabel, deliveryDescription) = resolveDeliveryInfo(deliveryType, isBase, moduleType)

        val totalSize = dexSize + resourcesSize + assetsSize + nativeLibsSize + otherSize
        val sizePercentage = if (totalAabSize > 0) {
            totalSize.toDouble() / totalAabSize * 100
        } else 0.0
        val roundedPercentage = ApkSizeHelpers.roundOffDecimal(sizePercentage) ?: sizePercentage

        return BundleModuleInfo(
            moduleName = moduleName,
            deliveryType = deliveryType,
            deliveryLabel = deliveryLabel,
            deliveryDescription = deliveryDescription,
            moduleType = moduleType,
            fileCount = fileCount,
            nativeAbis = abiSet.sorted(),
            sizePercentage = roundedPercentage,
            totalSize = totalSize,
            dexSize = dexSize,
            resourcesSize = resourcesSize,
            assetsSize = assetsSize,
            nativeLibsSize = nativeLibsSize,
            otherSize = otherSize
        )
    }

    /**
     * Maps raw delivery type to a human-readable label and description.
     */
    private fun resolveDeliveryInfo(
        deliveryType: String,
        isBase: Boolean,
        moduleType: String
    ): Pair<String, String> {
        if (isBase) {
            return "Base Module" to "Always installed with the app. Contains the core application code and resources."
        }
        return when (deliveryType) {
            "always-initial-install" ->
                "Install-time $moduleType" to "Installed together with the base module at download time."
            "conditional-initial-install" ->
                "Conditional $moduleType" to "Installed at download time only when device meets specific conditions (e.g., SDK version, device features)."
            "no-initial-install" ->
                "Dynamic $moduleType" to "Not included in the initial install. Downloaded on-demand when the user needs it."
            else ->
                moduleType to "Delivery type: $deliveryType"
        }
    }
}
