package com.gi.apksize.processors

import com.android.tools.apk.analyzer.ApkSizeCalculator
import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.utils.ApkSizeHelpers
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


object ApkFileProcessor {

    const val BYTE_TO_KB_DIVIDER = 1024
    const val BYTE_TO_MB_DIVIDER = 1048576.0

    /**
     * Gives stats for apk like file size, file size of resources, dex & others.
     */
    fun apkStudioAnalyzerTools(
        releaseApkFile: File, proguardMappingFile: File?, analyzerOptions: AnalyzerOptions
    ): ApkStats {
        val apkStats = ApkStats()
        val apk = releaseApkFile.toPath()
        val apkSizeCalculator = ApkSizeCalculator.getDefault()

        val numberOfProcessors = Runtime.getRuntime().availableProcessors()
        val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)

        pool.submit {
            ApkSizeHelpers.calculateBasicSizes(apkStats, apkSizeCalculator, apk)
            ApkGeneralFileProcessor.calculatePerFileSize(apkStats, apkSizeCalculator, apk, analyzerOptions)
        }
        pool.submit {
            try {
                DexFileProcessor.calculateDexStats(apk, apkStats, proguardMappingFile, analyzerOptions)
            } catch (e: Exception) {
                println("Error: Dex Processing: ${e.message}")
            }
        }
        pool.submit {
            AaptProcessor.process(releaseApkFile, apkStats, analyzerOptions)
        }
        pool.shutdown()
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            println(e.message)
        }

        return apkStats
    }
}