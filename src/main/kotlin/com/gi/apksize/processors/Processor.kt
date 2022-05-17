package com.gi.apksize.processors

import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder

/**
 * A processor that processes the data from the given input [AnalyzerOptions] and fill the data in [ApkStats].
 * TODO: it's not a good way to let processors directly change the apkStats. They should not change the values directly
 *
 */
abstract class Processor<T> {

    abstract val name: String

    abstract fun process(dataHolder: DataHolder, apkStats: ApkStats, extraHolder: T)

    open fun preMsg() = "Starting processing $name"

    open fun postMsg() = "Completed processing $name"

    open fun failedMsg(e : Throwable) = "Failed processing $name. ${e.message}"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Processor<*>) return false

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

}