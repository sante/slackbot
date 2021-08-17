package com.xmartlabs.slackbot.extensions

import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.toPrettyString(): String {
    val sw = StringWriter()
    printStackTrace(PrintWriter(sw))
    return sw.toString()
}
