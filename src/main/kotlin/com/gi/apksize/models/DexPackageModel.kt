package com.gi.apksize.models

import com.google.gson.annotations.SerializedName

data class DexPackageModel constructor(
    @SerializedName("pkg") val basePackage: String,
    @SerializedName("pkgSize") var basePackageSize: Long,
    @SerializedName("depth") val depth: Int,
    @SerializedName("pkgSizeKb") var packageSizeKb: Long? = null,
)

data class DexPackageTreeModel constructor(
    @SerializedName("pkg") val basePackage: String,
    @SerializedName("pkgSize") var basePackageSize: Long,
    @SerializedName("depth") val depth: Int,
    @SerializedName("pkgSizeKb") var packageSizeKb: Long? = null,
    @SerializedName("children") var children: HashSet<DexPackageTreeModel>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DexPackageTreeModel

        return basePackage == other.basePackage
    }

    override fun hashCode(): Int {
        return basePackage.hashCode()
    }
}