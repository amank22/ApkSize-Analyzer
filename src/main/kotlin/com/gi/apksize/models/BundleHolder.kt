package com.gi.apksize.models

import com.android.tools.build.bundletool.model.AppBundle
import java.io.Closeable
import java.util.zip.ZipFile

/**
 * Holds a parsed [AppBundle] and its underlying [ZipFile].
 * Created once in BundleStatsTask and shared across all bundle processors
 * to avoid re-parsing the AAB file.
 */
class BundleHolder(
    val appBundle: AppBundle,
    val zipFile: ZipFile
) : Closeable {
    override fun close() {
        zipFile.close()
    }
}
