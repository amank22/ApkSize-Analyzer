package com.gi.apksize.tasks

import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder
import com.gi.apksize.models.DexProcessorHolder
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
        val numberOfProcessors = Runtime.getRuntime().availableProcessors()
        val pool: ExecutorService = Executors.newFixedThreadPool(numberOfProcessors)
        val processors: List<List<Processor<*>>> = buildList {
            add(listOf(BasicSizeProcessor(false), ApkGeneralFileProcessor()))
            add(listOf(AaptProcessor()))
            add(listOf(DexFileProcessor(DexProcessorHolder(isCompareFile = false, needAppPackages = true))))
        }

        processors.forEach { listOfProcess ->
            pool.submit {
                listOfProcess.forEach { process ->
                    Printer.log(process.preMsg())
                    kotlin.runCatching {
                        runProcess(process, dataHolder, apkStats)
                    }.onFailure {
                        Printer.log(process.failedMsg(it))
                    }.onSuccess {
                        Printer.log(process.postMsg())
                    }
                }
            }
        }
        try {
            pool.awaitTermination(analyzerOptions.executionTimeOut, TimeUnit.MINUTES)
        } catch (e: InterruptedException) {
            Printer.log(
                "Execution timed-out. Try adjusting executionTimeOut value in config.json. " +
                        "Current is ${analyzerOptions.executionTimeOut} minutes"
            )
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