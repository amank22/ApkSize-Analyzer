package com.gi.apksize.utils

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.ApkStats
import com.gi.apksize.processors.ApkFileProcessor
import java.math.RoundingMode
import java.nio.file.Path
import java.text.DecimalFormat

object ApkSizeHelpers {

    fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDoubleOrNull()
    }

    /**
     * Calculates basic file sizes like raw file size, download file size.
     */
    fun calculateBasicSizes(
        apkStats: ApkStats,
        apkSizeCalculator: ApkSizeCalculator,
        apk: Path,
        isComparingApk : Boolean = false
    ) {
        val downloadSize = apkSizeCalculator.getFullApkDownloadSize(apk)
        if (isComparingApk) {
            apkStats.compareDownloadSize = downloadSize
            apkStats.compareDownloadSizeInMb = roundOffDecimal(downloadSize / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
            val fullApkRawSize = apkSizeCalculator.getFullApkRawSize(apk)
            apkStats.compareApkSize = fullApkRawSize
            apkStats.compareApkSizeInMb =
                roundOffDecimal(fullApkRawSize / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
        } else {
            apkStats.downloadSize = downloadSize
            apkStats.downloadSizeInMb = roundOffDecimal(downloadSize / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
            val fullApkRawSize = apkSizeCalculator.getFullApkRawSize(apk)
            apkStats.apkSize = fullApkRawSize
            apkStats.apkSizeInMb =
                roundOffDecimal(fullApkRawSize / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
        }
    }

}