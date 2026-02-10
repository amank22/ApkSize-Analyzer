package com.gi.apksize.processors

import com.android.tools.build.bundletool.model.utils.ResourcesUtils
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.BundleHolder
import com.gi.apksize.models.DataHolder
import com.gi.apksize.utils.Printer

/**
 * Resource analysis for AAB files. Replaces [AaptProcessor] for bundles.
 *
 * Directly parses resources.pb from each module via bundletool's
 * [com.android.tools.build.bundletool.model.BundleModule.getResourceTable]
 * and [ResourcesUtils.entries] to count resources by type.
 */
class BundleResourceProcessor(private val bundleHolder: BundleHolder) : SimpleProcessor() {

    override val name: String = "Bundle Resources"

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val appBundle = bundleHolder.appBundle
        val resourceCounts = hashMapOf<String, Long>()

        for ((moduleName, module) in appBundle.modules) {
            val resourceTable = module.resourceTable
            if (!resourceTable.isPresent) {
                Printer.log("Module '${moduleName.name}' has no resource table")
                continue
            }

            try {
                val entries = ResourcesUtils.entries(resourceTable.get())
                entries.forEach { entry ->
                    val typeName = entry.type.name ?: "unknown"
                    resourceCounts[typeName] = (resourceCounts[typeName] ?: 0L) + 1L
                }
                Printer.log("Module '${moduleName.name}': processed resource table")
            } catch (e: Exception) {
                Printer.log("Failed to process resources for module '${moduleName.name}': ${e.message}")
            }
        }

        if (resourceCounts.isNotEmpty()) {
            apkStats.resourcesMap = resourceCounts
            Printer.log("Resource counts: $resourceCounts")
        } else {
            Printer.log("No resources found in any module")
        }
    }

    override fun postMsg(): String {
        return "Bundle resources processing done"
    }
}
