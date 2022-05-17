package com.gi.apksize

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.tasks.ApkSizeTask

object ApkSizeAnalyzer {

    fun analyze(analyzerOptions: AnalyzerOptions) {
        ApkSizeTask.evaluate(analyzerOptions)
    }

}