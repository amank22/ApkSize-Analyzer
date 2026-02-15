package com.apkanalyzer.modulesize

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/**
 * Extracts class/package information from JARs, AARs, class directories,
 * and local Android module build intermediates.
 */
object ClassExtractor {

    // ── Stream-based class scanner ───────────────────────────────────────

    /**
     * Scan a JAR stream for .class entries.
     * Returns FULL package names (no truncation); consumer can aggregate at any depth.
     */
    fun scanClassesFromStream(inputStream: InputStream): ClassScanResult {
        val packages = mutableMapOf<String, Int>()
        var totalClasses = 0

        try {
            val zis = ZipInputStream(inputStream)
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.endsWith(".class") && !entry.isDirectory) {
                    val fileName = name.substringAfterLast('/')
                    if (fileName != "module-info.class" && fileName != "package-info.class") {
                        val lastSlash = name.lastIndexOf('/')
                        if (lastSlash > 0) {
                            val pkg = name.substring(0, lastSlash).replace('/', '.')
                            packages[pkg] = (packages[pkg] ?: 0) + 1
                        } else {
                            packages["(default)"] = (packages["(default)"] ?: 0) + 1
                        }
                        totalClasses++
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
            zis.close()
        } catch (e: Exception) {
            // Silently handle — will show in warnings
        }

        return ClassScanResult(packages, totalClasses)
    }

    // ── Directory-based class scanner ────────────────────────────────────

    /**
     * Scan a directory tree of .class files (for app / dynamic-feature modules).
     * These modules compile to `build/intermediates/javac/<variant>/classes/` or
     * `build/tmp/kotlin-classes/<variant>/` rather than a classes.jar.
     */
    fun scanClassesFromDirectory(classDir: File?): ClassScanResult {
        val packages = mutableMapOf<String, Int>()
        var totalClasses = 0

        if (classDir == null || !classDir.exists() || !classDir.isDirectory) {
            return ClassScanResult(packages, totalClasses)
        }

        classDir.walkTopDown().filter { it.isFile && it.name.endsWith(".class") }.forEach { file ->
            if (file.name == "module-info.class" || file.name == "package-info.class") return@forEach
            val relativePath = classDir.toPath().relativize(file.toPath()).toString()
            val normalized = relativePath.replace(File.separatorChar, '/')
            val lastSlash = normalized.lastIndexOf('/')
            if (lastSlash > 0) {
                val pkg = normalized.substring(0, lastSlash).replace('/', '.')
                packages[pkg] = (packages[pkg] ?: 0) + 1
            } else {
                packages["(default)"] = (packages["(default)"] ?: 0) + 1
            }
            totalClasses++
        }

        return ClassScanResult(packages, totalClasses)
    }


    // ── AAR extraction ───────────────────────────────────────────────────

    /**
     * Extract class info from an AAR (classes.jar + libs*.jar inside).
     */
    fun extractClassesFromAAR(zip: ZipFile): ClassResult {
        val allPackages = mutableMapOf<String, Int>()
        var totalClasses = 0
        val warnings = mutableListOf<String>()

        // classes.jar
        val classesJarEntry = zip.getEntry("classes.jar")
        if (classesJarEntry != null) {
            val result = scanClassesFromStream(zip.getInputStream(classesJarEntry))
            result.packages.forEach { (pkg, count) -> allPackages[pkg] = (allPackages[pkg] ?: 0) + count }
            totalClasses += result.classCount
        } else {
            warnings.add("No classes.jar found in AAR")
        }

        // Additional classes (classes2.jar, classes3.jar, etc.)
        for (i in 2..10) {
            val extraEntry = zip.getEntry("classes$i.jar")
            if (extraEntry != null) {
                val result = scanClassesFromStream(zip.getInputStream(extraEntry))
                result.packages.forEach { (pkg, count) -> allPackages[pkg] = (allPackages[pkg] ?: 0) + count }
                totalClasses += result.classCount
            }
        }

        // libs/*.jar inside AAR
        zip.entries().asSequence()
            .filter { it.name.startsWith("libs/") && it.name.endsWith(".jar") && !it.isDirectory }
            .forEach { entry ->
                val result = scanClassesFromStream(zip.getInputStream(entry))
                result.packages.forEach { (pkg, count) -> allPackages[pkg] = (allPackages[pkg] ?: 0) + count }
                totalClasses += result.classCount
            }

        return ClassResult(allPackages, totalClasses, warnings)
    }

    // ── Plain JAR extraction ─────────────────────────────────────────────

    /**
     * Extract class info from a plain JAR file.
     */
    fun extractClassesFromJAR(jarFile: File): ClassResult {
        val warnings = mutableListOf<String>()
        return try {
            val result = scanClassesFromStream(FileInputStream(jarFile))
            ClassResult(result.packages, result.classCount, warnings)
        } catch (e: Exception) {
            warnings.add("Failed to read JAR: ${e.message}")
            ClassResult(warnings = warnings)
        }
    }

    // ── Local library module extraction ──────────────────────────────────

    /**
     * Extract class info from local module build intermediates.
     */
    fun extractClassesFromLocalModule(projectDir: File, variant: String): ClassResult {
        val allPackages = mutableMapOf<String, Int>()
        var totalClasses = 0
        val warnings = mutableListOf<String>()

        // Check AAR output first (most reliable, contains everything)
        val aarDir = File(projectDir, "build/outputs/aar")
        if (aarDir.exists()) {
            val aarFile = aarDir.listFiles()?.firstOrNull { it.name.endsWith(".aar") }
            if (aarFile != null) {
                try {
                    ZipFile(aarFile).use { zip -> return extractClassesFromAAR(zip) }
                } catch (e: Exception) {
                    warnings.add("Failed to read local AAR: ${e.message}")
                }
            }
        }

        // Intermediates base dirs to search (AGP 7.x and 8.x)
        val baseDirs = listOf("compile_library_classes_jar", "runtime_library_classes_jar", "full_jar")
        val variantNames = (listOf(variant) + VariantUtils.splitVariant(variant) + listOf("release", "debug")).distinct()

        var classesJar: File? = null
        outer@ for (base in baseDirs) {
            for (v in variantNames) {
                val variantDir = File(projectDir, "build/intermediates/$base/$v")
                if (!variantDir.exists()) continue
                // Direct check (AGP 7.x)
                val direct = File(variantDir, "classes.jar")
                if (direct.exists()) { classesJar = direct; break@outer }
                // Search one level deeper (AGP 8.x task-name subdir)
                variantDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                    val nested = File(subDir, "classes.jar")
                    if (nested.exists()) { classesJar = nested; return@forEach }
                }
                if (classesJar != null) break@outer
            }
        }

        if (classesJar != null) {
            try {
                val result = scanClassesFromStream(FileInputStream(classesJar))
                result.packages.forEach { (pkg, count) -> allPackages[pkg] = (allPackages[pkg] ?: 0) + count }
                totalClasses += result.classCount
            } catch (e: Exception) {
                warnings.add("Failed to read classes.jar: ${e.message}")
            }
        } else {
            warnings.add("No classes.jar found in build intermediates (has the module been built?)")
        }

        return ClassResult(allPackages, totalClasses, warnings)
    }

