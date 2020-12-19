package com.gi.apksize.processors

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DexPackageDiffModel
import com.gi.apksize.models.DexPackageModel
import com.gi.apksize.utils.ApkSizeHelpers
import java.io.File

object DiffProcessor {

    fun calculate(
        releaseApkFile: File, proguardMappingFile: File?,
        compareApkFile: File, compareProguardMappingFile: File?,
        analyzerOptions: AnalyzerOptions
    ): ApkStats {

        val apkStats = ApkStats()

//        val numberOfProcessors = Runtime.getRuntime().availableProcessors()
//        val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)
        val apkPath = releaseApkFile.toPath()
        val compareApkPath = compareApkFile.toPath()
//        pool.submit {
//
//        }
//
////        pool.shutdown()
//        try {
//            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
//        } catch (e: InterruptedException) {
//            println(e.message)
//        }

        apkStats.fileDiffs = FileToFileDiffProcessor.diff(
            apkPath,
            compareApkPath, analyzerOptions
        )

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
        val l1 = calculateDexPackagesSizeDiff(dexPackages, packagesSet, compareDexPackages, analyzerOptions)
        val l2 = calculateDexPackagesSizeDiff(compareDexPackages, packagesSet, dexPackages, analyzerOptions)
        val l3 = (l1 + l2).sortedByDescending { it.packageSizeDiff }
        apkStats.dexPackagesDiffs = l3
        apkStats.dexPackages = null
        apkStats.comparedDexPackages = null

        val apkSizeCalculator = ApkSizeCalculator.getDefault()

        ApkSizeHelpers.calculateBasicSizes(apkStats, apkSizeCalculator, apkPath)
        ApkSizeHelpers.calculateBasicSizes(apkStats, apkSizeCalculator, compareApkPath, true)

        apkStats.diffApkSize = (apkStats.compareApkSize?:0) - (apkStats.apkSize?:0)
        apkStats.diffApkSizeInMb =
            ApkSizeHelpers.roundOffDecimal(
                (apkStats.compareApkSizeInMb?:0.0) - (apkStats.apkSizeInMb?:0.0))
        apkStats.diffDownloadSize = (apkStats.compareDownloadSize?:0) - (apkStats.downloadSize?:0)
        apkStats.diffDownloadSizeInMb =
            ApkSizeHelpers.roundOffDecimal(
                (apkStats.compareDownloadSizeInMb?:0.0) - (apkStats.downloadSizeInMb?:0.0))

        return apkStats
    }

    private fun calculateDexPackagesSizeDiff(
        dexPackages: List<DexPackageModel>,
        packagesSet: HashSet<String>,
        compareDexPackages: List<DexPackageModel>,
        analyzerOptions: AnalyzerOptions
    ): MutableList<DexPackageDiffModel> {
        val list = mutableListOf<DexPackageDiffModel>()
        dexPackages.forEach { baseDex ->
            val pck = baseDex.basePackage
            if (packagesSet.contains(pck)) return@forEach
            val compareDex = compareDexPackages.find { it.basePackage == pck }
            if (compareDex != null) {
                packagesSet.add(pck)
                val diff = compareDex.basePackageSize - baseDex.basePackageSize
                if (diff > analyzerOptions.diffSizeLimiter) {
                    val dexDiff = DexPackageDiffModel(
                        pck, baseDex.basePackageSize, baseDex.packageSizeKb,
                        compareDex.basePackageSize, compareDex.packageSizeKb,
                        baseDex.depth,
                        diff,
                        diff / ApkFileProcessor.BYTE_TO_KB_DIVIDER
                    )
                    list.add(dexDiff)
                }
            }
        }
        return list
    }

}