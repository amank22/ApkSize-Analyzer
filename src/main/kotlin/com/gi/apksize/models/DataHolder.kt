package com.gi.apksize.models

import java.io.File

data class DataHolder constructor(
    val analyzerOptions: AnalyzerOptions,
    val primaryFile: FileHolder,
    val outputDir: File,
    val secondaryFile: FileHolder? = null
) {

    private constructor(builder: Builder) : this(builder.analyzerOptions,
        FileHolder(builder.primaryFile, builder.primaryProguard), builder.outputDir,
        builder.secondaryFile?.let { secFile ->
            FileHolder(secFile, builder.secondaryProguard)
        }
    )

    companion object {
        inline fun build(
            analyzerOptions: AnalyzerOptions,
            primaryFile: File, outputDir: File, block: Builder.() -> Unit
        ): DataHolder {
            return Builder(analyzerOptions, primaryFile, outputDir).apply(block).build()
        }
    }

    class Builder(
        val analyzerOptions: AnalyzerOptions,
        val primaryFile: File,
        val outputDir: File
    ) {
        var primaryProguard: File? = null
        var secondaryFile: File? = null
        var secondaryProguard: File? = null

        fun build() = DataHolder(this)
    }

}

data class FileHolder(val file: File, val proguardFile: File?)
