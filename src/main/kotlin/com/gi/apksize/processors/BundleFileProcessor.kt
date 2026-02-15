package com.gi.apksize.processors

import com.gi.apksize.models.*
import com.gi.apksize.utils.Constants
import com.gi.apksize.utils.Printer

/**
 * Per-file analysis for AAB files. Equivalent of [ApkGeneralFileProcessor] but reads
 * from BundleModule entries instead of ApkSizeCalculator.
 *
 * Iterates all entries across all modules, classifies them by type,
 * and populates top files, top images, group sizes.
 */
class BundleFileProcessor(
    private val bundleHolder: BundleHolder,
    private val lobContext: LobContext? = null,
) : SimpleProcessor() {

    override val name: String = "Bundle Top Files"

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val appBundle = bundleHolder.appBundle
        val analyzerOptions = dataHolder.analyzerOptions

        val fileTypesData = HashMap<String, ArrayList<ApkFileData>>()
        val fileTypeSizes = hashMapOf<String, ApkGroupSizes>()
        val topFilesList = arrayListOf<ApkFileData>()
        val topFilesFilteredList = arrayListOf<ApkFileData>()
        val topImagesList = arrayListOf<ApkFileData>()
        val compressedSizeMap = mutableMapOf<String, Long>()
        try {
            for (zipEntry in bundleHolder.zipFile.entries()) {
                compressedSizeMap[zipEntry.name] = zipEntry.compressedSize.coerceAtLeast(0L)
            }
        } catch (e: Exception) {
            Printer.log("Could not read AAB ZIP for compressed sizes: ${e.message}")
        }

        for ((moduleName, module) in appBundle.modules) {
            for (entry in module.entries) {
                val entryPath = entry.path.toString()
                // Prefix with module name for display (e.g., "base/dex/classes.dex")
                val displayName = "${moduleName.name}/$entryPath"
                val sizeInBytes = compressedSizeMap[displayName] ?: 0L
                val sizeInKb = sizeInBytes / Constants.BYTE_TO_KB_DIVIDER

                val type = fileTypeLookup(entryPath)
                val apkFileData = ApkFileData(displayName, sizeInBytes, sizeInKb, type, type.simpleFileName!!, moduleName = moduleName.name)
                // Collect raw file data for LOB analysis (before any size filtering)
                lobContext?.collectFile(displayName, sizeInBytes, type.fileType)

                // Check for React Native bundle
                if (entryPath == "assets/index.android.bundle") {
                    apkStats.reactBundleSize = sizeInBytes
                    apkStats.reactBundleSizeInMb = com.gi.apksize.utils.ApkSizeHelpers.roundOffDecimal(
                        sizeInBytes / Constants.BYTE_TO_MB_DIVIDER
                    )
                }

                type.simpleFileName = null
                val subtype = type.fileSubType

                if (sizeInBytes >= analyzerOptions.topFilesImagesSizeLimiter) {
                    val dataInMap = fileTypesData[subtype]
                    if (dataInMap.isNullOrEmpty()) {
                        fileTypesData[subtype] = arrayListOf()
                    }
                    fileTypesData[subtype]?.add(apkFileData)
                    val imageFile = isImageFile(apkFileData)
                    topFilesList.add(apkFileData)
                    if (imageFile) {
                        topImagesList.add(apkFileData)
                    }
                    if (!imageFile && isFilteredFile(apkFileData)) {
                        topFilesFilteredList.add(apkFileData)
                    }
                }

                val currentType = fileTypeSizes[type.fileType]
                if (currentType == null) {
                    fileTypeSizes[type.fileType] = ApkGroupSizes()
                }
                val currentSubGroupSize = currentType?.subGroups?.get(subtype)?.size ?: 0L
                val updateSize = sizeInBytes + currentSubGroupSize
                val updateSizeInKb = updateSize / Constants.BYTE_TO_KB_DIVIDER
                fileTypeSizes[type.fileType]?.subGroups?.put(subtype, SizeModel(updateSize, updateSizeInKb))
            }
        }

        fileTypeSizes.forEach { it.value.calculateGroupSize() }
        val dexBytes = fileTypeSizes["dex"]?.groupSize ?: 0L
        val resourcesBytes =
            (fileTypeSizes["resources"]?.groupSize ?: 0L) + (fileTypeSizes["compileRes"]?.groupSize ?: 0L)
        val assetsBytes = fileTypeSizes["assets"]?.groupSize ?: 0L
        val nativeLibsBytes = fileTypeSizes["staticLibs"]?.groupSize ?: 0L
        val totalBytes = fileTypeSizes.values.sumOf { it.groupSize ?: 0L }
        val otherBytes = (totalBytes - dexBytes - resourcesBytes - assetsBytes - nativeLibsBytes).coerceAtLeast(0L)
        apkStats.artifactSizeBreakdown = ArtifactSizeBreakdown(
            dex = dexBytes,
            resources = resourcesBytes,
            assets = assetsBytes,
            nativeLibs = nativeLibsBytes,
            other = otherBytes,
            total = totalBytes,
        )

        val filteredSizes = hashMapOf<String, ApkGroupSizes>()
        fileTypeSizes.forEach {
            if ((it.value.groupSize ?: 0L) >= analyzerOptions.filteredFilesSizeLimiter) {
                filteredSizes[it.key] = it.value
            }
        }

        val sublistMaxCount = analyzerOptions.filesListMaxCount
        apkStats.topFiles = topFilesList.sortedByDescending { it.sizeInBytes }.take(sublistMaxCount)
        apkStats.topFilteredFiles = topFilesFilteredList.sortedByDescending { it.sizeInBytes }.take(sublistMaxCount)
        apkStats.topImages = topImagesList.sortedByDescending { it.sizeInBytes }.take(sublistMaxCount)
        apkStats.fileStats = fileTypesData
        apkStats.groupSizes = filteredSizes

        Printer.log("Analyzed ${topFilesList.size} files across all modules")
    }

    /**
     * Looks at the entry path (module-relative, e.g. "res/drawable/icon.png")
     * and returns its type & subtype.
     */
    private fun fileTypeLookup(name: String): ApkFileType {
        val splitName = name.split("/")
        val singleName = splitName[0]
        val fileType = fileNameTypeExtractor(singleName)
        if (splitName.size == 1) {
            return ApkFileType(fileType, singleName, fileType)
        }
        if (splitName.size == 2) {
            val localSubType = if (isImageFile(splitName[1])) {
                "images"
            } else {
                fileType
            }
            return ApkFileType(fileType, splitName[1], localSubType)
        }
        if (splitName.size == 3) {
            val subtype = splitName[1]
            val actualName = splitName[2]
            return ApkFileType(fileType, actualName, subtype)
        }
        val simpleFileName = splitName.last()
        return ApkFileType(fileType, simpleFileName, fileType)
    }

    /**
     * Classifies the first path segment into types.
     * Handles both APK-style and AAB-style paths (dex/, manifest/ directories).
     */
    private fun fileNameTypeExtractor(namePath: String): String {
        return when {
            namePath.endsWith(".arsc") -> "compileRes"
            namePath.startsWith("res") -> "resources"
            namePath.startsWith("raw") -> "resources"
            namePath.startsWith("META-INF") -> "Meta"
            namePath.startsWith("kotlin") -> "kotlin"
            namePath.endsWith(".dex") -> "dex"
            namePath.startsWith("dex") -> "dex"                 // AAB: dex/ directory
            namePath.startsWith("AndroidManifest") -> "manifest"
            namePath.startsWith("manifest") -> "manifest"       // AAB: manifest/ directory
            namePath.startsWith("lib") -> "staticLibs"
            namePath.startsWith("assets") -> "assets"
            namePath.endsWith(".properties") -> "properties"
            namePath.endsWith(".pb") -> "protobuf"              // AAB: .pb config files
            namePath.startsWith("root") -> "root"               // AAB: root/ directory
            else -> "others"
        }
    }

    private fun isImageFile(apkFileData: ApkFileData): Boolean {
        val isDrawable = apkFileData.fileType.fileSubType.contains("drawable")
        return isDrawable || isImageFile(apkFileData.simpleFileName)
    }

    private fun isImageFile(fileName: String): Boolean {
        if (!fileName.contains(".")) return false
        val extension = fileName.substringAfterLast(".")
        return extension == "png" || extension == "jpg" || extension == "jpeg" || extension == "webp"
    }

    private fun isFilteredFile(apkFileData: ApkFileData): Boolean {
        if (isImageFile(apkFileData)) return false
        val fileName = apkFileData.name
        if (fileName.contains("/lib/")) return false
        if (fileName.endsWith(".dex")) return false
        if (fileName.endsWith(".arsc")) return false
        if (fileName.contains("META-INF")) return false
        if (fileName.endsWith(".pb")) return false
        return true
    }
}
