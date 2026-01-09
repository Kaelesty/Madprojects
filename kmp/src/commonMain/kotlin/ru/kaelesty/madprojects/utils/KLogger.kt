package ru.kaelesty.madprojects.utils

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter

object KLogger {
    private val logger = Logger(
        config = loggerConfigInit(platformLogWriter())
    )

    fun d(tag: String, msg: () -> String) = logger.d("KLogger: ${msg()}", null, tag)
    fun i(tag: String, msg: () -> String) = logger.i("KLogger: ${msg()}", null, tag)
    fun w(tag: String, msg: () -> String) = logger.w("KLogger: ${msg()}", null, tag)
    fun e(tag: String, t: Throwable? = null, msg: () -> String) = logger.e("KLogger: ${msg()}", t, tag)
}