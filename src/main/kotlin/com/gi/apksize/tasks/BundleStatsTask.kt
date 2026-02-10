package com.gi.apksize.tasks

import com.android.tools.build.bundletool.model.AppBundle
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.BundleHolder
import com.gi.apksize.models.DataHolder
import com.gi.apksize.models.InputFileType
import com.gi.apksize.processors.*
import com.gi.apksize.utils.Printer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile

/**
 * Orchestrates AAB analysis by running bundle-specific processors in parallel.
 * Parses the AAB once via [AppBundle.buildFromZip] and shares the result
 * across all processors via [BundleHolder].
 */
object BundleStatsTask : Task {

    @OptIn(ExperimentalStdlibApi::class)
    override fun process(dataHolder: DataHolder): ApkStats {
        val apkStats = ApkStats()
        apkStats.inputFileType = InputFileType.AAB
        val analyzerOptions = dataHolder.analyzerOptions

        val aabFile = dataHolder.primaryFile.file
        Printer.log("Opening AAB file: ${aabFile.absolutePath}")

        val zipFile = ZipFile(aabFile)
        val appBundle = AppBundle.buildFromZip(zipFile)
        val bundleHolder = BundleHolder(appBundle, zipFile)

        Printer.log("AAB parsed: ${appBundle.modules.size} modules found")

        try {
            val numberOfProcessors = Runtime.getRuntime().availableProcessors()
            val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)

            val processors: List<List<Processor<*>>> = buildList {
                // Group 1: Size + File analysis (can run in parallel with others)
                add(listOf(BundleSizeProcessor(bundleHolder), BundleFileProcessor(bundleHolder)))
                // Group 2: Resource analysis
                add(listOf(BundleResourceProcessor(bundleHolder)))
                // Group 3: DEX analysis
                add(listOf(BundleDexProcessor(bundleHolder)))
                // Group 4: Metadata extraction
                add(listOf(BundleMetadataProcessor(bundleHolder)))
                // Group 5: Estimated device download sizes (standalone, no BundleHolder needed)
                add(listOf(BundleEstimatedSizeProcessor()))
            }

            processors.forEach { listOfProcess ->
                Printer.log("listOfProcess = " + listOfProcess.size)
                pool.submit {
                    listOfProcess.forEach { process ->
                        Printer.log(process.preMsg())
                        kotlin.runCatching {
                            runProcess(process, dataHolder, apkStats)
                        }.onFailure {
                            it.printStackTrace()
                            Printer.log(process.failedMsg(it))
                        }.onSuccess {
                            Printer.log(process.postMsg())
                        }
                        Printer.log("process = ${process.name} done")
                    }
                }
            }

            pool.shutdown()
            try {
                pool.awaitTermination(analyzerOptions.executionTimeOut, TimeUnit.MINUTES)
            } catch (e: InterruptedException) {
                Printer.log(
                    "Execution timed-out. Try adjusting executionTimeOut value in config.json. " +
                            "Current is ${analyzerOptions.executionTimeOut} minutes"
                )
            }
        } finally {
            bundleHolder.close()
        }

        return apkStats
    }

    private fun runProcess(process: Processor<*>, dataHolder: DataHolder, apkStats: ApkStats) {
        when (process) {
            is SimpleProcessor -> {
                process.process(dataHolder, apkStats)
            }
            else -> throw Exception("Process ${process.name} is not configured to run")
        }
    }
}
