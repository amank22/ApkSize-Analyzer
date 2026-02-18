package com.gi.apksize.models

/**
 * Result of LOB (functional unit) size analysis.
 * Breaks down APK/AAB size by functional units (LOBs) with per-category detail.
 */
data class LobAnalysisResult(
    /** Size breakdown per functional unit (LOB name -> breakdown) */
    val lobSizes: Map<String, LobSizeBreakdown>,
    /** Aggregate total across all LOBs */
    val total: LobSizeBreakdown,
    /** Info about files that could not be matched to any module */
    val unmatchedFiles: UnmatchedInfo,
    /** Info about DEX packages that could not be matched to any module */
    val unmatchedDex: UnmatchedInfo,
    /** Overall attribution summary */
    val summary: LobSummary,
)

/**
 * Size breakdown for a single functional unit across categories.
 */
data class LobSizeBreakdown(
    /** DEX / code size in bytes */
    val code: Long,
    /** Resource files (res/) size in bytes */
    val resources: Long,
    /** Asset files (assets/) size in bytes */
    val assets: Long,
    /** Native libraries (lib/) size in bytes */
    val nativeLibs: Long,
    /** Other matched files size in bytes */
    val other: Long,
    /** Total size in bytes (sum of all categories) */
    val total: Long,
)

/**
 * Summary of unmatched entries (files or DEX packages not found in mappings).
 */
data class UnmatchedInfo(
    val count: Int,
    val totalSizeBytes: Long,
)

/**
 * High-level summary of LOB analysis coverage.
 */
data class LobSummary(
    val totalAttributedBytes: Long,
    val totalUnattributedBytes: Long,
    val coveragePercent: Double,
    /** Raw .dex file bytes excluded from file analysis (handled by DEX processor instead) */
    val dexFileBytes: Long = 0,
    /** Bytes in .dex files not covered by package-level analysis (DEX headers, string pools, etc.) */
    val dexOverheadBytes: Long = 0,
    /** Share of raw DEX bytes that are structural overhead. */
    val dexOverheadPercentOfDex: Double = 0.0,
)

/**
 * Detailed info about a single unmatched file (not found in resource-mapping).
 *
 * @property originalName When AAPT2 resource path shortening is active, this holds the
 *   pre-shortening source path (e.g. `res/drawable-xhdpi/hotel_icon.webp`). Null when
 *   the file was not renamed or no shortening map is available.
 */
data class UnmatchedFileDetail(
    val name: String,
    val sizeInBytes: Long,
    val category: String,
    val originalName: String? = null,
)

/**
 * Detailed info about a single unmatched DEX package (not found in package-mapping).
 */
data class UnmatchedDexDetail(
    val packageName: String,
    val sizeInBytes: Long,
)

/**
 * Container for all unmatched detail lists — written to a separate file for review.
 */
data class UnmatchedDetails(
    val unmatchedFiles: List<UnmatchedFileDetail>,
    val unmatchedDex: List<UnmatchedDexDetail>,
    val summary: UnmatchedDetailsSummary,
)

data class UnmatchedDetailsSummary(
    val unmatchedFileCount: Int,
    val unmatchedFileTotalBytes: Long,
    val unmatchedDexCount: Int,
    val unmatchedDexTotalBytes: Long,
)

// ─── Attributed details (per-LOB breakdown for review) ───

/**
 * A single file attributed to a LOB.
 */
data class AttributedFileDetail(
    val name: String,
    val sizeInBytes: Long,
    val category: String,
)

/**
 * A single DEX package (or share of one) attributed to a LOB.
 */
data class AttributedDexDetail(
    val packageName: String,
    val sizeInBytes: Long,
)

/**
 * Detailed attribution for one LOB — files and DEX packages that contribute to its size.
 */
data class LobAttributedDetail(
    val files: List<AttributedFileDetail>,
    val dexPackages: List<AttributedDexDetail>,
)

/**
 * Container for per-LOB attributed details — written to a separate file for review.
 */
data class AttributedDetails(
    val lobDetails: Map<String, LobAttributedDetail>,
)

// ─── DEX overhead details ───

/**
 * Per-DEX-file breakdown showing raw size vs package-analyzed size.
 */
data class DexOverheadDetails(
    val dexFiles: List<DexFileEntry>,
    val totalRawDexBytes: Long,
    val totalPackageAnalyzedBytes: Long,
    val overheadBytes: Long,
    val overheadPercent: Double,
    val note: String = "Overhead = DEX structural data (string pools, type/proto/method/field ID tables, " +
            "annotations, debug info, headers) not attributable to individual packages.",
)

data class DexFileEntry(
    val name: String,
    val sizeInBytes: Long,
)
