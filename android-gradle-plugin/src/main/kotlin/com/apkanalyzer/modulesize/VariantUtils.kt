package com.apkanalyzer.modulesize

/**
 * Utility for splitting camelCase Android build-variant names into
 * possible sub-paths that AGP may use for intermediates directories.
 *
 * e.g. "standardRelease" → ["standardRelease", "standard/release", "standardDebug", "standard/debug"]
 */
object VariantUtils {

    private val VARIANT_REGEX = Regex("^([a-z][a-zA-Z_]*?)([A-Z][a-zA-Z_]*)$")

    fun splitVariant(variant: String): List<String> {
        val results = mutableListOf(variant)
        val match = VARIANT_REGEX.matchEntire(variant)
        if (match != null) {
            val flavor = match.groupValues[1]
            val buildType = match.groupValues[2].replaceFirstChar { it.lowercase() }
            results.add("$flavor/$buildType")
            results.add("$flavor${buildType.replaceFirstChar { it.uppercase() }}")
            if (buildType == "release") {
                results.add("${flavor}Debug")
                results.add("$flavor/debug")
            } else if (buildType == "debug") {
                results.add("${flavor}Release")
                results.add("$flavor/release")
            }
        }
        return results.distinct()
    }
}
