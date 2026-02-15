package com.gi.apksize.models

/**
 * Represents analysis data for a single module in an Android App Bundle.
 * Each AAB contains at least a base module, and optionally feature/asset modules.
 */
data class BundleModuleInfo(
    /** Module name (e.g., "base", "feature_login", "dynamic_feature") */
    val moduleName: String,
    /** Raw delivery type key from bundletool (e.g., "always-initial-install") */
    val deliveryType: String,
    /** Human-readable delivery label (e.g., "Base Module - Always Installed") */
    val deliveryLabel: String,
    /** Short description of what the delivery type means */
    val deliveryDescription: String,
    /** Module type (e.g., "Feature", "Asset", "ML") */
    val moduleType: String,
    /** Total number of files in the module */
    val fileCount: Int,
    /** List of ABI architectures with native libs in this module (e.g., ["arm64-v8a", "armeabi-v7a"]) */
    val nativeAbis: List<String>,
    /** Percentage of total AAB size this module represents (0.0 - 100.0) */
    val sizePercentage: Double,
    /** Total size of all entries in the module (bytes) */
    val totalSize: Long,
    /** Total size of DEX files in the module (bytes) */
    val dexSize: Long,
    /** Total size of resources (res/ + resources.pb) in the module (bytes) */
    val resourcesSize: Long,
    /** Total size of assets in the module (bytes) */
    val assetsSize: Long,
    /** Total size of native libraries in the module (bytes) */
    val nativeLibsSize: Long,
    /** Total size of other files (manifest, .pb configs, etc.) in the module (bytes) */
    val otherSize: Long
)
