package com.apkanalyzer.modulesize

/**
 * Deserialization model for the React Native bundle analysis JSON.
 * Only the fields needed for LOB size integration are declared here;
 * unknown fields are silently ignored by Gson.
 */
internal data class RnBundleAnalysis(
    val bundleSizeBytes: Long = 0,
    val sourceAttributionBytes: Long = 0,
    val teamBreakdown: List<RnTeamEntry> = emptyList(),
    val assetsByTeam: List<RnAssetTeamEntry> = emptyList(),
)

internal data class RnTeamEntry(
    val team: String,
    val size: Long = 0,
    val assetSize: Long = 0,
)

internal data class RnAssetTeamEntry(
    val lob: String,
    val totalSize: Long = 0,
    val files: List<RnAssetFile> = emptyList(),
)

internal data class RnAssetFile(
    val relPath: String,
    val size: Long = 0,
)
