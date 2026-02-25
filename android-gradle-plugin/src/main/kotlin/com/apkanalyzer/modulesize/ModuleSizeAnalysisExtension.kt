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

    // ─── React Native bundle analysis integration ────────────────────────

    /**
     * Path to the React Native bundle analysis JSON file (produced by the RN bundle analyzer).
     * When set, the plugin reads per-team breakdowns from this file and:
     *   - Re-attributes RN image resources to individual team LOBs (via [assetsByTeam])
     *   - Generates proportional split data for `index.android.bundle` (via [teamBreakdown])
     *
     * When null or file doesn't exist, all RN artifacts stay under the existing "reactnative" FU.
     */
    var rnBundleAnalysisFile: File? = null

    /**
     * Optional explicit overrides mapping RN team names to FU names.
     * Applied before auto-normalization. Useful for edge cases where the RN team name
     * doesn't normalize cleanly to a matching FU name.
     *
     * Example:
     * ```
     * rnTeamToFuOverrides = mapOf(
     *     "Payment Team" to "payments",
     *     "Where2Go/Hubble" to "hubble",
     * )
     * ```
     */
    var rnTeamToFuOverrides: Map<String, String> = emptyMap()

    /** Output file path for the proportional splits JSON (RN bundle → per-team split ratios). */
    var proportionalSplitsFile: File? = null
}
