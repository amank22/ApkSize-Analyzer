package com.gi.apksize.models

/**
 * Estimated download/install size for a specific device configuration.
 * Produced by generating split APKs from the AAB via bundletool's BuildApksCommand,
 * then measuring per-component compressed sizes using ApkBreakdownGenerator.
 *
 * These numbers approximate what a user would download from the Play Store.
 */
data class EstimatedDeviceSize(
    /** Human-readable config name, e.g. "Low-end", "Mid-end", "High-end" */
    val configName: String,
    /** ABI used for this estimate, e.g. "armeabi-v7a", "arm64-v8a" */
    val abi: String,
    /** Screen density in DPI, e.g. 240 (hdpi), 480 (xxhdpi), 640 (xxxhdpi) */
    val screenDensityDpi: Int,
    /** Android SDK version, e.g. 24, 30, 34 */
    val sdkVersion: Int,
    /** Total estimated download size in bytes (GZip compressed) */
    val totalDownloadBytes: Long,
    /** Total on-disk size in bytes (after install) */
    val totalDiskBytes: Long,
    /** DEX component download size in bytes */
    val dexDownloadBytes: Long,
    /** Resources component download size in bytes */
    val resourcesDownloadBytes: Long,
    /** Assets component download size in bytes */
    val assetsDownloadBytes: Long,
    /** Native libraries component download size in bytes */
    val nativeLibsDownloadBytes: Long,
    /** Other files component download size in bytes */
    val otherDownloadBytes: Long
)
