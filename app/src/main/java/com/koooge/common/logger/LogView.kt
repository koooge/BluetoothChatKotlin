package com.koooge.common.logger

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

class LogView : TextView, LogNode {
    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defstyle: Int) : super(context, attrs, defstyle)

    override fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        var priorityStr: String? = null

        when (priority) {

            android.util.Log.VERBOSE -> {
                priorityStr = "VERBOSE"
            }
            android.util.Log.DEBUG -> {
                priorityStr = "DEBUG"
            }
            android.util.Log.INFO -> {
                priorityStr = "INFO"
            }
            android.util.Log.WARN -> {
                priorityStr = "WARN"
            }
            android.util.Log.ERROR -> {
                priorityStr = "ERROR"
            }
            android.util.Log.ASSERT -> {
                priorityStr = "ASSERT"
            }
        }

        var exceptionStr: String? = null
        if (tr != null) {
            exceptionStr = android.util.Log.getStackTraceString(tr)
        }

        val outputBuilder: StringBuilder = StringBuilder()

        val delimiter = "\t"
        appendIfNotNull(outputBuilder, priorityStr, delimiter)
        appendIfNotNull(outputBuilder, tag, delimiter)
        appendIfNotNull(outputBuilder, msg, delimiter)
        appendIfNotNull(outputBuilder, exceptionStr, delimiter)

        (getContext() as Activity).runOnUiThread((Thread(Runnable() {
            fun run() {
                appendToLog(outputBuilder.toString())
            }
        })))

        mNext?.println(priority, tag, msg, tr)
    }

    fun getNext(): LogNode? {
        return mNext
    }

    fun setNext(node: LogNode) {
        mNext = node
    }

    private fun appendIfNotNull(source: StringBuilder, addStr: String?, delimiter: String): StringBuilder {
        if (addStr == null) return source

        var mDelimiter: String = delimiter
        if (addStr?.length == 0) {
            mDelimiter = ""
        }

        return source.append(addStr).append(mDelimiter)
    }

    var mNext: LogNode? = null

    fun appendToLog(s: String) {
        append("\n" + s)
    }
}