package com.gi.apksize.models

import com.android.tools.apk.analyzer.dex.DexFileStats
import com.google.gson.Gson

class ApkStats {
    var apkName: String? = null
    var apkSize: Long? = null
    var reactBundleSize: Long? = null
    var downloadSize: Long? = null
    var apkSizeInMb: Double? = null
    var reactBundleSizeInMb: Double? = null
    var downloadSizeInMb: Double? = null
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

    fun json(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

}