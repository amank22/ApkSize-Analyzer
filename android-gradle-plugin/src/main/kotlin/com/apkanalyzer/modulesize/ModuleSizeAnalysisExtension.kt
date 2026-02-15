package com.apkanalyzer.modulesize

import java.io.File

/**
 * DSL extension for configuring module size analysis.
 *
 * ```kotlin
 * moduleSizeAnalysis {
 *     variant = "standardRelease"
 *     packageDepth = 4
 *     functionalUnitMapping = mapOf(
 *         "hotels"  to listOf("com.mmt:mmt-hotels*", ":mmt-home*"),
 *         "flights" to listOf("com.mmt:mmt-flights*"),
 *     )
 * }
 * ```
 */
open class ModuleSizeAnalysisExtension {

    /** Build variant to analyze (e.g. "release", "standardRelease"). */
    var variant: String = "release"

    /**
     * Glob patterns for modules to include.
     * Matched against "group:artifact" (remote) or ":project-path" (local).
     * Use "*" as wildcard. Default: include everything.
     */
    var includePatterns: List<String> = listOf("*")

    /**
     * Glob patterns for modules to exclude.
     * Applied after [includePatterns].
     */
    var excludePatterns: List<String> = emptyList()

    /**
     * Whether to include local project modules (dependencies declared as `project(":xyz")`).
     * When false, only remote (Maven/AAR/JAR) dependencies are analyzed.
     */
    var includeLocalModules: Boolean = true

    /**
     * How many levels deep to go when grouping classes by package.
     * e.g. depth=3 → com.example.lib, depth=4 → com.example.lib.utils
     */
    var packageDepth: Int = 3

    /** Output file path for the JSON report. */
    var outputFile: File? = null

    /** Output file path for the resource mapping JSON. */
    var resourceMappingFile: File? = null

    /**
     * Output file path for the shared metadata JSON (modules list, FU mapping, etc.).
     * Both module-analysis and resource-mapping reports reference this file.
     */
    var metadataFile: File? = null

    /** The target app module path (e.g. ":mobile", ":app"). Auto-detected if null. */
    var appModule: String? = null

    /** Enable/disable the module analysis step (resources, classes, native libs per module). */
    var enableModuleAnalysis: Boolean = true

    /** Enable/disable the resource mapping step (filePath → module mapping). */
    var enableResourceMapping: Boolean = true

    /**
     * Mapping of functional unit name to list of glob patterns.
     * Modules matching a pattern are assigned to that functional unit.
     * Modules not matching any pattern are auto-assigned to "android_platform"
     * (androidx, com.google, etc.) or "thirdparty" (everything else).
     *
     * Example:
     * ```
     * functionalUnitMapping = mapOf(
     *     "hotels"   to listOf("com.mmt:mmt-hotels*", ":mmt-home*"),
     *     "flights"  to listOf("com.mmt:mmt-flights*", "com.mmt:mmt-skywalker*"),
     *     "payments" to listOf("com.gommt:pay*", ":mmt-payments"),
     * )
     * ```
     */
    var functionalUnitMapping: Map<String, List<String>> = emptyMap()

    /** Output file path for the package mapping JSON (package → modules reverse index). */
    var packageMappingFile: File? = null

    /**
     * Minimum package depth to include in packageMapping.
     * Filters out shallow/noise packages (obfuscated single-segment names, etc.).
     * Default: 3 (skips "com.mmt" level, keeps "com.mmt.hotel").
     */
    var minPackageDepth: Int = 3

    /**
     * Manual overrides for specific package prefixes in packageMapping.
     * Value can be "ignore" to exclude, or an FU name to force-assign.
     *
     * Example:
     * ```
     * packageOverrides = mapOf(
     *     "hilt_aggregated_deps" to "ignore",
     *     "(default)"            to "ignore",
     * )
     * ```
     */
    var packageOverrides: Map<String, String> = emptyMap()

    /**
     * File-path-based FU overrides for resource mapping.
     * Re-attributes specific files to an FU directly, bypassing module-level FU assignment.
     *
     * Key = FU name, Value = list of glob patterns matched against resource-mapping file paths.
     */
    var resourceFUOverrides: Map<String, List<String>> = emptyMap()

    /**
     * Directory-based FU overrides for resource mapping.
     * All resource files found in these directories are attributed directly to the FU.
     * Paths are relative to the app module's project dir.
     *
     * Key = FU name, Value = list of directory paths (relative to app module project dir).
     */
    var resourceDirFUOverrides: Map<String, List<String>> = emptyMap()
}
