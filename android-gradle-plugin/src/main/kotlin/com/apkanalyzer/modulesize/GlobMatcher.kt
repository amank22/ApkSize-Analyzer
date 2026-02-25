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

}
