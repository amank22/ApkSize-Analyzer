package com.gi.apksize.models

/**
 * Represents a single Maven dependency found in the AAB's
 * BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb.
 */
data class BundleDependencyInfo(
    /** Maven group ID (e.g., "androidx.core") */
    val groupId: String,
    /** Maven artifact ID (e.g., "core-ktx") */
    val artifactId: String,
    /** Version string (e.g., "1.12.0") */
    val version: String,
    /** One of [CATEGORY_APP], [CATEGORY_PLATFORM], or [CATEGORY_THIRD_PARTY]. */
    val category: String = CATEGORY_THIRD_PARTY
) {
    companion object {
        const val CATEGORY_APP = "app"
        const val CATEGORY_PLATFORM = "platform"
        const val CATEGORY_THIRD_PARTY = "third_party"

        val PLATFORM_PREFIXES = listOf("androidx.", "com.google.", "com.android.", "org.jetbrains.", "android.arch.")

        /**
         * Assigns a category to each dependency based on groupId prefix matching.
         * App prefixes take priority over platform prefixes.
         */
        fun categorize(
            deps: List<BundleDependencyInfo>,
            appPrefixes: List<String>
        ): List<BundleDependencyInfo> = deps.map { dep ->
            val category = when {
                appPrefixes.isNotEmpty() &&
                        appPrefixes.any { dep.groupId.startsWith(it) } -> CATEGORY_APP
                PLATFORM_PREFIXES.any { dep.groupId.startsWith(it) } -> CATEGORY_PLATFORM
                else -> CATEGORY_THIRD_PARTY
            }
            dep.copy(category = category)
        }
    }
}
