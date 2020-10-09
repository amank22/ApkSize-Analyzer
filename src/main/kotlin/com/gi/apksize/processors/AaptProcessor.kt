package com.gi.apksize.processors

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.StringBuilder

object AaptProcessor {

    fun process(releaseApkFile: File, apkStats: ApkStats, analyzerOptions: AnalyzerOptions) {
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
                    println(e.message)
                }
                builder.appendLine(it)
            }
            val exitVal = pr.waitFor()
            println("Aapt Exited with error code $exitVal")
            val completeText = builder.toString()
            if (!completeText.isBlank()) {
                apkStats.aaptData = completeText
            }
            if (map.isNotEmpty()) {
                apkStats.resourcesMap = map
            }
        } catch (e: java.lang.Exception) {
            println(e.toString())
            e.printStackTrace()
        }
    }

}