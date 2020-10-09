package com.gi.apksize.utils

import java.math.RoundingMode
import java.text.DecimalFormat

object ApkSizeHelpers {

    fun roundOffDecimal(number: Double): Double? {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number).toDoubleOrNull()
    }

}