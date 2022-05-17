package com.gi.apksize.processors

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder
import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Constants
import java.nio.file.Path

class BasicSizeProcessor(private val isComparingApk : Boolean) : SimpleProcessor() {

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val apk = dataHolder.primaryFile.file.toPath()
        val apkSizeCalculator = ApkSizeCalculator.getDefault()
        calculateBasicSizes(apkStats, apkSizeCalculator, apk, isComparingApk)
    }

    override val name: String = "Basic Sizes"

    /**
     * Calculates basic file sizes like raw file size, download file size.
     */
    private fun calculateBasicSizes(
        apkStats: ApkStats,
        apkSizeCalculator: ApkSizeCalculator,
        apk: Path,
        isComparingApk: Boolean = false
    ) {
        val downloadSize = apkSizeCalculator.getFullApkDownloadSize(apk)
        if (isComparingApk) {
            apkStats.compareDownloadSize = downloadSize
            apkStats.compareDownloadSizeInMb =
                ApkSizeHelpers.roundOffDecimal(downloadSize / Constants.BYTE_TO_MB_DIVIDER)
            val fullApkRawSize = apkSizeCalculator.getFullApkRawSize(apk)
            apkStats.compareApkSize = fullApkRawSize
            apkStats.compareApkSizeInMb =
                ApkSizeHelpers.roundOffDecimal(fullApkRawSize / Constants.BYTE_TO_MB_DIVIDER)
        } else {
            apkStats.downloadSize = downloadSize
            apkStats.downloadSizeInMb = ApkSizeHelpers.roundOffDecimal(downloadSize / Constants.BYTE_TO_MB_DIVIDER)
            val fullApkRawSize = apkSizeCalculator.getFullApkRawSize(apk)
            apkStats.apkSize = fullApkRawSize
            apkStats.apkSizeInMb =
                ApkSizeHelpers.roundOffDecimal(fullApkRawSize / Constants.BYTE_TO_MB_DIVIDER)
        }
    }

}