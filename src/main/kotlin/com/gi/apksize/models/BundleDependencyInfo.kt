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
    val version: String
)
