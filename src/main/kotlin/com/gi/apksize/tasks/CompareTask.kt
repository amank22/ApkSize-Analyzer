package com.gi.apksize.tasks

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.*
import com.gi.apksize.processors.DexFileProcessor
import com.gi.apksize.processors.FileToFileDiffProcessor
import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Constants
import java.io.File
import java.nio.file.Path
import kotlin.math.absoluteValue

object CompareTask : Task {

    private fun calculate(
        releaseApkFile: File, proguardMappingFile: File?,
        compareApkFile: File, compareProguardMappingFile: File?,
        analyzerOptions: AnalyzerOptions
    ): ApkStats {

        val apkStats = ApkStats()
        val apkPath = releaseApkFile.toPath()
        val compareApkPath = compareApkFile.toPath()

        if (!analyzerOptions.disableFileByFileComparison) {
            FileToFileDiffProcessor.diff(
                apkPath, compareApkPath,
                analyzerOptions, apkStats
            )
        }

        calculateBaseSizes(apkStats, apkPath, compareApkPath)

        calculateDexPackagesDiffs(
            apkPath,
            apkStats,
            proguardMappingFile,
            analyzerOptions,
            compareApkPath,
            compareProguardMappingFile
        )

        return apkStats
    }

    private fun calculateDexPackagesDiffs(
        apkPath: Path,
        apkStats: ApkStats,
        proguardMappingFile: File?,
        analyzerOptions: AnalyzerOptions,
        compareApkPath: Path,
        compareProguardMappingFile: File?
    ) {
        DexFileProcessor.calculateDexStats(
            apkPath, apkStats, proguardMappingFile,
            analyzerOptions, isCompareFile = false, needAppPackages = false
        )
        DexFileProcessor.calculateDexStats(
            compareApkPath, apkStats,
            compareProguardMappingFile, analyzerOptions,
            isCompareFile = true,
            needAppPackages = false
        )
        val dexPackages = apkStats.dexPackages.orEmpty()
        val compareDexPackages = apkStats.comparedDexPackages.orEmpty()
        val packagesSet = hashSetOf<String>()
        val l1 = calculateDexPackagesSizeDiff(
            dexPackages, packagesSet,
            compareDexPackages, analyzerOptions
        )
        val l2 = calculateDexPackagesSizeDiff(
            compareDexPackages, packagesSet,
            dexPackages, analyzerOptions, true
        )
        val l3 = (l1 + l2).sortedByDescending { it.packageSizeDiff }
        apkStats.dexPackagesDiffs = l3
        apkStats.dexPackages = null
        apkStats.comparedDexPackages = null

        apkStats.diffApkSize = (apkStats.compareApkSize ?: 0) - (apkStats.apkSize ?: 0)
        apkStats.diffApkSizeInMb =
            ApkSizeHelpers.roundOffDecimal(
                (apkStats.compareApkSizeInMb ?: 0.0) - (apkStats.apkSizeInMb ?: 0.0)
            )
        apkStats.diffDownloadSize = (apkStats.compareDownloadSize ?: 0) - (apkStats.downloadSize ?: 0)
        apkStats.diffDownloadSizeInMb =
            ApkSizeHelpers.roundOffDecimal(
                (apkStats.compareDownloadSizeInMb ?: 0.0) - (apkStats.downloadSizeInMb ?: 0.0)
            )
    }

    private fun calculateBaseSizes(
        apkStats: ApkStats,
        apkPath: Path,
        compareApkPath: Path
    ) {
        val apkSizeCalculator = ApkSizeCalculator.getDefault()

        ApkSizeHelpers.calculateBasicSizes(apkStats, apkSizeCalculator, apkPath)
        ApkSizeHelpers.calculateBasicSizes(apkStats, apkSizeCalculator, compareApkPath, true)
    }

    private fun calculateDexPackagesSizeDiff(
        dexPackages: List<DexPackageModel>,
        packagesSet: HashSet<String>,
        compareDexPackages: List<DexPackageModel>,
        analyzerOptions: AnalyzerOptions,
        isReversed: Boolean = false
    ): MutableList<DexPackageDiffModel> {
        val list = mutableListOf<DexPackageDiffModel>()
        dexPackages.forEach { baseDex ->
            val pck = baseDex.basePackage
            if (packagesSet.contains(pck)) return@forEach
            val compareDex = compareDexPackages.find { it.basePackage == pck }
            if (compareDex != null) {
                packagesSet.add(pck)
                val diff = if (isReversed) {
                    baseDex.basePackageSize - compareDex.basePackageSize
                } else {
                    compareDex.basePackageSize - baseDex.basePackageSize
                }
                if (diff.absoluteValue > analyzerOptions.diffSizeLimiter) {
                    val dexDiff = DexPackageDiffModel(
                        pck, baseDex.basePackageSize, baseDex.packageSizeKb,
                        compareDex.basePackageSize, compareDex.packageSizeKb,
                        baseDex.depth,
                        diff,
                        diff / Constants.BYTE_TO_KB_DIVIDER
                    )
                    list.add(dexDiff)
                }
            }
        }
        return list
    }

    override fun process(dataHolder: DataHolder) : ApkStats {
        val apk = dataHolder.primaryFile.file
        val proguardFile = dataHolder.primaryFile.proguardFile
        val compareApk = dataHolder.secondaryFile?.file
        if (compareApk == null) {
            throw IllegalArgumentException("File for comparison is required")
        }
        val compareProguardFile = dataHolder.secondaryFile.proguardFile
        val analyzerOptions = dataHolder.analyzerOptions
        return calculate(apk, proguardFile, compareApk, compareProguardFile, analyzerOptions)
    }

}