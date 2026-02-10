package com.gi.apksize.models

/**
 * Represents configuration data from BundleConfig.pb inside an AAB file.
 * Contains build-time settings like compression, split dimensions, and optimization flags.
 */
data class BundleConfigInfo(
    /** Version of bundletool used to build the bundle */
    val bundletoolVersion: String,
    /** Glob patterns for files left uncompressed in generated APKs */
    val compressionUncompressedGlobs: List<String>,
    /** Active split dimensions (e.g., "ABI", "SCREEN_DENSITY", "LANGUAGE") */
    val splitDimensions: List<String>,
    /** Whether native libraries are left uncompressed in APKs */
    val uncompressNativeLibs: Boolean,
    /** Whether DEX files are left uncompressed in APKs */
    val uncompressDex: Boolean
)
