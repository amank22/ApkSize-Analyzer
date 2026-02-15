package com.gi.apksize.models

import com.gi.apksize.utils.ApkSizeHelpers
import com.gi.apksize.utils.Printer
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong

/**
 * Thread-safe raw data collector + independent LOB size calculator.
 *
 * **Phase 1 (collection)**: Processors call [collectFile] and [collectDexPackages] from parallel
 * threads. These just append to lock-free queues — no mapping logic runs here.
 *
 * **Phase 2 (calculation)**: After all processors finish, the task calls [buildResult] which
 * performs all mapping lookups, attribution, dedup, and aggregation single-threaded.
 */
class LobContext private constructor(
    private val resourceMapping: Map<String, List<Int>>,
    private val packageMapping: Map<String, List<List<Int>>>,
    private val moduleToFu: Map<Int, String>,
    /** Negative-index → FU name lookup (from resource-mapping.json `fuIndex` field). */
    private val fuIndex: Map<Int, String>,
    private val isAab: Boolean,
    private val appPackagePrefixes: List<String>,
) {
    /** Simple tuple for collected file data */
    data class FileEntry(val name: String, val sizeInBytes: Long, val fileType: String)

    // Thread-safe queues for raw data — written by parallel processors
    private val collectedFiles = ConcurrentLinkedQueue<FileEntry>()
    private val collectedDexPackages = ConcurrentLinkedQueue<DexPackageModel>()
    private val appPackagePathPrefixes = appPackagePrefixes.map { prefix ->
        prefix.trim().trim('.').replace('.', '/')
    }.filter { it.isNotEmpty() }

    /** Populated after [buildResult] — detailed list of unmatched entries for review. */
    var unmatchedDetails: UnmatchedDetails? = null
        private set

    /** Populated after [buildResult] — per-LOB attributed file/dex details for review. */
    var attributedDetails: AttributedDetails? = null
        private set

    /** Populated after [buildResult] — DEX overhead breakdown. */
    var dexOverheadDetails: DexOverheadDetails? = null
        private set

    // ───────────────────────────────────────────────
    // Phase 1: Data collection (called from processor loops)
    // ───────────────────────────────────────────────

    /** Collect a single file entry. Called from file processor for every file in the APK/AAB. */
    fun collectFile(name: String, sizeInBytes: Long, fileType: String) {
        collectedFiles.add(FileEntry(name, sizeInBytes, fileType))
    }

    /** Collect all DEX packages. Called from dex processor with the full uniquePackageList. */
    fun collectDexPackages(packages: List<DexPackageModel>) {
        collectedDexPackages.addAll(packages)
    }

    // ───────────────────────────────────────────────
    // Phase 2: Independent LOB calculation
    // ───────────────────────────────────────────────

    /**
     * Perform all LOB matching, attribution, and aggregation.
     * Called once after all processors complete — single-threaded.
     */
    fun buildResult(): LobAnalysisResult {
        val fuFileSizes = mutableMapOf<String, MutableMap<String, Long>>()
        var unmatchedFileSize = 0L
        var unmatchedFileCount = 0
        var totalFileSize = 0L
        val unmatchedFileDetailsList = mutableListOf<UnmatchedFileDetail>()
        val attributedFilesByLob = mutableMapOf<String, MutableList<AttributedFileDetail>>()
        val autoAttributedFileCountByLob = mutableMapOf<String, Int>()
        val autoAttributedFileSizeByLob = mutableMapOf<String, Long>()

        // --- File matching ---
        // Skip .dex files: their content is analyzed at the package level by the DEX processor.
        // Including them here would double-count code bytes.
        var skippedDexFileSize = 0L
        var skippedDexFileCount = 0
        val dexFileEntries = mutableListOf<DexFileEntry>()
        var ignoredFileCount = 0
        var ignoredFileSize = 0L
        for (file in collectedFiles) {
            if (file.name.endsWith(".dex")) {
                skippedDexFileSize += file.sizeInBytes
                skippedDexFileCount++
                dexFileEntries.add(DexFileEntry(file.name, file.sizeInBytes))
                continue
            }
            if (shouldIgnoreFileForLob(file.name)) {
                ignoredFileCount++
                ignoredFileSize += file.sizeInBytes
                continue
            }
            totalFileSize += file.sizeInBytes
            val category = categorize(file.fileType)
            val moduleIndices = resolveFileMapping(file.name)
            if (moduleIndices != null && moduleIndices.isNotEmpty()) {
                val lastIndex = moduleIndices.last()
                val fuName = if (lastIndex < 0) {
                    // Negative index → direct FU lookup via fuIndex
                    fuIndex[lastIndex] ?: "unmapped"
                } else {
                    // Non-negative index → module index → FU from metadata
                    moduleToFu[lastIndex] ?: "unmapped"
                }
                fuFileSizes.getOrPut(fuName) { mutableMapOf() }
                    .merge(category, file.sizeInBytes) { a, b -> a + b }
                attributedFilesByLob.getOrPut(fuName) { mutableListOf() }
                    .add(AttributedFileDetail(file.name, file.sizeInBytes, category))
            } else {
                val fallbackLob = resolveFallbackLobForFile(file.name, category)
                if (fallbackLob != null) {
                    fuFileSizes.getOrPut(fallbackLob) { mutableMapOf() }
                        .merge(category, file.sizeInBytes) { a, b -> a + b }
                    attributedFilesByLob.getOrPut(fallbackLob) { mutableListOf() }
                        .add(AttributedFileDetail(file.name, file.sizeInBytes, category))
                    autoAttributedFileCountByLob.merge(fallbackLob, 1) { a, b -> a + b }
                    autoAttributedFileSizeByLob.merge(fallbackLob, file.sizeInBytes) { a, b -> a + b }
                } else {
                    unmatchedFileSize += file.sizeInBytes
                    unmatchedFileCount++
                    unmatchedFileDetailsList.add(
                        UnmatchedFileDetail(file.name, file.sizeInBytes, category)
                    )
                }
            }
        }
        if (skippedDexFileCount > 0) {
            Printer.log("LOB file analysis: skipped $skippedDexFileCount .dex files " +
                    "(${skippedDexFileSize} bytes) — handled by DEX processor")
        }
        if (ignoredFileCount > 0) {
            Printer.log(
                "LOB file analysis: ignored $ignoredFileCount files " +
                        "(${ignoredFileSize} bytes) by policy"
            )
        }
        if (autoAttributedFileCountByLob.isNotEmpty()) {
            val breakdown = autoAttributedFileCountByLob.keys.sorted().joinToString(", ") { lob ->
                "$lob=${autoAttributedFileCountByLob[lob] ?: 0} files/" +
                        "${autoAttributedFileSizeByLob[lob] ?: 0L} bytes"
            }
            Printer.log("LOB file fallback attribution: $breakdown")
        }

        // --- DEX matching (hierarchical dedup) ---
        val fuDexSizes = mutableMapOf<String, Long>()
        var unmatchedDexSize = 0L
        var unmatchedDexCount = 0
        var totalDexSize = 0L
        val unmatchedDexDetailsList = mutableListOf<UnmatchedDexDetail>()
        val attributedDexByLob = mutableMapOf<String, MutableList<AttributedDexDetail>>()
        var ancestorRollupDexCount = 0
        var ancestorRollupDexSize = 0L
        val autoAttributedDexCountByLob = mutableMapOf<String, Int>()
        val autoAttributedDexSizeByLob = mutableMapOf<String, Long>()

        val allPackages = collectedDexPackages.toList()
        val sorted = allPackages.sortedByDescending { it.depth }
        // Track ALL processed package sizes (matched + unmatched) to avoid double-counting.
        // When a child is unmatched, its size must still be subtracted from the parent.
        val processedSizeByPkg = mutableMapOf<String, Long>()

        for (pkg in sorted) {
            totalDexSize += pkg.basePackageSize
            // Subtract sizes of ALL children already processed (matched or unmatched)
            val childrenSize = processedSizeByPkg.entries
                .filter { it.key.startsWith(pkg.basePackage + ".") }
                .sumOf { it.value }
            val remainingSize = pkg.basePackageSize - childrenSize
            if (remainingSize <= 0) continue

            val mappedPackage = resolveMappedPackageKey(pkg.basePackage)
            val entries = mappedPackage?.let { packageMapping[it] }
            if (entries != null && entries.isNotEmpty()) {
                if (mappedPackage != pkg.basePackage) {
                    ancestorRollupDexCount++
                    ancestorRollupDexSize += remainingSize
                }
                val totalClasses = entries.sumOf { it[1] }
                if (totalClasses > 0) {
                    for (entry in entries) {
                        val modIdx = entry[0]
                        val classCount = entry[1]
                        val fuName = moduleToFu[modIdx] ?: "unmapped"
                        val attributed = remainingSize * classCount / totalClasses
                        fuDexSizes.merge(fuName, attributed) { a, b -> a + b }
                        attributedDexByLob.getOrPut(fuName) { mutableListOf() }
                            .add(AttributedDexDetail(pkg.basePackage, attributed))
                    }
                }
            } else {
                val fallbackLob = resolveFallbackLobForDexPackage(pkg.basePackage)
                if (fallbackLob != null) {
                    fuDexSizes.merge(fallbackLob, remainingSize) { a, b -> a + b }
                    attributedDexByLob.getOrPut(fallbackLob) { mutableListOf() }
                        .add(AttributedDexDetail(pkg.basePackage, remainingSize))
                    autoAttributedDexCountByLob.merge(fallbackLob, 1) { a, b -> a + b }
                    autoAttributedDexSizeByLob.merge(fallbackLob, remainingSize) { a, b -> a + b }
                } else {
                    unmatchedDexSize += remainingSize
                    unmatchedDexCount++
                    unmatchedDexDetailsList.add(
                        UnmatchedDexDetail(pkg.basePackage, remainingSize)
                    )
                }
            }
            // Always record processed size so parent packages can subtract it
            processedSizeByPkg[pkg.basePackage] = remainingSize
        }

        if (ancestorRollupDexCount > 0) {
            Printer.log(
                "LOB DEX ancestor rollup: resolved $ancestorRollupDexCount packages " +
                        "(${ancestorRollupDexSize} bytes) via parent package mappings"
            )
        }
        if (autoAttributedDexCountByLob.isNotEmpty()) {
            val breakdown = autoAttributedDexCountByLob.keys.sorted().joinToString(", ") { lob ->
                "$lob=${autoAttributedDexCountByLob[lob] ?: 0} pkgs/" +
                        "${autoAttributedDexSizeByLob[lob] ?: 0L} bytes"
            }
            Printer.log(
                "LOB DEX fallback attribution: $breakdown"
            )
        }

        // DEX package model sizes can be larger than raw dex file bytes because package tree sizes
        // come from structural accounting that is not always byte-identical to on-disk dex entries.
        // To keep LOB reconciliation intuitive, cap package-attributed dex bytes to raw dex bytes by
        // proportional normalization when needed.
        val rawDexBytesBudget = skippedDexFileSize
        val packageDexBytesBeforeNormalization = fuDexSizes.values.sumOf { it } + unmatchedDexSize
        if (rawDexBytesBudget > 0 &&
            packageDexBytesBeforeNormalization > 0 &&
            packageDexBytesBeforeNormalization > rawDexBytesBudget
        ) {
            val normalizationFactor = rawDexBytesBudget.toDouble() / packageDexBytesBeforeNormalization.toDouble()
            Printer.log(
                "LOB DEX normalization applied: package bytes=$packageDexBytesBeforeNormalization, " +
                        "raw dex bytes=$rawDexBytesBudget, factor=$normalizationFactor"
            )

            // Scale FU-level dex attribution.
            fuDexSizes.keys.toList().forEach { fu ->
                fuDexSizes[fu] = (fuDexSizes[fu]!! * normalizationFactor).roundToLong()
            }

            // Scale unmatched dex aggregate and per-package details.
            unmatchedDexSize = (unmatchedDexSize * normalizationFactor).roundToLong()
            for (i in unmatchedDexDetailsList.indices) {
                val item = unmatchedDexDetailsList[i]
                unmatchedDexDetailsList[i] = item.copy(
                    sizeInBytes = (item.sizeInBytes * normalizationFactor).roundToLong()
                )
            }

            // Scale attributed per-LOB dex detail rows.
            attributedDexByLob.keys.toList().forEach { fu ->
                val scaledRows = attributedDexByLob[fu].orEmpty().map { detail ->
                    detail.copy(sizeInBytes = (detail.sizeInBytes * normalizationFactor).roundToLong())
                }
                attributedDexByLob[fu] = scaledRows.toMutableList()
            }
        }

        // --- Build unmatched details (sorted by size desc for easy review) ---
        unmatchedDetails = UnmatchedDetails(
            unmatchedFiles = unmatchedFileDetailsList.sortedByDescending { it.sizeInBytes },
            unmatchedDex = unmatchedDexDetailsList.sortedByDescending { it.sizeInBytes },
            summary = UnmatchedDetailsSummary(
                unmatchedFileCount = unmatchedFileCount,
                unmatchedFileTotalBytes = unmatchedFileSize,
                unmatchedDexCount = unmatchedDexCount,
                unmatchedDexTotalBytes = unmatchedDexSize,
            ),
        )

        // --- Build attributed details (per-LOB, sorted by size desc) ---
        val allLobNames = (attributedFilesByLob.keys + attributedDexByLob.keys).toSet()
        attributedDetails = AttributedDetails(
            lobDetails = allLobNames.associateWith { lob ->
                LobAttributedDetail(
                    files = (attributedFilesByLob[lob] ?: emptyList())
                        .sortedByDescending { it.sizeInBytes },
                    dexPackages = (attributedDexByLob[lob] ?: emptyList())
                        .sortedByDescending { it.sizeInBytes },
                )
            }
        )

        // --- Aggregate per FU (sorted by total size descending) ---
        val allFuNames = (fuFileSizes.keys + fuDexSizes.keys).toSet()
        val lobSizes = allFuNames.map { fu ->
            val fileSizes = fuFileSizes[fu]
            val code = fuDexSizes[fu] ?: 0L
            val resources = fileSizes?.get("resources") ?: 0L
            val assets = fileSizes?.get("assets") ?: 0L
            val nativeLibs = fileSizes?.get("nativeLibs") ?: 0L
            val other = fileSizes?.get("other") ?: 0L
            fu to LobSizeBreakdown(
                code = code,
                resources = resources,
                assets = assets,
                nativeLibs = nativeLibs,
                other = other,
                total = code + resources + assets + nativeLibs + other,
            )
        }.sortedByDescending { it.second.total }
            .associate { it.first to it.second }  // LinkedHashMap preserves insertion order

        // --- Compute aggregate total across all LOBs ---
        val allBreakdowns = lobSizes.values
        val totalBreakdown = LobSizeBreakdown(
            code = allBreakdowns.sumOf { it.code },
            resources = allBreakdowns.sumOf { it.resources },
            assets = allBreakdowns.sumOf { it.assets },
            nativeLibs = allBreakdowns.sumOf { it.nativeLibs },
            other = allBreakdowns.sumOf { it.other },
            total = allBreakdowns.sumOf { it.total },
        )

        val totalUnattributed = unmatchedFileSize + unmatchedDexSize
        val totalAll = totalBreakdown.total + totalUnattributed
        val coveragePercent = if (totalAll > 0) {
            ApkSizeHelpers.roundOffDecimal(totalBreakdown.total.toDouble() / totalAll * 100.0) ?: 0.0
        } else {
            0.0
        }

        // DEX overhead: raw .dex file bytes not accounted for by package-level analysis
        // (DEX headers, string/type/proto pools, etc.)
        val totalDexPackageBytes = totalBreakdown.code + unmatchedDexSize
        val dexOverhead = (skippedDexFileSize - totalDexPackageBytes).coerceAtLeast(0)

        // --- Build DEX overhead details ---
        val overheadPercent = if (skippedDexFileSize > 0) {
            ApkSizeHelpers.roundOffDecimal(dexOverhead.toDouble() / skippedDexFileSize * 100.0) ?: 0.0
        } else 0.0
        dexOverheadDetails = DexOverheadDetails(
            dexFiles = dexFileEntries.sortedByDescending { it.sizeInBytes },
            totalRawDexBytes = skippedDexFileSize,
            totalPackageAnalyzedBytes = totalDexPackageBytes,
            overheadBytes = dexOverhead,
            overheadPercent = overheadPercent,
        )

        Printer.log("LOB analysis: ${lobSizes.size} FUs, " +
                "attributed ${totalBreakdown.total} bytes (${coveragePercent}%), " +
                "unmatched files=$unmatchedFileCount, unmatched dex=$unmatchedDexCount, " +
                "dex overhead=${dexOverhead} bytes (${overheadPercent}% of raw dex)")

        return LobAnalysisResult(
            lobSizes = lobSizes,
            total = totalBreakdown,
            unmatchedFiles = UnmatchedInfo(unmatchedFileCount, unmatchedFileSize),
            unmatchedDex = UnmatchedInfo(unmatchedDexCount, unmatchedDexSize),
            summary = LobSummary(
                totalAttributedBytes = totalBreakdown.total,
                totalUnattributedBytes = totalUnattributed,
                coveragePercent = coveragePercent,
                dexFileBytes = skippedDexFileSize,
                dexOverheadBytes = dexOverhead,
                dexOverheadPercentOfDex = overheadPercent,
            ),
        )
    }

    /**
     * Try to match a file name against the resource mapping.
     * For APK mode, paths lack the `base/` prefix so we try prepending it.
     */
    private fun resolveFileMapping(name: String): List<Int>? {
        val candidates = linkedSetOf<String>()
        candidates += name
        candidates += normalizeCompiledResourcePath(name)
        // AAPT2 may split <animated-vector> drawables into synthetic frame files like
        // "$avd_hide_password__0.xml". Remap those to the parent drawable key.
        val parentDrawableName = normalizeAapt2AnimatedFramePath(name)
        candidates += parentDrawableName
        candidates += normalizeCompiledResourcePath(parentDrawableName)

        for (candidate in candidates) {
            val mapping = resolveFileMappingCandidate(candidate)
            if (mapping != null) return mapping
        }
        return null
    }

    /** Resolves direct and APK base-prefixed mapping candidates. */
    private fun resolveFileMappingCandidate(path: String): List<Int>? {
        // Direct match (works for AAB paths like "base/res/drawable/icon.webp")
        resourceMapping[path]?.let { return it }
        if (!isAab) {
            // APK paths lack module prefix — try prepending "base/"
            resourceMapping["base/$path"]?.let { return it }
        }
        return null
    }

    /**
     * Convert compiled APK resource paths to source-style mapping keys by dropping implicit
     * API-level suffixes added by AAPT2 on directory qualifiers (e.g. `-v4`, `-v8`, `-v13`).
     */
    private fun normalizeCompiledResourcePath(path: String): String {
        return COMPILED_RESOURCE_VERSION_SUFFIX.replace(path, "")
    }

    /**
     * Convert AAPT2 synthetic animated drawable frame file names back to parent drawable names.
     * Example: `$avd_hide_password__0.xml` -> `avd_hide_password.xml`.
     */
    private fun normalizeAapt2AnimatedFramePath(path: String): String {
        val fileName = path.substringAfterLast('/')
        if (!AAPT2_ANIMATED_FRAME_FILE_NAME.matches(fileName)) return path

        val parentFileName = fileName
            .substring(1) // strip '$'
            .replace(AAPT2_ANIMATED_FRAME_SUFFIX, ".xml")
        val parentDir = path.substringBeforeLast('/', "")
        return if (parentDir.isEmpty()) parentFileName else "$parentDir/$parentFileName"
    }

    /** Files intentionally excluded from LOB attribution. */
    private fun shouldIgnoreFileForLob(fileName: String): Boolean {
        return fileName in IGNORED_LOB_FILES
    }

    /**
     * Fallback attribution for unmatched non-resource files. This handles known library metadata
     * and package-like paths that are not present in resource-mapping.json.
     */
    private fun resolveFallbackLobForFile(fileName: String, category: String): String? {
        if (
            fileName in ANDROID_PLATFORM_FILE_EXACT_NAMES ||
            ANDROID_PLATFORM_FILE_PREFIXES.any { fileName.startsWith(it) } ||
            BUNDLE_SPLITS_CONFIG_PATH.matches(fileName) ||
            fileName.endsWith(".kotlin_builtins")
        ) {
            return ANDROID_PLATFORM_LOB
        }

        if (category != "other") return null
        if (isAppOwnedFilePath(fileName)) return null

        if (
            THIRD_PARTY_FILE_PREFIXES.any { fileName.startsWith(it) } ||
            THIRD_PARTY_FILE_SUFFIXES.any { fileName.endsWith(it) }
        ) {
            return THIRD_PARTY_LOB
        }

        return null
    }

    /**
     * Fallback attribution for packages that are known platform/runtime namespaces but may not
     * exist in package-mapping.json. This keeps them out of unmatched dex.
     */
    private fun resolveFallbackLobForDexPackage(packageName: String): String? {
        if (
            ANDROID_PLATFORM_PACKAGE_ROOTS.any { root ->
                packageName == root || packageName.startsWith("$root.")
            }
        ) {
            return ANDROID_PLATFORM_LOB
        }

        // If app package prefixes are configured, treat everything outside app-owned namespaces
        // as third-party code instead of unmatched.
        if (appPackagePrefixes.isNotEmpty() && !isAppOwnedPackage(packageName)) {
            return THIRD_PARTY_LOB
        }
        return null
    }

    /** Checks whether a package belongs to app-owned namespaces from analyzer config. */
    private fun isAppOwnedPackage(packageName: String): Boolean {
        return appPackagePrefixes.any { root ->
            packageName == root || packageName.startsWith("$root.")
        }
    }

    /** Checks whether a package-like file path belongs to app-owned namespaces. */
    private fun isAppOwnedFilePath(fileName: String): Boolean {
        return appPackagePathPrefixes.any { root ->
            fileName == root || fileName.startsWith("$root/")
        }
    }

    /**
     * Resolve package mapping key by walking up the ancestor chain until a mapping is found.
     * Example: `a.b.c.d` -> try `a.b.c.d`, `a.b.c`, `a.b`, `a`.
     */
    private fun resolveMappedPackageKey(packageName: String): String? {
        var current = packageName
        while (true) {
            val entries = packageMapping[current]
            if (entries != null && entries.isNotEmpty()) return current
            val lastDot = current.lastIndexOf('.')
            if (lastDot < 0) return null
            current = current.substring(0, lastDot)
        }
    }

    /** Maps file type strings from the processors to LOB categories. */
    private fun categorize(fileType: String): String = when (fileType) {
        "resources", "compileRes" -> "resources"
        "assets" -> "assets"
        "staticLibs" -> "nativeLibs"
        else -> "other"
    }

    companion object {
        private const val METADATA_FILE = "module-metadata.json"
        private const val RESOURCE_MAPPING_FILE = "resource-mapping.json"
        private const val PACKAGE_MAPPING_FILE = "package-mapping.json"
        private const val ANDROID_PLATFORM_LOB = "android_platform"
        private const val THIRD_PARTY_LOB = "thirdparty"
        private val IGNORED_LOB_FILES = setOf("resources.arsc")
        private val ANDROID_PLATFORM_PACKAGE_ROOTS = listOf(
            "android",
            "androidx",
            "dagger",
            "java",
            "javax",
            "org.xml.sax",
            "com.google.api.client.http.javanet",
        )
        private val ANDROID_PLATFORM_FILE_PREFIXES = listOf(
            "android/",
            "androidx/",
            "dagger/",
            "java/",
            "javax/",
            "kotlin/",
            "kotlinx/",
            "META-INF/androidx/",
            "assets/dexopt/",
            "base/assets/dexopt/",
        )
        private val ANDROID_PLATFORM_FILE_EXACT_NAMES = setOf(
            "DebugProbesKt.bin",
            "AndroidManifest.xml",
            "assets/dexopt/baseline.prof",
            "assets/dexopt/baseline.profm",
            "base/assets/dexopt/baseline.prof",
            "base/assets/dexopt/baseline.profm",
            "kotlin-tooling-metadata.json",
            "androidsupportmultidexversion.txt",
        )
        private val BUNDLE_SPLITS_CONFIG_PATH = Regex("(^|.*/)res/xml/splits\\d+\\.xml$")
        private val AAPT2_ANIMATED_FRAME_FILE_NAME = Regex("^\\$[^/]+__\\d+\\.xml$")
        private val AAPT2_ANIMATED_FRAME_SUFFIX = Regex("__\\d+\\.xml$")
        private val THIRD_PARTY_FILE_PREFIXES = listOf(
            "META-INF/",
            "com/",
            "org/",
            "io/",
            "net/",
            "okhttp3/",
            "google/",
            "firebase/",
        )
        private val THIRD_PARTY_FILE_SUFFIXES = listOf(
            ".proto",
            ".properties",
            ".kotlin_module",
        )
        private val COMPILED_RESOURCE_VERSION_SUFFIX = Regex("-v\\d+(?=/)")

        /**
         * Load mapping files from a directory or zip and build a [LobContext].
         *
         * @param path Path to a directory containing the 3 JSON files, or a .zip file containing them.
         * @param isAab Whether the analysis is for an AAB (affects path normalization for file matching).
         * @param appPackagePrefixes App-owned package prefixes from analyzer config.
         */
        fun load(
            path: String,
            isAab: Boolean,
            appPackagePrefixes: List<String> = emptyList(),
        ): LobContext {
            Printer.log("Loading LOB mappings from: $path")
            val file = File(path)
            if (!file.exists()) {
                throw IllegalArgumentException("Module mappings path does not exist: $path")
            }

            val jsonContents: Map<String, String> = if (file.isDirectory) {
                loadFromDirectory(file)
            } else if (path.endsWith(".zip", ignoreCase = true)) {
                loadFromZip(file)
            } else {
                throw IllegalArgumentException(
                    "moduleMappingsPath must be a directory or .zip file: $path"
                )
            }

            val gson = Gson()

            // Parse module-metadata.json
            val metadataJson = jsonContents[METADATA_FILE]
                ?: throw IllegalArgumentException("$METADATA_FILE not found in mappings")
            val metadata = gson.fromJson(metadataJson, ModuleMetadata::class.java)
            Printer.log("Loaded metadata: ${metadata.modules.size} modules, " +
                    "${metadata.functionalUnits.size} functional units")

            // Parse resource-mapping.json
            val resourceJson = jsonContents[RESOURCE_MAPPING_FILE]
                ?: throw IllegalArgumentException("$RESOURCE_MAPPING_FILE not found in mappings")
            val resourceMappingType = object : TypeToken<Map<String, Any>>() {}.type
            val resourceWrapper: Map<String, Any> = gson.fromJson(resourceJson, resourceMappingType)
            @Suppress("UNCHECKED_CAST")
            val rawResourceMapping = resourceWrapper["resourceMapping"] as? Map<String, List<Double>>
                ?: emptyMap()
            // Convert Double lists to Int lists (Gson deserializes JSON numbers as Double in untyped maps)
            val resourceMapping = rawResourceMapping.mapValues { (_, v) ->
                (v as List<*>).map { (it as Number).toInt() }
            }
            Printer.log("Loaded resource mapping: ${resourceMapping.size} entries")

            // Parse optional fuIndex: maps negative indices directly to FU names
            @Suppress("UNCHECKED_CAST")
            val rawFuIndex = resourceWrapper["fuIndex"] as? Map<String, String> ?: emptyMap()
            val fuIndex = rawFuIndex.mapKeys { (k, _) -> k.toInt() }
            if (fuIndex.isNotEmpty()) {
                Printer.log("Loaded fuIndex: ${fuIndex.size} direct FU mappings")
            }

            // Parse package-mapping.json
            val packageJson = jsonContents[PACKAGE_MAPPING_FILE]
                ?: throw IllegalArgumentException("$PACKAGE_MAPPING_FILE not found in mappings")
            val packageWrapper: Map<String, Any> = gson.fromJson(packageJson, resourceMappingType)
            @Suppress("UNCHECKED_CAST")
            val rawPackageMapping = packageWrapper["packageMapping"] as? Map<String, List<List<Double>>>
                ?: emptyMap()
            val packageMapping = rawPackageMapping.mapValues { (_, v) ->
                (v as List<*>).map { pair ->
                    (pair as List<*>).map { (it as Number).toInt() }
                }
            }
            Printer.log("Loaded package mapping: ${packageMapping.size} entries")

            // Build module index -> FU name reverse lookup
            val moduleToFu = mutableMapOf<Int, String>()
            for ((fuName, indices) in metadata.functionalUnits) {
                for (idx in indices) {
                    moduleToFu[idx] = fuName
                }
            }

            val normalizedAppPackagePrefixes = appPackagePrefixes
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()

            return LobContext(
                resourceMapping = resourceMapping,
                packageMapping = packageMapping,
                moduleToFu = moduleToFu,
                fuIndex = fuIndex,
                isAab = isAab,
                appPackagePrefixes = normalizedAppPackagePrefixes,
            )
        }

        private fun loadFromDirectory(dir: File): Map<String, String> {
            val result = mutableMapOf<String, String>()
            for (name in listOf(METADATA_FILE, RESOURCE_MAPPING_FILE, PACKAGE_MAPPING_FILE)) {
                val f = File(dir, name)
                if (f.exists() && f.canRead()) {
                    result[name] = f.readText()
                }
            }
            return result
        }

        private fun loadFromZip(zipFile: File): Map<String, String> {
            val targetNames = setOf(METADATA_FILE, RESOURCE_MAPPING_FILE, PACKAGE_MAPPING_FILE)
            val result = mutableMapOf<String, String>()
            ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    // Match by file name (ignore directory structure in zip)
                    val simpleName = entry.name.substringAfterLast("/")
                    if (simpleName in targetNames) {
                        result[simpleName] = InputStreamReader(zis, Charsets.UTF_8).readText()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            return result
        }
    }
}
