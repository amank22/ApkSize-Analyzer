package com.gi.apksize.processors

import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder

/**
 * A simple processor which don't need any extra data to process.
 * If you need input which is not included in [DataHolder], you can instead extend you class
 * directly with [Processor] and pass in extra holder you want with the additional data.
 *
 * For most cases, we can simply use this [SimpleProcessor]
 */
abstract class SimpleProcessor : Processor<Nothing>() {

    override fun process(dataHolder: DataHolder, apkStats: ApkStats, extraHolder: Nothing) {
        process(dataHolder, apkStats)
    }

    abstract fun process(dataHolder: DataHolder, apkStats: ApkStats)

}