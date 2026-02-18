package com.gi.apksize.tasks

import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder
import com.gi.apksize.models.DexProcessorHolder
import com.gi.apksize.models.LobContext
import com.gi.apksize.processors.*
import com.gi.apksize.utils.Printer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


object SingleStatsTask : Task {

    /**
     * Gives stats for apk like file size, file size of resources, dex & others.
     */
    @OptIn(ExperimentalStdlibApi::class)
    override fun process(dataHolder: DataHolder): ApkStats {
        val apkStats = ApkStats()
        val analyzerOptions = dataHolder.analyzerOptions

        // Load LOB mappings if configured
        val lobContext = if (analyzerOptions.moduleMappingsPath.isNotBlank()) {
            kotlin.runCatching {
                val path = analyzerOptions.getPath(analyzerOptions.moduleMappingsPath)
                LobContext.load(
                    path = path,
                    isAab = false,
                    appPackagePrefixes = analyzerOptions.appPackagePrefix,
                    apkPath = dataHolder.primaryFile.file.absolutePath,
                )
            }.onFailure {
                Printer.log("Failed to load LOB mappings: ${it.message}")
            }.getOrNull()
        } else null

        val numberOfProcessors = Runtime.getRuntime().availableProcessors()
        val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)
        val processors: List<List<Processor<*>>> = buildList {
            add(listOf(BasicSizeProcessor(false), ApkGeneralFileProcessor(lobContext)))
            add(listOf(AaptProcessor()))
            add(listOf(DexFileProcessor(DexProcessorHolder(isCompareFile = false, needAppPackages = true), lobContext)))
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
        pool.shutdown() // Without this termination gets stuck
        try {
            pool.awaitTermination(analyzerOptions.executionTimeOut, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Printer.log(
                "Execution timed-out. Try adjusting executionTimeOut value in config.json. " +
                        "Current is ${analyzerOptions.executionTimeOut} minutes"
            )
        }

        // Finalize LOB analysis after all processors complete
        if (lobContext != null) {
            apkStats.lobAnalysis = lobContext.buildResult()
            apkStats.unmatchedDetails = lobContext.unmatchedDetails
            apkStats.attributedDetails = lobContext.attributedDetails
            apkStats.dexOverheadDetails = lobContext.dexOverheadDetails
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