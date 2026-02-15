package com.apkanalyzer.modulesize

import java.io.File
import java.util.zip.ZipFile

/**
 * Extracts native library (.so) information from AARs and local Android modules.
 */
object NativeLibExtractor {

    /**
     * Scan AAR zip for `jni/<abi>*.so` entries.
     */
    fun extractNativeLibsFromAAR(zip: ZipFile): List<NativeLibInfo> {
        val libs = mutableListOf<NativeLibInfo>()
        zip.entries().asSequence().forEach { entry ->
            val name = entry.name
            if (name.startsWith("jni/") && name.endsWith(".so") && !entry.isDirectory) {
                val parts = name.split('/')
                if (parts.size >= 3) {
                    libs.add(NativeLibInfo(abi = parts[1], name = parts.last(), sizeBytes = entry.size))
                }
            }
        }
        return libs
    }

    /**
     * Scan local module intermediates for native libs.
     */
    fun extractNativeLibsFromLocalModule(projectDir: File, variant: String): List<NativeLibInfo> {
        val libs = mutableListOf<NativeLibInfo>()
        val variantParts = VariantUtils.splitVariant(variant)

        val possiblePaths = mutableListOf(
            File(projectDir, "build/intermediates/library_jni/$variant/jni"),
            File(projectDir, "build/intermediates/merged_jni_libs/$variant/out"),
            File(projectDir, "src/main/jniLibs"),
        )
        variantParts.forEach { v ->
            possiblePaths.add(File(projectDir, "build/intermediates/library_jni/$v/jni"))
            possiblePaths.add(File(projectDir, "build/intermediates/merged_jni_libs/$v/out"))
        }

        val jniDir = possiblePaths.firstOrNull { it.exists() && it.isDirectory }
        jniDir?.listFiles()?.filter { it.isDirectory }?.forEach { abiDir ->
            abiDir.listFiles()?.filter { it.isFile && it.name.endsWith(".so") }?.forEach { soFile ->
                libs.add(NativeLibInfo(abi = abiDir.name, name = soFile.name, sizeBytes = soFile.length()))
            }
        }

        // Also check AAR output
        if (libs.isEmpty()) {
            val aarDir = File(projectDir, "build/outputs/aar")
            if (aarDir.exists()) {
                val aarFile = aarDir.listFiles()?.firstOrNull { it.name.endsWith(".aar") }
                if (aarFile != null) {
                    try {
                        ZipFile(aarFile).use { zip -> libs.addAll(extractNativeLibsFromAAR(zip)) }
                    } catch (e: Exception) { /* ignore */ }
                }
            }
        }

        return libs
    }
}
