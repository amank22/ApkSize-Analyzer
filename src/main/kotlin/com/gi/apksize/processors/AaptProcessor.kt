package com.gi.apksize.processors

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder
import com.gi.apksize.utils.Printer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder

class AaptProcessor : SimpleProcessor() {

    private var completionStatus : Int = 1 // 0 is success, 1 is failed

    private fun process(releaseApkFile: File, apkStats: ApkStats, analyzerOptions: AnalyzerOptions) {
        completionStatus = 1
        try {
            val aapt2Path = analyzerOptions.aapt2Executor
            if (aapt2Path.isBlank()) return
            val rt = Runtime.getRuntime()
            val command = "$aapt2Path dump resources ${releaseApkFile.absolutePath}"
            val pr = rt.exec(command)
            val input = BufferedReader(InputStreamReader(pr.inputStream))
            val builder = StringBuilder()
            val map = hashMapOf<String, Long>()
            input.forEachLine {
                val line = it.trim()
                try {
                    if (line.startsWith("type")) {
                        val splitted = line.split(" ")
                        val type = splitted[1]
                        val count = splitted[3].removePrefix("entryCount=").toLong()
                        map[type] = count
                    }
                } catch (e : Exception) {
                    Printer.log(e.message)
                }
                builder.appendLine(it)
            }
            val exitVal = pr.waitFor()
            completionStatus = exitVal
            if (exitVal == 0) {
                Printer.log("Resources processing done")
            } else {
                Printer.log("Resources processing failed")
            }
            val completeText = builder.toString()
            if (completeText.isNotBlank()) {
                apkStats.aaptData = completeText
            }
            if (map.isNotEmpty()) {
                apkStats.resourcesMap = map
            }
        } catch (e: Exception) {
            Printer.log(e)
        }
    }

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        process(dataHolder.primaryFile.file, apkStats, dataHolder.analyzerOptions)
    }

    override val name: String = "Resources"

    override fun postMsg(): String {
        return if (completionStatus == 0) {
            "Resources processing done"
        } else {
            "Resources processing failed"
        }
    }

}