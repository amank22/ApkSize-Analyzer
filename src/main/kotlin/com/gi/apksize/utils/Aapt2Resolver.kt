package com.gi.apksize.utils

import com.gi.apksize.models.AnalyzerOptions
import java.io.File

/**
 * Shared aapt2 executable resolution.
 * Priority: 1) config [AnalyzerOptions.aapt2Executor], 2) ANDROID_HOME / ANDROID_SDK_ROOT
 * env vars, 3) common SDK installation paths on macOS, Linux, and Windows.
 */
object Aapt2Resolver {

    /**
     * @return absolute path to a usable aapt2 executable, or null if not found.
     */
    fun resolve(options: AnalyzerOptions): String? {
        if (options.aapt2Executor.isNotBlank()) {
            val f = File(options.aapt2Executor)
            if (f.exists() && f.canExecute()) return f.absolutePath
            Printer.log("Configured aapt2Executor '${options.aapt2Executor}' not found or not executable")
        }

        val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (sdkRoot != null) {
            val aapt2 = findInSdk(File(sdkRoot))
            if (aapt2 != null) return aapt2
        }

        val commonPaths = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk",
            "${System.getProperty("user.home")}/Android/Sdk",
            "C:\\Users\\${System.getProperty("user.name")}\\AppData\\Local\\Android\\Sdk"
        )
        for (path in commonPaths) {
            val sdkDir = File(path)
            if (sdkDir.exists()) {
                val aapt2 = findInSdk(sdkDir)
                if (aapt2 != null) return aapt2
            }
        }
        return null
    }

    private fun findInSdk(sdkDir: File): String? {
        val buildToolsDir = File(sdkDir, "build-tools")
        if (!buildToolsDir.exists()) return null
        return buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedDescending()
            ?.firstNotNullOfOrNull { versionDir ->
                val aapt2 = File(versionDir, "aapt2")
                if (aapt2.exists() && aapt2.canExecute()) aapt2.absolutePath else null
            }
    }
}
