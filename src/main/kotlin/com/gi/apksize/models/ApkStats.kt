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

    fun json(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

}