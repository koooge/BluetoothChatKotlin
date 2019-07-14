package com.koooge.common.logger

import android.util.Log

class LogWrapper : LogNode {
    private var mNext: LogNode? = null

    fun getNext(): LogNode? {
        return mNext
    }

    fun setNext(node: LogNode) {
        mNext = node
    }

    override fun println(priority: Int, tag: String, msg: String?, tr: Throwable?) {
        val useMsg = if (msg == null) "" else msg
        val mMsg: String = if (tr != null) useMsg + "\n" + Log.getStackTraceString(tr) else useMsg

        Log.println(priority, tag, useMsg)

        mNext?.println(priority, tag, mMsg, tr)
    }
}