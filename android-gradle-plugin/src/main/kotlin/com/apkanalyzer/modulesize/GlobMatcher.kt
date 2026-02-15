package com.apkanalyzer.modulesize

/**
 * Simple glob matching utility (supports `*` wildcard).
 */
object GlobMatcher {

    /**
     * Match a glob pattern against a value.
     * Dots are escaped, `*` becomes `.*`.
     */
    fun matches(pattern: String, value: String): Boolean {
        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
        return value.matches(Regex(regex))
    }

    /**
     * Check whether a module ID should be included based on include/exclude patterns.
     * Normalises colons to dots so "com.mmt.*" matches both "com.mmt.react-native-libs"
     * and "com.mmt:mmt-hotels".
     */
    fun shouldInclude(
        moduleId: String,
        includePatterns: List<String>,
        excludePatterns: List<String>,
    ): Boolean {
        val normalizedId = moduleId.replace(':', '.')
        val included = includePatterns.any { matches(it, normalizedId) }
        if (!included) return false
        val excluded = excludePatterns.any { matches(it, normalizedId) }
        return !excluded
    }
}
