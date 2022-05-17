package com.gi.apksize.processors

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.*
import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Constants
import java.nio.file.Path

class ApkGeneralFileProcessor : SimpleProcessor() {

    /**
     * Calculates file sizes of each and every file & filters them according to their type.
     * Also fills data for top images, top files, top filtered list etc
     */
    private fun calculatePerFileSize(
        apkStats: ApkStats,
        apkSizeCalculator: ApkSizeCalculator,
        apk: Path,
        analyzerOptions: AnalyzerOptions
    ) {
        val fileTypesData = HashMap<String, ArrayList<ApkFileData>>()
        val fileTypeSizes = hashMapOf<String, ApkGroupSizes>()
        val perFileSize = apkSizeCalculator.getDownloadSizePerFile(apk)
        val topFilesList = arrayListOf<ApkFileData>()
        val topFilesFilteredList = arrayListOf<ApkFileData>()
        val topImagesList = arrayListOf<ApkFileData>()
        perFileSize.forEach {
            val name = it.key.removePrefix("/")
            val sizeInBytes = it.value
            val sizeInKb = sizeInBytes / Constants.BYTE_TO_KB_DIVIDER
            val type = fileTypeLookup(name)
            val apkFileData = ApkFileData(name, sizeInBytes, sizeInKb, type, type.simpleFileName!!)
            if (name == "assets/index.android.bundle") {
                apkStats.reactBundleSize = sizeInBytes
                apkStats.reactBundleSizeInMb = ApkSizeHelpers.roundOffDecimal(
                    sizeInBytes
                            / Constants.BYTE_TO_MB_DIVIDER
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
                topFilesList.add(apkFileData)
                val imageFile = isImageFile(apkFileData)
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
        fileTypeSizes.forEach {
            it.value.calculateGroupSize()
        }
        val filteredSizes = hashMapOf<String, ApkGroupSizes>()
        fileTypeSizes.forEach {
            if (it.value.groupSize ?: 0L >= analyzerOptions.filteredFilesSizeLimiter) {
                filteredSizes[it.key] = it.value
            }
        }
        val sublistMaxCount = analyzerOptions.filesListMaxCount
        apkStats.topFiles = topFilesList.sortedByDescending { it.sizeInBytes }.takeLast(sublistMaxCount)
        apkStats.topFilteredFiles = topFilesFilteredList.sortedByDescending { it.sizeInBytes }.takeLast(sublistMaxCount)
        apkStats.topImages = topImagesList.sortedByDescending { it.sizeInBytes }.takeLast(sublistMaxCount)
        apkStats.fileStats = fileTypesData
        apkStats.groupSizes = filteredSizes
    }

    /**
     * Checks if the name is of an image file according to the extension.
     * Used some common extensions like jpg, webp, png.
     * These only account for the rasterize images and not vector.
     */
    private fun isImageFile(apkFileData: ApkFileData): Boolean {
        if (!apkFileData.simpleFileName.contains(".")) return false
        val simpleFileName = apkFileData.simpleFileName.split(".")[1]
        return simpleFileName == "png" || simpleFileName == "jpg" || simpleFileName == "jpeg" || simpleFileName == "webp"
    }

    /**
     * Checks if file is not some common files in the apk like dex, arsc, static library
     */
    private fun isFilteredFile(apkFileData: ApkFileData): Boolean {
        val fileName = apkFileData.name
        if (fileName.startsWith("lib")) return false
        if (fileName.endsWith(".dex")) return false
        if (fileName.endsWith(".arsc")) return false
        if (fileName.startsWith("META-INF")) return false
        return true
    }

    /**
     * Looks at the name of the file as "/raw/drawable/xx.png" and returns it's type & subtype
     * Type is the first part of the input which will be raw in this example & subtype is the second part or
     * type in case of no second part as in /classes.dex.
     * Example:
     * 1. "/raw/drawable/xx.png" -> type = raw, subtype = drawable
     * 2. "/classes.dex" -> type = dex, subtype = dex
     */
    private fun fileTypeLookup(name: String): ApkFileType {
        val splitName = name.split("/")
        val singleName = splitName[0]
        val fileType = fileNameTypeExtractor(singleName)
        if (splitName.size == 1) {
            return ApkFileType(fileType, singleName, fileType)
        }
        if (splitName.size == 2) {
            return ApkFileType(fileType, splitName[1], fileType)
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
     * Classifies the name in types like raw, res, dex, manifest etc or others if no known found.
     */
    private fun fileNameTypeExtractor(namePath: String): String {
        return when {
            namePath.endsWith(".arsc") -> {
                "compileRes"
            }
            namePath.startsWith("res") -> {
                "resources"
            }
            namePath.startsWith("raw") -> {
                "resources"
            }
            namePath.startsWith("META-INF") -> {
                "Meta"
            }
            namePath.startsWith("kotlin") -> {
                "kotlin"
            }
            namePath.endsWith(".dex") -> {
                "dex"
            }
            namePath.startsWith("AndroidManifest") -> {
                "manifest"
            }
            namePath.startsWith("lib") -> {
                "staticLibs"
            }
            namePath.startsWith("assets") -> {
                "assets"
            }
            namePath.endsWith(".properties") -> {
                "properties"
            }
            else -> {
                "others"
            }
        }
    }

    override val name: String = "Top Files"

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val apk = dataHolder.primaryFile.file.toPath()
        val apkSizeCalculator = ApkSizeCalculator.getDefault()
        calculatePerFileSize(apkStats, apkSizeCalculator, apk, dataHolder.analyzerOptions)
    }

}