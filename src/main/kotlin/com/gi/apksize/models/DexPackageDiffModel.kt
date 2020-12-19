package com.gi.apksize.models

import com.google.gson.annotations.SerializedName

data class DexPackageDiffModel(
    @SerializedName("pkg") val basePackage: String,
    @SerializedName("oldPkgSize") var oldPackageSize: Long,
    @SerializedName("oldPkgSizeKb") var oldPackageSizeKb: Long? = null,
    @SerializedName("newPkgSize") var newPackageSize: Long,
    @SerializedName("newPkgSizeKb") var newPackageSizeKb: Long? = null,
    @SerializedName("depth") val depth: Int,
    @SerializedName("pkgSizeDiff") var packageSizeDiff: Long,
    @SerializedName("pkgSizeDiffKb") var packageSizeDiffKb: Long? = null,
)