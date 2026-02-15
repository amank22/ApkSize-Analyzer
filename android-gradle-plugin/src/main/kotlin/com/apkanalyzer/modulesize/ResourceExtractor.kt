package com.apkanalyzer.modulesize

import org.gradle.api.Project
import java.io.File
import java.util.LinkedHashSet
import java.util.zip.ZipFile

/**
 * Extracts resource information from AARs, JARs, and local Android modules.
 */
object ResourceExtractor {

    // ── R.txt parser ─────────────────────────────────────────────────────

    /**
     * Parse an R.txt file and return deduplicated resource counts by type.
     * Format per line: `int|int[] <type> <name> <value>`
     */
    fun parseRTxt(content: String): ResourceCount {
        val resourceSet = LinkedHashSet<String>() // "type:name" for dedup
        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split("\\s+".toRegex(), limit = 4)
            if (parts.size >= 3) {
                resourceSet.add("${parts[1]}:${parts[2]}")
            }
        }
        val byType = mutableMapOf<String, Int>()
        resourceSet.forEach { key ->
            val type = key.substringBefore(':')
            byType[type] = (byType[type] ?: 0) + 1
        }
        return ResourceCount(total = resourceSet.size, byType = byType)
    }

    // ── res/ folder scanner ──────────────────────────────────────────────

    /**
     * Scan `res/` entries inside a zip to get declared resource folder types.
     * Returns set of "type:filename" strings.
     */
    fun scanResFolder(zip: ZipFile): Set<String> {
        val declaredSet = LinkedHashSet<String>()
        zip.entries().asSequence().forEach { entry ->
            val name = entry.name
            if (name.startsWith("res/") && !entry.isDirectory) {
                val pathParts = name.split('/')
                if (pathParts.size >= 3) {
                    val resType = pathParts[1].substringBefore('-')
                    val fileName = pathParts.last()
                    val baseName = if ('.' in fileName) fileName.substringBeforeLast('.') else fileName
                    declaredSet.add("$resType:$baseName")
                }
            }
        }
        return declaredSet
    }

    // ── File-path scanners (for resource-mapping) ────────────────────────

    /**
     * Scan file entries inside a zip (AAR/JAR) that are meaningful for size mapping.
     * Skips: dex, build metadata, jars, META-INF, data-binding, merged values XMLs.
     */
    fun scanFilePathsFromZip(zip: ZipFile): List<String> {
        val paths = mutableListOf<String>()
        val skipFiles = setOf(
            "R.txt", "proguard.txt", "AndroidManifest.xml",
            "public.txt", "annotations.zip", "lint.jar"
        )
        val skipPrefixes = listOf("META-INF/", "data-binding/", "data-binding-base-class-log/")

        zip.entries().asSequence().forEach { entry ->
            if (entry.isDirectory) return@forEach
            val name = entry.name
            if (name.endsWith(".dex") || name.endsWith(".jar")) return@forEach
            if (skipPrefixes.any { name.startsWith(it) }) return@forEach
            val fileName = if ('/' in name) name.substringAfterLast('/') else name
            if (fileName in skipFiles) return@forEach
            if (name.matches(Regex("res/values[^/]*/values\\.xml"))) return@forEach

            // Normalize jni/ → lib/ to match APK format
            if (name.startsWith("jni/")) {
                paths.add("lib/" + name.substring(4))
            } else {
                paths.add(name)
            }
        }
        return paths
    }

    /**
     * Scan file paths from a local module's source directories.
     * When a Gradle [Project] reference is provided, reads `android.sourceSets.main.res.srcDirs`
     * (and assets.srcDirs) so that custom/nested resource directories are handled correctly.
     * Falls back to scanning src/main/res, src/main/assets, src/main/jniLibs when no project.
     *
     * Returns list of relative paths like "res/drawable-xhdpi/splash.webp",
     * "assets/fonts/roboto.ttf", "lib/arm64-v8a/libfoo.so".
     */
    fun scanFilePathsFromLocalModule(moduleDir: File, project: Project? = null): List<String> {
        val paths = mutableListOf<String>()

        // --- Resources ---
        val resDirs = mutableListOf<File>()
        if (project != null) {
            try {
                val androidExt = project.extensions.findByName("android")
                if (androidExt != null) {
                    val sourceSets = androidExt.javaClass.getMethod("getSourceSets").invoke(androidExt)
                    val findByName = sourceSets.javaClass.getMethod("findByName", String::class.java)
                    val mainSourceSet = findByName.invoke(sourceSets, "main")
                    if (mainSourceSet != null) {
                        val res = mainSourceSet.javaClass.getMethod("getRes").invoke(mainSourceSet)
                        @Suppress("UNCHECKED_CAST")
                        val srcDirs = res.javaClass.getMethod("getSrcDirs").invoke(res) as Set<File>
                        srcDirs.forEach { dir ->
                            if (dir.exists() && dir.isDirectory) resDirs.add(dir)
                        }
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        if (resDirs.isEmpty()) {
            val defaultDir = File(moduleDir, "src/main/res")
            if (defaultDir.exists() && defaultDir.isDirectory) resDirs.add(defaultDir)
        }

        // Canonical paths of all declared source dirs — skip nested dirs that are themselves roots
        val resDirCanonicalPaths = resDirs.map { it.canonicalPath }.toSet()

        resDirs.forEach { resDir ->
            resDir.listFiles()?.filter { it.isDirectory }?.forEach { typeDir ->
                if (typeDir.canonicalPath in resDirCanonicalPaths) return@forEach
                typeDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    paths.add("res/${typeDir.name}/${file.name}")
                }
            }
        }

        // --- Assets ---
        val assetsDirs = mutableListOf<File>()
        if (project != null) {
            try {
                val androidExt = project.extensions.findByName("android")
                if (androidExt != null) {
                    val sourceSets = androidExt.javaClass.getMethod("getSourceSets").invoke(androidExt)
                    val findByName = sourceSets.javaClass.getMethod("findByName", String::class.java)
                    val mainSourceSet = findByName.invoke(sourceSets, "main")
                    if (mainSourceSet != null) {
                        val assets = mainSourceSet.javaClass.getMethod("getAssets").invoke(mainSourceSet)
                        @Suppress("UNCHECKED_CAST")
                        val srcDirs = assets.javaClass.getMethod("getSrcDirs").invoke(assets) as Set<File>
                        srcDirs.forEach { dir ->
                            if (dir.exists() && dir.isDirectory) assetsDirs.add(dir)
                        }
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
        if (assetsDirs.isEmpty()) {
            val defaultDir = File(moduleDir, "src/main/assets")
            if (defaultDir.exists() && defaultDir.isDirectory) assetsDirs.add(defaultDir)
        }

        assetsDirs.forEach { assetsDir ->
            assetsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = file.absolutePath.substring(assetsDir.absolutePath.length + 1)
                paths.add("assets/$relPath")
            }
        }

        // --- JNI libs ---
        val jniDir = File(moduleDir, "src/main/jniLibs")
        if (jniDir.exists() && jniDir.isDirectory) {
            jniDir.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = file.absolutePath.substring(jniDir.absolutePath.length + 1)
                paths.add("lib/$relPath") // normalize to lib/ to match APK format
            }
        }

        return paths
    }

    // ── Resource extraction from AAR zip ─────────────────────────────────

    /**
     * Extract resource info from an AAR [zip]: R.txt + res/ folder scan.
     */
    fun extractResources(zip: ZipFile): ResourceResult {
        val warnings = mutableListOf<String>()

        val rTxtEntry = zip.getEntry("R.txt")
        val rTxtContent = rTxtEntry?.let { zip.getInputStream(it).bufferedReader().readText() }

        val declaredResSet = scanResFolder(zip)
        val declaredByType = mutableMapOf<String, Int>()
        declaredResSet.forEach { key ->
            val type = key.substringBefore(':')
            declaredByType[type] = (declaredByType[type] ?: 0) + 1
        }

        return if (rTxtContent != null && rTxtContent.isNotBlank()) {
            val parsed = parseRTxt(rTxtContent)
            ResourceResult(
                declared = ResourceCount(declaredResSet.size, declaredByType),
                totalRTxtEntries = parsed.total,
                transitive = maxOf(0, parsed.total - declaredResSet.size),
                warnings = warnings,
            )
        } else {
            if (declaredResSet.isEmpty()) {
                warnings.add("No R.txt and no res/ folder found")
            } else {
                warnings.add("No R.txt found; resource counts from res/ folder scan only")
            }
            ResourceResult(
                declared = ResourceCount(declaredResSet.size, declaredByType),
                totalRTxtEntries = declaredResSet.size,
                transitive = 0,
                warnings = warnings,
            )
        }
    }

    // ── Resource extraction from local module intermediates ──────────────

    /**
     * Extract resources from local module build intermediates.
     */
    fun extractResourcesFromLocalModule(projectDir: File, variant: String): ResourceResult {
        val warnings = mutableListOf<String>()
        val variantParts = VariantUtils.splitVariant(variant)

        // Try to find R.txt in intermediates
        val possibleRTxtPaths = mutableListOf(
            File(projectDir, "build/intermediates/runtime_symbol_list/$variant/R.txt"),
            File(projectDir, "build/intermediates/runtime_symbol_list/$variant/out/R.txt"),
        )
        variantParts.forEach { v ->
            possibleRTxtPaths.add(File(projectDir, "build/intermediates/runtime_symbol_list/$v/R.txt"))
            possibleRTxtPaths.add(File(projectDir, "build/intermediates/runtime_symbol_list/$v/out/R.txt"))
        }
        val rTxtFile = possibleRTxtPaths.firstOrNull { it.exists() }

        // Try AAR output for res/ scanning
        val aarDir = File(projectDir, "build/outputs/aar")
        val aarFile = aarDir.takeIf { it.exists() }?.listFiles()?.firstOrNull { it.name.endsWith(".aar") }

        // Count declared resources from res/ source sets
        val declaredResSet = LinkedHashSet<String>()
        val resDir = File(projectDir, "src/main/res")
        if (resDir.exists()) {
            resDir.listFiles()?.filter { it.isDirectory }?.forEach { typeDir ->
                val resType = typeDir.name.substringBefore('-')
                typeDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    val baseName = if ('.' in file.name) file.name.substringBeforeLast('.') else file.name
                    declaredResSet.add("$resType:$baseName")
                }
            }
        } else if (aarFile != null) {
            try {
                ZipFile(aarFile).use { zip -> declaredResSet.addAll(scanResFolder(zip)) }
            } catch (e: Exception) {
                warnings.add("Failed to scan AAR res/: ${e.message}")
            }
        }

        val declaredByType = mutableMapOf<String, Int>()
        declaredResSet.forEach { key ->
            val type = key.substringBefore(':')
            declaredByType[type] = (declaredByType[type] ?: 0) + 1
        }

        return if (rTxtFile != null) {
            val parsed = parseRTxt(rTxtFile.readText())
            ResourceResult(
                declared = ResourceCount(declaredResSet.size, declaredByType),
                totalRTxtEntries = parsed.total,
                transitive = maxOf(0, parsed.total - declaredResSet.size),
                warnings = warnings,
            )
        } else {
            if (declaredResSet.isEmpty()) {
                warnings.add("No R.txt and no res/ folder found (has the module been built?)")
            } else {
                warnings.add("No R.txt found; resource counts from source res/ folder only")
            }
            ResourceResult(
                declared = ResourceCount(declaredResSet.size, declaredByType),
                totalRTxtEntries = declaredResSet.size,
                transitive = 0,
                warnings = warnings,
            )
        }
    }
}
