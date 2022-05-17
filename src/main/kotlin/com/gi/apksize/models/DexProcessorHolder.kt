package com.gi.apksize.models

/**
 * Holder for dex processing.
 * @param isCompareFile a boolean variable indicating this is the secondary file / comparison file
 */
data class DexProcessorHolder(val isCompareFile: Boolean = false,
                              val needAppPackages: Boolean = true)
