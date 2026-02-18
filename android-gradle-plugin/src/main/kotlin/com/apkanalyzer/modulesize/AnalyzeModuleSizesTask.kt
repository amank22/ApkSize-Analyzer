package com.apkanalyzer.modulesize

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.TreeMap
import java.util.TreeSet
import java.util.zip.ZipFile

/**
 * Analyzes Android module and AAR dependencies to extract resource counts,
 * package/class info, native library details, and produces file-level
 * resource mapping for APK size attribution across functional units.
 */
open class AnalyzeModuleSizesTask : DefaultTask() {

    init {
        group = "analysis"
        description = "Analyzes dependencies: module sizes + resource file mapping"
    }

    @TaskAction
    fun analyze() {
        val rootProject = project.rootProject
        val config = rootProject.extensions.getByType(ModuleSizeAnalysisExtension::class.java)

        // Apply default output paths (to app module build dir)
        applyDefaultOutputPaths(config)
        // Apply -P property overrides
        applyPropertyOverrides(config)

        logger.lifecycle("=== Module Size Analysis ===")
        logger.lifecycle("  Variant           : ${config.variant}")
        logger.lifecycle("  Package Depth     : ${config.packageDepth}")
        logger.lifecycle("  Include           : ${config.includePatterns}")
        logger.lifecycle("  Exclude           : ${config.excludePatterns}")
        logger.lifecycle("  Local Modules     : ${config.includeLocalModules}")
        logger.lifecycle("  Module Analysis   : ${config.enableModuleAnalysis}")
        logger.lifecycle("  Resource Mapping  : ${config.enableResourceMapping}")
        logger.lifecycle("  FU Mapping        : ${config.functionalUnitMapping.size} units defined")
        logger.lifecycle("")

        // ══════════════════════════════════════════════════════════════════
        //  Shared state
        // ══════════════════════════════════════════════════════════════════
        val allModuleIds = TreeSet<String>()
        var analysisModules: Map<String, ModuleInfo>? = null
        var analysisSummary: Map<String, Any?>? = null
        var rawResourceMapping: MutableMap<String, MutableList<String>>? = null
        var totalMappedFiles = 0

        // Detect dynamic feature modules
        val dynamicFeatures = mutableSetOf<String>()
        val dynamicFeatureDeps = mutableMapOf<String, MutableSet<String>>()
        try {
            val androidExt = project.extensions.findByName("android")
            if (androidExt != null) {
                @Suppress("UNCHECKED_CAST")
                val features = androidExt.javaClass.getMethod("getDynamicFeatures")
                    .invoke(androidExt) as? Collection<String>
                features?.let { dynamicFeatures.addAll(it) }
            }
        } catch (e: Exception) {
            logger.warn("  Could not read dynamicFeatures: ${e.message}")
        }
        if (dynamicFeatures.isNotEmpty()) logger.lifecycle("  Dynamic features  : $dynamicFeatures")

        // ══════════════════════════════════════════════════════════════════
        //  1. Find the right configuration for the variant
        // ══════════════════════════════════════════════════════════════════
        val resolvedConfig = findConfiguration(config)

        // ══════════════════════════════════════════════════════════════════
        //  2. Resolve artifacts using lenient artifactView
        // ══════════════════════════════════════════════════════════════════
        val artifactSet = resolvedConfig.incoming.artifactView { it.lenient(true) }.artifacts
        val resolvedProjectPaths = HashSet<String>()

        logger.lifecycle("  Resolved artifacts : ${artifactSet.artifacts.size} (may include duplicates from transforms)")
        logger.lifecycle("")

        val modules = LinkedHashMap<String, ModuleInfo>()
        var skipped = 0
        var duplicates = 0

        // ── Process resolved artifacts (remote AARs/JARs + some local) ──
        artifactSet.forEach { artifact ->
            val componentId = artifact.id.componentIdentifier
            val isLocal = componentId is ProjectComponentIdentifier
            val moduleId: String
            val moduleType: String
            val artifactFile: File = artifact.file

            if (isLocal) {
                moduleId = componentId.projectPath
                moduleType = "local_module"
                resolvedProjectPaths.add(componentId.projectPath)
                if (!config.includeLocalModules) { skipped++; return@forEach }
            } else {
                moduleId = componentIdToString(componentId)
                moduleType = if (artifactFile.name.endsWith(".aar")) "remote_aar" else "remote_jar"
            }

            if (moduleId in modules) { duplicates++; return@forEach }

            logger.lifecycle("  Analyzing: $moduleId ($moduleType)")
            val moduleInfo = ModuleInfo(type = moduleType, file = artifactFile.absolutePath)

            try {
                when (moduleType) {
                    "remote_aar" -> processRemoteAAR(moduleInfo, artifactFile)
                    "remote_jar" -> processRemoteJAR(moduleInfo, artifactFile)
                    "local_module" -> {
                        val localProject = rootProject.findProject(
                            (componentId as ProjectComponentIdentifier).projectPath
                        )
                        if (localProject != null) {
                            if (artifactFile.name.endsWith(".aar") && artifactFile.exists()) {
                                processRemoteAAR(moduleInfo, artifactFile)
                            } else {
                                processLocalModuleFromDisk(moduleInfo, localProject.projectDir, config)
                            }
                            moduleInfo.file = localProject.projectDir.absolutePath
                        } else {
                            moduleInfo.warnings.add(
                                "Could not find project for ${componentId.projectPath}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                moduleInfo.warnings.add("Error processing: ${e.message}")
                logger.warn("    WARNING: ${e.message}")
            }

            modules[moduleId] = moduleInfo
        }

        // ── Find local project deps that failed variant resolution ───────
        resolvedConfig.allDependencies.forEach { dep ->
            if (dep is ProjectDependency) {
                val depProjectPath = dep.dependencyProject.path
                if (depProjectPath in resolvedProjectPaths || depProjectPath in modules) return@forEach
                if (!config.includeLocalModules) { skipped++; return@forEach }

                val localProject = dep.dependencyProject
                logger.lifecycle("  Analyzing (variant fallback): $depProjectPath")

                val moduleInfo = ModuleInfo(
                    type = "local_module",
                    file = localProject.projectDir.absolutePath,
                    warnings = mutableListOf(
                        "Variant resolution failed (flavor mismatch); scanned from source/intermediates directly"
                    ),
                )
                try {
                    processLocalModuleFromDisk(moduleInfo, localProject.projectDir, config)
                } catch (e: Exception) {
                    moduleInfo.warnings.add("Error: ${e.message}")
                    logger.warn("    WARNING: ${e.message}")
                }
                modules[depProjectPath] = moduleInfo
            }
        }

        // ── Process the app module itself ────────────────────────────────
        val appModuleId = project.path
        if (appModuleId !in modules) {
            logger.lifecycle("  Analyzing (app module): $appModuleId")
            val moduleInfo = ModuleInfo(type = "app_module", file = project.projectDir.absolutePath)
            try {
                processAppOrFeatureModule(moduleInfo, project.projectDir, config)
            } catch (e: Exception) {
                moduleInfo.warnings.add("Error processing app module: ${e.message}")
                logger.warn("    WARNING: ${e.message}")
            }
            modules[appModuleId] = moduleInfo
        }

        // ── Process dynamic feature modules ──────────────────────────────
        if (config.includeLocalModules) {
            dynamicFeatures.forEach { featurePath ->
                if (featurePath in modules) return@forEach
                val featureProject = rootProject.findProject(featurePath) ?: return@forEach
                logger.lifecycle("  Analyzing (dynamic feature): $featurePath")
                val moduleInfo = ModuleInfo(type = "dynamic_feature", file = featureProject.projectDir.absolutePath)
                try {
                    processAppOrFeatureModule(moduleInfo, featureProject.projectDir, config)
                } catch (e: Exception) {
                    moduleInfo.warnings.add("Error: ${e.message}")
                    logger.warn("    WARNING: ${e.message}")
                }
                modules[featurePath] = moduleInfo
            }
        }

        // ══════════════════════════════════════════════════════════════════
        //  STEP 1: Module Analysis
        // ══════════════════════════════════════════════════════════════════
        if (config.enableModuleAnalysis) {
            logger.lifecycle("")
            logger.lifecycle("  Analyzed   : ${modules.size} modules")
            logger.lifecycle("  Duplicates : $duplicates (same module, different artifact type -- skipped)")
            logger.lifecycle("  Filtered   : $skipped (excluded by patterns)")

            var totalDeclaredRes = 0; var totalClasses = 0
            var totalNativeLibs = 0; var totalNativeSize = 0L
            modules.values.forEach { data ->
                totalDeclaredRes += data.resources.declared.total
                totalClasses += data.classCount
                totalNativeLibs += data.nativeLibs.size
                data.nativeLibs.forEach { totalNativeSize += it.sizeBytes }
            }

            fun <T : Comparable<T>> topN(selector: (Map.Entry<String, ModuleInfo>) -> T, label: String) =
                modules.entries.map { mapOf("module" to it.key, label to selector(it)) }
                    .sortedByDescending { @Suppress("UNCHECKED_CAST") (it[label] as T) }
                    .take(20).filter {
                        val v = it[label]
                        when (v) { is Int -> v > 0; is Long -> v > 0L; else -> true }
                    }

            val moduleSummary: Map<String, Any?> = mapOf(
                "totalModules" to modules.size,
                "totalDeclaredResources" to totalDeclaredRes,
                "totalClasses" to totalClasses,
                "totalNativeLibs" to totalNativeLibs,
                "totalNativeSizeBytes" to totalNativeSize,
                "topByDeclaredResources" to topN({ it.value.resources.declared.total }, "count"),
                "topByClasses" to topN({ it.value.classCount }, "count"),
                "topByNativeSize" to topN({ it.value.nativeLibs.sumOf { l -> l.sizeBytes } }, "sizeBytes"),
                "topByTransitiveRes" to topN({ it.value.resources.transitive }, "count"),
            )

            allModuleIds.addAll(modules.keys)
            analysisModules = modules
            analysisSummary = moduleSummary

            logger.lifecycle("")
            logger.lifecycle("=== Module Analysis Summary ===")
            logger.lifecycle("  Modules analyzed     : ${modules.size}")
            logger.lifecycle("  Declared resources   : $totalDeclaredRes")
            logger.lifecycle("  Total classes        : $totalClasses")
            logger.lifecycle("  Native libraries     : $totalNativeLibs")
            logger.lifecycle("  Native size (bytes)  : $totalNativeSize")
            logger.lifecycle("================================")
        } else {
            logger.lifecycle("  Module analysis SKIPPED (enableModuleAnalysis=false)")
        }

        // ══════════════════════════════════════════════════════════════════
        //  STEP 2: Resource Mapping (filePath → module)
        // ══════════════════════════════════════════════════════════════════
        if (config.enableResourceMapping) {
            logger.lifecycle("")
            logger.lifecycle("--- Resource Mapping ---")

            // Resolve each dynamic feature's deps
            dynamicFeatures.forEach { featurePath ->
                val featureProject = rootProject.findProject(featurePath) ?: return@forEach
                val featureName = featureProject.name
                val featureDepIds = mutableSetOf<String>()

                val frc = featureProject.configurations.findByName("${config.variant}RuntimeClasspath")
                    ?: featureProject.configurations.findByName("releaseRuntimeClasspath")
                if (frc != null) {
                    try {
                        frc.incoming.artifactView { vc -> vc.lenient(true) }.artifacts.forEach { art ->
                            val cid = art.id.componentIdentifier
                            if (cid is ProjectComponentIdentifier) {
                                if (cid.projectPath != project.path) featureDepIds.add(cid.projectPath)
                            } else {
                                try { featureDepIds.add(componentIdToString(cid)) } catch (e: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("  Could not resolve deps for feature $featurePath: ${e.message}")
                    }
                    frc.allDependencies.forEach { dep ->
                        if (dep is ProjectDependency && dep.dependencyProject.path != project.path)
                            featureDepIds.add(dep.dependencyProject.path)
                    }
                }
                dynamicFeatureDeps[featureName] = featureDepIds
                logger.lifecycle("  Feature '$featureName': ${featureDepIds.size} deps")
            }

            // Collect base module's dep IDs
            val baseDepIds = mutableSetOf<String>()
            try {
                resolvedConfig.incoming.artifactView { vc -> vc.lenient(true) }.artifacts.forEach { art ->
                    val cid = art.id.componentIdentifier
                    if (cid is ProjectComponentIdentifier) baseDepIds.add(cid.projectPath)
                    else try { baseDepIds.add(componentIdToString(cid)) } catch (e: Exception) {}
                }
            } catch (e: Exception) {}
            resolvedConfig.allDependencies.forEach { dep ->
                if (dep is ProjectDependency) baseDepIds.add(dep.dependencyProject.path)
            }

            fun getApkSplit(modId: String): String {
                for ((featureName, featureDeps) in dynamicFeatureDeps) {
                    if (modId in featureDeps && modId !in baseDepIds) return featureName
                }
                for (fp in dynamicFeatures) {
                    val fp2 = rootProject.findProject(fp)
                    if (fp2 != null && (fp2.path == modId || fp2.name == modId)) return fp2.name
                }
                return "base"
            }

            val resourceMapping = mutableMapOf<String, MutableList<String>>()

            fun addMapping(filePath: String, modId: String) {
                val existing = resourceMapping[filePath]
                if (existing == null) {
                    resourceMapping[filePath] = mutableListOf(modId)
                    totalMappedFiles++
                } else if (modId !in existing) {
                    existing.add(modId)
                }
            }

            fun mapResArtifact(modId: String, artFile: File?, apkSplit: String) {
                if (artFile == null || !artFile.exists() || !artFile.name.endsWith(".aar")) return
                try {
                    ZipFile(artFile).use { zip ->
                        ResourceExtractor.scanFilePathsFromZip(zip).forEach { resPath ->
                            addMapping("$apkSplit/$resPath", modId)
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("    Could not scan $modId: ${e.message}")
                }
            }

            // Process base config artifacts
            val processedRes = HashSet<String>()
            resolvedConfig.incoming.artifactView { vc -> vc.lenient(true) }.artifacts.forEach { artifact ->
                val cid = artifact.id.componentIdentifier
                val isLocal = cid is ProjectComponentIdentifier
                val modId = if (isLocal) {
                    if (!config.includeLocalModules) return@forEach
                    cid.projectPath
                } else {
                    componentIdToString(cid)
                }
                if (modId in processedRes) return@forEach
                processedRes.add(modId)
                mapResArtifact(modId, artifact.file, getApkSplit(modId))
            }

            // Variant fallback for local modules
            resolvedConfig.allDependencies.forEach { dep ->
                if (dep is ProjectDependency) {
                    val depPath = dep.dependencyProject.path
                    if (depPath in processedRes || !config.includeLocalModules) return@forEach
                    processedRes.add(depPath)

                    val pDir = dep.dependencyProject.projectDir
                    val apkSplit = getApkSplit(depPath)
                    val localAarDir = File(pDir, "build/outputs/aar")
                    val localAarFile = localAarDir.takeIf { it.exists() }
                        ?.listFiles()?.firstOrNull { it.name.endsWith(".aar") }

                    if (localAarFile != null) {
                        mapResArtifact(depPath, localAarFile, apkSplit)
                    } else {
                        ResourceExtractor.scanFilePathsFromLocalModule(pDir, dep.dependencyProject)
                            .forEach { resPath -> addMapping("$apkSplit/$resPath", depPath) }
                    }
                }
            }

            // App module's own resources
            val appResFiles = ResourceExtractor.scanFilePathsFromLocalModule(project.projectDir, project)
            appResFiles.forEach { resPath -> addMapping("base/$resPath", project.path) }
            logger.lifecycle("  App module (${project.path}): ${appResFiles.size} own files -> base/")

            // Dynamic feature modules' own resources + exclusive deps
            dynamicFeatures.forEach { featurePath ->
                val featureProject = rootProject.findProject(featurePath) ?: return@forEach
                val featureName = featureProject.name
                var featureFileCount = 0

                val featureOwnFiles = ResourceExtractor.scanFilePathsFromLocalModule(
                    featureProject.projectDir, featureProject
                )
                featureOwnFiles.forEach { resPath -> addMapping("$featureName/$resPath", featurePath) }
                featureFileCount += featureOwnFiles.size

                val featureAarDir = File(featureProject.projectDir, "build/outputs/aar")
                val featureAarFile = featureAarDir.takeIf { it.exists() }
                    ?.listFiles()?.firstOrNull { it.name.endsWith(".aar") }
                if (featureAarFile != null) {
                    try {
                        ZipFile(featureAarFile).use { zip ->
                            val aarFiles = ResourceExtractor.scanFilePathsFromZip(zip)
                            aarFiles.forEach { resPath -> addMapping("$featureName/$resPath", featurePath) }
                            featureFileCount += aarFiles.size
                        }
                    } catch (e: Exception) {}
                }

                // Feature-exclusive dependency resources
                val fConfig = featureProject.configurations.findByName("${config.variant}RuntimeClasspath")
                    ?: featureProject.configurations.findByName("releaseRuntimeClasspath")
                if (fConfig != null) {
                    try {
                        val processedFDeps = HashSet<String>()
                        fConfig.incoming.artifactView { vc -> vc.lenient(true) }.artifacts.forEach { art ->
                            val cid = art.id.componentIdentifier
                            val mId = if (cid is ProjectComponentIdentifier) {
                                if (cid.projectPath == project.path) return@forEach
                                cid.projectPath
                            } else {
                                try { componentIdToString(cid) } catch (e: Exception) { return@forEach }
                            }
                            if (mId in processedFDeps) return@forEach
                            processedFDeps.add(mId)
                            if (mId !in baseDepIds) mapResArtifact(mId, art.file, featureName)
                        }
                    } catch (e: Exception) {
                        logger.warn("  Could not scan feature $featureName deps: ${e.message}")
                    }
                }

                if (featureFileCount == 0) {
                    logger.lifecycle("  Feature '$featureName': no resource/asset/jni files (code-only module)")
                } else {
                    logger.lifecycle("  Feature '$featureName': $featureFileCount files -> $featureName/")
                }
            }

            resourceMapping.values.forEach { modList -> allModuleIds.addAll(modList) }
            rawResourceMapping = resourceMapping

            val collisions = resourceMapping.count { (_, v) -> v.size > 1 }
            logger.lifecycle("")
            logger.lifecycle("=== Resource Mapping Summary ===")
            logger.lifecycle("  Total mapped files : $totalMappedFiles")
            logger.lifecycle("  Collisions         : $collisions")
            logger.lifecycle("  Unique modules     : ${resourceMapping.values.flatten().toSet().size}")
            resourceMapping.keys.groupBy { it.substringBefore('/') }.forEach { (split, paths) ->
                logger.lifecycle("  ${split.padEnd(20)}: ${paths.size} files")
            }
            logger.lifecycle("=================================")
        } else {
            logger.lifecycle("  Resource mapping SKIPPED (enableResourceMapping=false)")
        }

        // ══════════════════════════════════════════════════════════════════
        //  STEP 3: Write reports
        // ══════════════════════════════════════════════════════════════════
        val gson = GsonBuilder().setPrettyPrinting().create()
        val allModules = allModuleIds.toList()
        val moduleIndexMap = mutableMapOf<String, Int>()
        allModules.forEachIndexed { idx, mod -> moduleIndexMap[mod] = idx }

        // ── Build functional unit mapping (FU name → module indices) ─────
        val fuMapping = LinkedHashMap<String, List<Int>>()
        val mappedModuleIndices = HashSet<Int>()

        config.functionalUnitMapping.forEach { (fuName, patterns) ->
            val matchingIndices = mutableListOf<Int>()
            allModules.forEachIndexed { idx, modId ->
                val normalizedId = modId.replace(':', '.')
                if (patterns.any { p -> GlobMatcher.matches(p.replace(':', '.'), normalizedId) }) {
                    matchingIndices.add(idx)
                }
            }
            if (matchingIndices.isNotEmpty()) {
                fuMapping[fuName] = matchingIndices
                mappedModuleIndices.addAll(matchingIndices)
            }
        }

        // Auto-assign unmapped modules
        val androidPlatformPrefixes = listOf("androidx.", "com.google.", "com.android.", "android.arch.")
        val androidPlatformIndices = mutableListOf<Int>()
        val thirdpartyIndices = mutableListOf<Int>()
        allModules.indices.forEach { idx ->
            if (idx in mappedModuleIndices) return@forEach
            val normalized = allModules[idx].replace(':', '.')
            if (androidPlatformPrefixes.any { normalized.startsWith(it) }) {
                androidPlatformIndices.add(idx)
            } else {
                thirdpartyIndices.add(idx)
            }
        }
        if (androidPlatformIndices.isNotEmpty()) fuMapping["android_platform"] = androidPlatformIndices
        if (thirdpartyIndices.isNotEmpty()) fuMapping["thirdparty"] = thirdpartyIndices

        // ── Write metadata file ──────────────────────────────────────────
        val metadataReport = linkedMapOf<String, Any?>(
            "generatedAt" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date()),
            "variant" to config.variant,
            "packageDepth" to config.packageDepth,
            "projectName" to rootProject.name,
            "appModule" to project.path,
            "dynamicFeatures" to dynamicFeatures.map { rootProject.findProject(it)?.name ?: it },
            "modules" to allModules,
            "functionalUnits" to fuMapping,
        )
        config.metadataFile!!.parentFile.mkdirs()
        config.metadataFile!!.writeText(compactNumberArrays(gson.toJson(metadataReport)))

        val autoFuNames = setOf("android_platform", "thirdparty")
        logger.lifecycle("")
        logger.lifecycle("=== Metadata ===")
        logger.lifecycle("  Total modules      : ${allModules.size}")
        logger.lifecycle("  Functional units   : ${fuMapping.keys.count { it !in autoFuNames }} defined")
        fuMapping.forEach { (fuName, indices) ->
            if (fuName !in autoFuNames) logger.lifecycle("    ${fuName.padEnd(20)}: ${indices.size} modules")
        }
        fuMapping["android_platform"]?.let { logger.lifecycle("  Android/Google     : ${it.size} modules (auto)") }
        fuMapping["thirdparty"]?.let { logger.lifecycle("  Third-party        : ${it.size} modules (auto)") }
        logger.lifecycle("  Report: ${config.metadataFile!!.absolutePath}")
        logger.lifecycle("=================")

        // ── Write module analysis report ─────────────────────────────────
        if (analysisModules != null) {
            val moduleReport = mapOf("modules" to analysisModules, "summary" to analysisSummary)
            config.outputFile!!.parentFile.mkdirs()
            config.outputFile!!.writeText(gson.toJson(moduleReport))
            logger.lifecycle("  Module analysis  : ${config.outputFile!!.absolutePath}")
        }

        // ── Write resource mapping report ────────────────────────────────
        if (rawResourceMapping != null) {
            writeResourceMappingReport(
                config, gson, rawResourceMapping!!, moduleIndexMap, totalMappedFiles
            )
        }

        // ── Copy shrunk resources .ap_ (for AAPT2 path-shortening reverse map) ──
        copyShrunkResourcesAp(config)

        // ── Write package mapping report ─────────────────────────────────
        if (analysisModules != null) {
            writePackageMappingReport(config, gson, analysisModules!!, moduleIndexMap)
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ══════════════════════════════════════════════════════════════════════

    private fun findConfiguration(config: ModuleSizeAnalysisExtension): Configuration {
        val configName = "${config.variant}RuntimeClasspath"
        project.configurations.findByName(configName)?.let { return it }

        val alternatives = listOf(
            "${config.variant}CompileClasspath",
            "releaseRuntimeClasspath",
            "debugRuntimeClasspath",
        )
        for (alt in alternatives) {
            val found = project.configurations.findByName(alt)
            if (found != null) {
                logger.lifecycle("  Configuration '$configName' not found, using '$alt'")
                return found
            }
        }
        throw GradleException(
            "Could not find configuration '$configName'. " +
                "Available: ${project.configurations.joinToString(", ") { it.name }}"
        )
    }

    private fun applyDefaultOutputPaths(config: ModuleSizeAnalysisExtension) {
        val buildDir = project.layout.buildDirectory.asFile.get()
        if (config.outputFile == null) config.outputFile = File(buildDir, "reports/module-size-analysis.json")
        if (config.resourceMappingFile == null) config.resourceMappingFile = File(buildDir, "reports/resource-mapping.json")
        if (config.metadataFile == null) config.metadataFile = File(buildDir, "reports/module-metadata.json")
        if (config.packageMappingFile == null) config.packageMappingFile = File(buildDir, "reports/package-mapping.json")
    }

    private fun applyPropertyOverrides(config: ModuleSizeAnalysisExtension) {
        project.findProperty("moduleSizeAnalysis.variant")?.let { config.variant = it.toString() }
        project.findProperty("moduleSizeAnalysis.packageDepth")?.let { config.packageDepth = it.toString().toInt() }
        project.findProperty("moduleSizeAnalysis.includePatterns")?.let {
            config.includePatterns = it.toString().split(',').map { s -> s.trim() }
        }
        project.findProperty("moduleSizeAnalysis.excludePatterns")?.let {
            config.excludePatterns = it.toString().split(',').map { s -> s.trim() }
        }
        project.findProperty("moduleSizeAnalysis.outputFile")?.let { config.outputFile = project.file(it) }
        project.findProperty("moduleSizeAnalysis.includeLocalModules")?.let {
            config.includeLocalModules = it.toString().toBoolean()
        }
        project.findProperty("moduleSizeAnalysis.enableModuleAnalysis")?.let {
            config.enableModuleAnalysis = it.toString().toBoolean()
        }
        project.findProperty("moduleSizeAnalysis.enableResourceMapping")?.let {
            config.enableResourceMapping = it.toString().toBoolean()
        }
    }

    private fun componentIdToString(componentId: Any): String {
        return try {
            val group = componentId.javaClass.getMethod("getGroup").invoke(componentId)
            val module = componentId.javaClass.getMethod("getModule").invoke(componentId)
            val version = componentId.javaClass.getMethod("getVersion").invoke(componentId)
            "$group:$module:$version"
        } catch (e: Exception) {
            try {
                componentId.javaClass.getMethod("getDisplayName").invoke(componentId)?.toString()
                    ?: componentId.toString()
            } catch (e: Exception) {
                componentId.toString()
            }
        }
    }

    private fun processRemoteAAR(moduleInfo: ModuleInfo, artifactFile: File) {
        ZipFile(artifactFile).use { zip ->
            val resResult = ResourceExtractor.extractResources(zip)
            moduleInfo.resources = ResourceInfoOut(resResult.declared, resResult.totalRTxtEntries, resResult.transitive)
            moduleInfo.warnings.addAll(resResult.warnings)

            val classResult = ClassExtractor.extractClassesFromAAR(zip)
            moduleInfo.packages = classResult.packages
            moduleInfo.classCount = classResult.classCount
            moduleInfo.warnings.addAll(classResult.warnings)

            moduleInfo.nativeLibs.addAll(NativeLibExtractor.extractNativeLibsFromAAR(zip))
        }
    }

    private fun processRemoteJAR(moduleInfo: ModuleInfo, artifactFile: File) {
        val classResult = ClassExtractor.extractClassesFromJAR(artifactFile)
        moduleInfo.packages = classResult.packages
        moduleInfo.classCount = classResult.classCount
        moduleInfo.warnings.addAll(classResult.warnings)
    }

    private fun processLocalModuleFromDisk(
        moduleInfo: ModuleInfo, projectDir: File, config: ModuleSizeAnalysisExtension,
    ) {
        val aarDir = File(projectDir, "build/outputs/aar")
        val aarFile = aarDir.takeIf { it.exists() }?.listFiles()?.firstOrNull { it.name.endsWith(".aar") }

        if (aarFile != null) {
            ZipFile(aarFile).use { zip ->
                val resResult = ResourceExtractor.extractResources(zip)
                moduleInfo.resources = ResourceInfoOut(resResult.declared, resResult.totalRTxtEntries, resResult.transitive)
                moduleInfo.warnings.addAll(resResult.warnings)

                val classResult = ClassExtractor.extractClassesFromAAR(zip)
                moduleInfo.packages = classResult.packages
                moduleInfo.classCount = classResult.classCount
                moduleInfo.warnings.addAll(classResult.warnings)

                moduleInfo.nativeLibs.addAll(NativeLibExtractor.extractNativeLibsFromAAR(zip))
            }
        } else {
            val resResult = ResourceExtractor.extractResourcesFromLocalModule(projectDir, config.variant)
            moduleInfo.resources = ResourceInfoOut(resResult.declared, resResult.totalRTxtEntries, resResult.transitive)
            moduleInfo.warnings.addAll(resResult.warnings)

            val classResult = ClassExtractor.extractClassesFromLocalModule(projectDir, config.variant)
            moduleInfo.packages = classResult.packages
            moduleInfo.classCount = classResult.classCount
            moduleInfo.warnings.addAll(classResult.warnings)

            moduleInfo.nativeLibs.addAll(NativeLibExtractor.extractNativeLibsFromLocalModule(projectDir, config.variant))
        }
    }

    private fun processAppOrFeatureModule(
        moduleInfo: ModuleInfo, projectDir: File, config: ModuleSizeAnalysisExtension,
    ) {
        val resResult = ResourceExtractor.extractResourcesFromLocalModule(projectDir, config.variant)
        moduleInfo.resources = ResourceInfoOut(resResult.declared, resResult.totalRTxtEntries, resResult.transitive)
        moduleInfo.warnings.addAll(resResult.warnings)

        val classResult = ClassExtractor.extractClassesFromAppOrFeatureModule(projectDir, config.variant)
        moduleInfo.packages = classResult.packages
        moduleInfo.classCount = classResult.classCount
        moduleInfo.warnings.addAll(classResult.warnings)

        moduleInfo.nativeLibs.addAll(NativeLibExtractor.extractNativeLibsFromLocalModule(projectDir, config.variant))
    }

    /**
     * Locate the pre-optimization shrunk resources `.ap_` from AGP intermediates
     * (`shrunk_resources_binary_format/<variant>/.../*.ap_`) and copy it to the
     * reports directory as `shrunk-resources.ap_`. The CLI uses this to build a
     * reverse map from AAPT2 shortened paths back to original resource paths.
     */
    private fun copyShrunkResourcesAp(config: ModuleSizeAnalysisExtension) {
        val variantNames = VariantUtils.splitVariant(config.variant)
        val intermediatesDir = File(project.projectDir, "build/intermediates/shrunk_resources_binary_format")

        val apFile = variantNames.firstNotNullOfOrNull { v ->
            val variantDir = File(intermediatesDir, v)
            if (!variantDir.exists()) return@firstNotNullOfOrNull null
            variantDir.walkTopDown().maxDepth(3).firstOrNull { it.isFile && it.name.endsWith(".ap_") }
        }

        if (apFile == null) {
            logger.lifecycle("  Shrunk resources .ap_ not found (resource shrinking may be disabled)")
            return
        }

        val reportsDir = config.resourceMappingFile!!.parentFile
        reportsDir.mkdirs()
        val dest = File(reportsDir, "shrunk-resources.ap_")
        apFile.copyTo(dest, overwrite = true)
        logger.lifecycle("  Shrunk resources : ${dest.absolutePath} (${apFile.length() / 1024} KB)")
    }

    // ── Report writers ───────────────────────────────────────────────────

    private fun writeResourceMappingReport(
        config: ModuleSizeAnalysisExtension,
        gson: com.google.gson.Gson,
        rawMapping: MutableMap<String, MutableList<String>>,
        moduleIndexMap: Map<String, Int>,
        totalMappedFilesCount: Int,
    ) {
        // Build negative-index mapping for FU overrides
        val fuNegativeIndex = mutableMapOf<String, Int>()
        val fuIndexLookup = mutableMapOf<String, String>()
        var nextNeg = -1
        fun registerFuIndex(fuName: String) {
            if (fuName !in fuNegativeIndex) {
                fuNegativeIndex[fuName] = nextNeg
                fuIndexLookup[nextNeg.toString()] = fuName
                nextNeg--
            }
        }
        config.resourceFUOverrides.keys.forEach { registerFuIndex(it) }
        config.resourceDirFUOverrides.keys.forEach { registerFuIndex(it) }

        // Directory-based FU overrides
        val dirOverrideFiles = HashMap<String, String>()
        config.resourceDirFUOverrides.forEach { (fuName, dirPaths) ->
            dirPaths.forEach { dirPath ->
                val dir = File(project.projectDir, dirPath)
                if (!dir.exists() || !dir.isDirectory) {
                    logger.warn("  resourceDirFUOverrides: dir not found: ${dir.absolutePath}")
                    return@forEach
                }
                dir.listFiles()?.filter { it.isDirectory }?.forEach { typeDir ->
                    typeDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        dirOverrideFiles["res/${typeDir.name}/${file.name}"] = fuName
                    }
                }
                logger.lifecycle("  Scanned $dirPath: ${dirOverrideFiles.size} RN resource files")
            }
        }

        // Convert to indexed mapping
        var fuOverrideCount = 0
        val indexedMapping = rawMapping.map { (path, modIds) ->
            var fuHit: String? = null

            if (config.resourceFUOverrides.isNotEmpty()) {
                for ((fuName, patterns) in config.resourceFUOverrides) {
                    if (patterns.any { GlobMatcher.matches(it, path) }) { fuHit = fuName; break }
                }
            }
            if (fuHit == null && dirOverrideFiles.isNotEmpty()) {
                val stripped = path.substringAfter('/', path)
                dirOverrideFiles[stripped]?.let { fuHit = it }
            }

            if (fuHit != null) {
                fuOverrideCount++
                path to listOf(fuNegativeIndex[fuHit]!!)
            } else {
                path to modIds.mapNotNull { moduleIndexMap[it] }
            }
        }.toMap()

        if (fuOverrideCount > 0) logger.lifecycle("  FU overrides applied: $fuOverrideCount files")
        val collisionCount = rawMapping.count { (_, v) -> v.size > 1 }

        val resReport = linkedMapOf<String, Any?>(
            "resourceMapping" to indexedMapping,
            "summary" to mapOf(
                "totalMappedFiles" to totalMappedFilesCount,
                "collisions" to collisionCount,
                "uniqueModules" to rawMapping.values.flatten().toSet().size,
                "byApkSplit" to rawMapping.keys.groupBy { it.substringBefore('/') }.mapValues { it.value.size },
            ),
        )
        if (fuIndexLookup.isNotEmpty()) resReport["fuIndex"] = fuIndexLookup

        config.resourceMappingFile!!.parentFile.mkdirs()
        config.resourceMappingFile!!.writeText(compactNumberArrays(gson.toJson(resReport)))
        logger.lifecycle("  Resource mapping : ${config.resourceMappingFile!!.absolutePath}")
    }

    private fun writePackageMappingReport(
        config: ModuleSizeAnalysisExtension,
        gson: com.google.gson.Gson,
        mods: Map<String, ModuleInfo>,
        moduleIndexMap: Map<String, Int>,
    ) {
        val leafMapping = TreeMap<String, MutableMap<Int, Int>>()
        var skippedShallow = 0
        var skippedOverride = 0

        mods.forEach { (modId, modData) ->
            val modIdx = moduleIndexMap[modId] ?: return@forEach
            modData.packages.forEach packages@{ (pkg, classCount) ->
                val overrideMatch = config.packageOverrides.entries.firstOrNull { (prefix, _) ->
                    pkg == prefix || pkg.startsWith("$prefix.")
                }
                if (overrideMatch != null && overrideMatch.value == "ignore") {
                    skippedOverride += classCount; return@packages
                }

                val parts = if (pkg == "(default)") emptyList() else pkg.split('.')
                if (parts.size < config.minPackageDepth) {
                    skippedShallow += classCount; return@packages
                }

                leafMapping.getOrPut(pkg) { mutableMapOf() }.merge(modIdx, classCount, Int::plus)
                for (d in parts.size - 1 downTo config.minPackageDepth) {
                    val ancestor = parts.subList(0, d).joinToString(".")
                    leafMapping.getOrPut(ancestor) { mutableMapOf() }.merge(modIdx, classCount, Int::plus)
                }
            }
        }

        val pkgMapping = TreeMap<String, List<List<Int>>>()
        leafMapping.forEach { (pkg, modCounts) ->
            pkgMapping[pkg] = modCounts.entries
                .map { (modIdx, count) -> listOf(modIdx, count) }
                .sortedByDescending { it[1] }
        }

        val pkgCollisions = pkgMapping.count { (_, v) -> v.size > 1 }
        val pkgReport = mapOf(
            "packageMapping" to pkgMapping,
            "summary" to mapOf(
                "totalPackages" to pkgMapping.size,
                "collisions" to pkgCollisions,
                "skippedShallowClasses" to skippedShallow,
                "skippedOverrideClasses" to skippedOverride,
                "minPackageDepth" to config.minPackageDepth,
            ),
        )
        config.packageMappingFile!!.parentFile.mkdirs()
        config.packageMappingFile!!.writeText(compactNumberArrays(gson.toJson(pkgReport)))

        logger.lifecycle("")
        logger.lifecycle("=== Package Mapping Summary ===")
        logger.lifecycle("  Packages mapped    : ${pkgMapping.size}")
        logger.lifecycle("  Collisions         : $pkgCollisions")
        logger.lifecycle("  Skipped (shallow)  : $skippedShallow classes")
        logger.lifecycle("  Skipped (override) : $skippedOverride classes")
        logger.lifecycle("  Report: ${config.packageMappingFile!!.absolutePath}")
        logger.lifecycle("================================")
    }

    private fun compactNumberArrays(json: String): String =
        json.replace(Regex("""\[\s*\n\s*(-?[\d,\s]+?)\s*\n\s*]""")) { match ->
            "[${match.groupValues[1].replace(Regex("\\s+"), "")}]"
        }
}

// ══════════════════════════════════════════════════════════════════════════
//  JSON output models
// ══════════════════════════════════════════════════════════════════════════

internal data class ModuleInfo(
    val type: String,
    var file: String,
    var resources: ResourceInfoOut = ResourceInfoOut(),
    var packages: MutableMap<String, Int> = mutableMapOf(),
    var classCount: Int = 0,
    var nativeLibs: MutableList<NativeLibInfo> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
)

internal data class ResourceInfoOut(
    var declared: ResourceCount = ResourceCount(),
    var totalRTxtEntries: Int = 0,
    var transitive: Int = 0,
)
