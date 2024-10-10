package com.gi.apksize.tasks

import com.gi.apksize.models.*
import com.gi.apksize.processors.BasicSizeProcessor
import com.gi.apksize.processors.DexFileProcessor
import com.gi.apksize.processors.FileToFileDiffProcessor
import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Constants
import kotlin.math.absoluteValue

object CompareTask : Task {

    private fun calculate(
        dataHolder: DataHolder
    ): ApkStats {

        val apkStats = ApkStats()
        FileToFileDiffProcessor.diff(dataHolder, apkStats)
        calculateBaseSizes(dataHolder, apkStats)
        calculateDexPackagesDiffs(dataHolder, apkStats)
        return apkStats
    }

    private fun calculateDexPackagesDiffs(
        dataHolder: DataHolder,
        apkStats: ApkStats
    ) {
        val dexProcessor = DexFileProcessor(DexProcessorHolder(isCompareFile = false, needAppPackages = true))
        val dexProcessorOtherFile = DexFileProcessor(DexProcessorHolder(isCompareFile = true, needAppPackages = true))
        dexProcessor.process(dataHolder = dataHolder, apkStats)
        dexProcessorOtherFile.process(dataHolder = dataHolder, apkStats)
        val dexPackages = apkStats.dexPackages.orEmpty()
        val compareDexPackages = apkStats.comparedDexPackages.orEmpty()
        val packagesSet = hashSetOf<String>()
        val l1 = calculateDexPackagesSizeDiff(
            dexPackages, packagesSet,
            compareDexPackages, dataHolder.analyzerOptions
        )
        val l2 = calculateDexPackagesSizeDiff(
            compareDexPackages, packagesSet,
            dexPackages, dataHolder.analyzerOptions, true
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
        dataHolder: DataHolder,
        apkStats: ApkStats
    ) {
        val basicSizeProcessor = BasicSizeProcessor(false)
        val basicSizeProcessorOther = BasicSizeProcessor(true)
        basicSizeProcessor.process(dataHolder, apkStats)
        basicSizeProcessorOther.process(dataHolder, apkStats)
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

    override fun process(dataHolder: DataHolder): ApkStats {
        return calculate(dataHolder)
    }

}