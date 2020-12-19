package com.gi.apksize.models

import com.google.gson.annotations.SerializedName

data class FileByFileSizeDiffModel(
    @SerializedName("name") val name: String,
    @SerializedName("oldSize") var oldSize: Long,
    @SerializedName("newSize") val newSize: Long,
    @SerializedName("sizeDiff") val sizeDiff: Long,
    @SerializedName("sizeDiffKb") var sizeDiffKb: Long,
    @SerializedName("path") var path: String,
)