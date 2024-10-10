@file:Suppress("NOTHING_TO_INLINE")

package com.gi.apksize.utils

object Printer {

    inline fun log(message: Any?) {
        println(message)
    }

    inline fun error(message: Any?) {
        kotlin.error(message?:"")
    }

    inline fun log(throwable: Throwable?) {
        println(throwable)
    }

}