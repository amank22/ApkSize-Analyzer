package com.gi.apksize.tasks

import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder

interface Task {

    fun process(dataHolder: DataHolder) : ApkStats

}