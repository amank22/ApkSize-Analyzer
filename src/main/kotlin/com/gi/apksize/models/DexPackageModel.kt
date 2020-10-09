package com.gi.apksize.models

import com.google.gson.annotations.SerializedName

data class DexPackageModel(
    @SerializedName("pkg") val basePackage: String,
    @SerializedName("pkgSize") var basePackageSize: Long,
    @SerializedName("depth") val depth: Int,
    @SerializedName("pkgSizeKb") var packageSizeKb: Long? = null,
)