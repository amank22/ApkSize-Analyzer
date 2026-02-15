package com.gi.apksize.utils

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.DataHolder
import com.gi.apksize.models.FileHolder
import com.gi.apksize.models.InputFileType
import java.io.File

fun AnalyzerOptions.primaryHolder(): DataHolder {
    val analyzerOptions = this
    val proguardFilePath = if (analyzerOptions.inputFileProguardPath.isBlank()) {
        null
    } else {
        analyzerOptions.getPath(analyzerOptions.inputFileProguardPath)
    }
    val releaseApkPath = analyzerOptions.getPath(analyzerOptions.inputFilePath)
    val separator = File.separator
    val outputDirectory = if (analyzerOptions.isDiffMode) {
        analyzerOptions.getPath(analyzerOptions.outputFolderPath)
            .removeSuffix(separator) + "${separator}diffs$separator"
    } else {
        analyzerOptions.getPath(analyzerOptions.outputFolderPath)
    }
    val releaseApkFile = File(releaseApkPath)
    val outputFolder = File(outputDirectory)
    val proguardMappingFile = if (proguardFilePath.isNullOrBlank()) null else File(proguardFilePath)
    if (!releaseApkFile.exists()) {
        throw Exception("Input file (APK/AAB) does not exist at $releaseApkPath. Are you sure you are using relative/absolute paths correctly?")
    }
    if (!outputFolder.exists()) {
        outputFolder.mkdirs()
    }
    return DataHolder.build(analyzerOptions, releaseApkFile, outputFolder) {
        primaryProguard = proguardMappingFile
    }
}

fun AnalyzerOptions.compareHolder(): DataHolder {
    val analyzerOptions = this
    if (analyzerOptions.inputFileType() == InputFileType.AAB) {
        throw Exception("Comparison (diff) mode is not supported for AAB files yet. Please use single analysis mode.")
    }
    val primaryHolder = analyzerOptions.primaryHolder()
    val compareProguardFilePath = if (analyzerOptions.compareFileProguardPath.isBlank()) {
        null
    } else {
        analyzerOptions.getPath(analyzerOptions.compareFileProguardPath)
    }
    val compareApkPath = analyzerOptions.getPath(analyzerOptions.compareFilePath)
    val compareApkFile = File(compareApkPath)
    val compareProguardMappingFile =
        if (compareProguardFilePath.isNullOrBlank()) null else File(compareProguardFilePath)
    if (!compareApkFile.exists()) {
        throw Exception("Compare file (APK) does not exist at $compareApkPath. Are you sure you are using relative/absolute paths correctly?")
    }
    return primaryHolder.copy(secondaryFile = FileHolder(compareApkFile, compareProguardMappingFile))
}