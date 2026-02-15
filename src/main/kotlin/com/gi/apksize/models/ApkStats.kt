package com.gi.apksize.models

import com.android.tools.apk.analyzer.dex.DexFileStats
import com.google.gson.Gson

class ApkStats {
    var apkName: String? = null
    var apkSize: Long? = null
    var downloadSize: Long? = null
    var apkSizeInMb: Double? = null
    var downloadSizeInMb: Double? = null

    var compareApkSize: Long? = null
    var compareDownloadSize: Long? = null
    var compareApkSizeInMb: Double? = null
    var compareDownloadSizeInMb: Double? = null


    var diffApkSize: Long? = null
    var diffDownloadSize: Long? = null
    var diffApkSizeInMb: Double? = null
    var diffDownloadSizeInMb: Double? = null

    var reactBundleSize: Long? = null
    var reactBundleSizeInMb: Double? = null
    var fileStats: HashMap<String, ArrayList<ApkFileData>>? = null
    var groupSizes: HashMap<String, ApkGroupSizes>? = null
    var topFiles : List<ApkFileData>? = null
    var topFilteredFiles : List<ApkFileData>? = null
    var topImages : List<ApkFileData>? = null
    var dexStats : DexFileStats? = null
    var dexPackages : List<DexPackageModel>? = null
    var appPackages : List<DexPackageModel>? = null
    var aaptData : String? = null
    var resourcesMap : HashMap<String, Long>? = null
    var fileDiffs : List<FileByFileSizeDiffModel>? = null
    var dexPackagesDiffs : List<DexPackageDiffModel>? = null
    var comparedDexStats : DexFileStats? = null
    var comparedDexPackages : List<DexPackageModel>? = null
    var comparedAppPackages : List<DexPackageModel>? = null
    /** Overall file-category totals for the analyzed APK/AAB (derived from full file scan). */
    var artifactSizeBreakdown: ArtifactSizeBreakdown? = null

    // region AAB-specific fields
    /** Type of input file analyzed (APK or AAB) */
    var inputFileType: InputFileType? = null
    /**
     * True when AAB analysis is executed on a generated install-time APK
     * (base + install-time dynamic modules) for primary size/file/dex metrics.
     */
    var isInstallTimeApkAnalysis: Boolean? = null
    /** Per-module breakdown for AAB analysis */
    var bundleModules: List<BundleModuleInfo>? = null
    /** Maven dependencies extracted from AAB metadata */
    var bundleDependencies: List<BundleDependencyInfo>? = null
    /** Group ID prefixes that identify app's own modules (from config) */
    var appModulePrefixes: List<String> = emptyList()
    /** Bundle configuration from BundleConfig.pb */
    var bundleConfig: BundleConfigInfo? = null
    /** Package name from AndroidManifest.xml */
    var manifestPackageName: String? = null
    /** Version code from AndroidManifest.xml */
    var manifestVersionCode: Int? = null
    /** Version name from AndroidManifest.xml */
    var manifestVersionName: String? = null
    /** Minimum SDK version from AndroidManifest.xml */
    var manifestMinSdk: Int? = null
    /** Target SDK version from AndroidManifest.xml */
    var manifestTargetSdk: Int? = null
    /** Estimated download sizes for different device configurations (low/mid/high-end) */
    var estimatedDeviceSizes: List<EstimatedDeviceSize>? = null
    // endregion

    // region LOB Analysis
    /** LOB (functional unit) size analysis result. Transient — written to separate lob-analysis.json. */
    @Transient
    var lobAnalysis: LobAnalysisResult? = null

    /** Detailed unmatched entries for LOB analysis. Transient — written to separate file. */
    @Transient
    var unmatchedDetails: UnmatchedDetails? = null

    /** Per-LOB attributed file/dex details. Transient — written to separate file. */
    @Transient
    var attributedDetails: AttributedDetails? = null

    /** DEX overhead breakdown. Transient — written to separate file. */
    @Transient
    var dexOverheadDetails: DexOverheadDetails? = null
    // endregion

    fun json(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

}

data class ArtifactSizeBreakdown(
    val dex: Long,
    val resources: Long,
    val assets: Long,
    val nativeLibs: Long,
    val other: Long,
    val total: Long,
)