    // ── App / dynamic-feature module extraction ──────────────────────────

    /**
     * Extract class info from an app or dynamic-feature module.
     * These modules don't produce classes.jar; they compile to:
     *   build/intermediates/javac/<variant>/classes/     (Java)
     *   build/tmp/kotlin-classes/<variant>/              (Kotlin)
     */
    fun extractClassesFromAppOrFeatureModule(projectDir: File, variant: String): ClassResult {
        val allPackages = mutableMapOf<String, Int>()
        var totalClasses = 0
        val warnings = mutableListOf<String>()
        var found = false

        val variantNames = (listOf(variant) + VariantUtils.splitVariant(variant) + listOf("release", "debug")).distinct()

        for (v in variantNames) {
            // Java compiled classes
            val javacBaseDir = File(projectDir, "build/intermediates/javac/$v")
            var javacDir: File? = null
            if (javacBaseDir.exists()) {
                val direct = File(javacBaseDir, "classes")
                if (direct.exists()) {
                    javacDir = direct
                } else {
                    // Search one level deeper for a 'classes' directory (AGP 8.x task-name subdir)
                    javacDir = javacBaseDir.listFiles()?.filter { it.isDirectory }?.firstNotNullOfOrNull { subDir ->
                        val candidate = File(subDir, "classes")
                        if (candidate.exists()) candidate else null
                    }
                }
            }
            if (javacDir != null) {
                found = true
                val result = scanClassesFromDirectory(javacDir)
                result.packages.forEach { (pkg, count) -> allPackages[pkg] = (allPackages[pkg] ?: 0) + count }
                totalClasses += result.classCount
            }

            // Kotlin compiled classes
            val kotlinDir = File(projectDir, "build/tmp/kotlin-classes/$v")
            if (kotlinDir.exists()) {
                found = true
                val result = scanClassesFromDirectory(kotlinDir)
                result.packages.forEach { (pkg, count) ->
                    if (pkg !in allPackages) {
                        allPackages[pkg] = count
                    } else {
                        allPackages[pkg] = allPackages[pkg]!! + count
                    }
                }
                totalClasses += result.classCount
            }

            if (found) break
        }

        if (!found) {
            warnings.add("No compiled classes found in javac/kotlin intermediates (has the module been built?)")
        }

        // Supplement with source-file scan
        val sourceDirs = mutableListOf<File>()
        var flavor = ""
        val varMatch = Regex("^([a-z][a-zA-Z_]*?)([A-Z][a-zA-Z_]*)$").matchEntire(variant)
        if (varMatch != null) flavor = varMatch.groupValues[1]

        listOf("java", "kotlin").forEach { lang ->
            sourceDirs.add(File(projectDir, "src/main/$lang"))
            if (flavor.isNotEmpty()) sourceDirs.add(File(projectDir, "src/$flavor/$lang"))
        }

        val sourcePackages = mutableMapOf<String, Int>()
        sourceDirs.filter { it.exists() }.forEach { srcDir ->
            srcDir.walkTopDown().filter { it.isFile && (it.name.endsWith(".java") || it.name.endsWith(".kt")) }.forEach { f ->
                val rel = srcDir.toPath().relativize(f.parentFile.toPath()).toString()
                if (rel.isNotEmpty()) {
                    val pkg = rel.replace(File.separatorChar, '.')
                    sourcePackages[pkg] = (sourcePackages[pkg] ?: 0) + 1
                }
            }
        }

        var sourceOnlyPackages = 0
        var sourceOnlyClasses = 0
        sourcePackages.forEach { (pkg, count) ->
            if (pkg !in allPackages) {
                allPackages[pkg] = count
                totalClasses += count
                sourceOnlyPackages++
                sourceOnlyClasses += count
            }
        }
        if (sourceOnlyPackages > 0) {
            warnings.add("Supplemented $sourceOnlyPackages packages ($sourceOnlyClasses source files) not found in compiled output")
        }

        return ClassResult(allPackages, totalClasses, warnings)
    }
}

