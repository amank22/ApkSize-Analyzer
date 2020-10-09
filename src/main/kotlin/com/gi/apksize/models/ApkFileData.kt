package com.gi.apksize.models

import com.gi.apksize.processors.ApkFileProcessor
import com.gi.apksize.utils.ApkSizeHelpers
import com.google.gson.annotations.Expose

data class ApkFileData(
    val name: String,
    val sizeInBytes: Long,
    val sizeInKb: Long,
    val fileType: ApkFileType,
    val simpleFileName: String,
)

data class ApkFileType(
    val fileType: String,
    var simpleFileName: String?,
    val fileSubType: String,
)

class ApkGroupSizes {
    var subGroups: HashMap<String, SizeModel>? = hashMapOf()
    var groupSize: Long? = null
    var groupSizeMb: Double? = null

    fun calculateGroupSize() {
        if (subGroups.isNullOrEmpty()) {
            groupSize = 0L
            return
        }
        if (subGroups?.size == 1) {
            val s = subGroups?.values?.elementAt(0)?.size ?: 0
            groupSize = s
            groupSizeMb = ApkSizeHelpers.roundOffDecimal(s / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
            subGroups = null
            return
        }
        var size = 0L
        subGroups?.forEach {
            size += it.value.size
        }
        groupSize = size
        groupSizeMb = ApkSizeHelpers.roundOffDecimal(size / ApkFileProcessor.BYTE_TO_MB_DIVIDER)
    }

}
