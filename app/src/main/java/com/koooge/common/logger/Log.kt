package com.koooge.common.logger

class Log {
    companion object {
        const val NONE = -1
        const val VERBOSE = android.util.Log.VERBOSE
        const val DEBUG = android.util.Log.DEBUG
        const val INFO = android.util.Log.INFO
        const val WARN = android.util.Log.WARN
        const val ERROR = android.util.Log.ERROR
        const val ASSERT = android.util.Log.ASSERT

        private var mLogNode: LogNode? = null

        fun setLogNode(node: LogNode) {
            mLogNode = node
        }

        fun println(priority: Int, tag: String, msg: String?, tr: Throwable?) {
            val mMsg = if (msg == null) "" else msg
            if (mLogNode != null) {
                mLogNode!!.println(priority, tag, mMsg, tr)
            } else {
                System.out.println(tag + ": " + mMsg)
            }
        }

        fun println(priority: Int, tag: String, msg: String?) {
            println(priority, tag, msg, null)
        }

        fun v(tag: String, msg: String, tr: Throwable?) {
            println(VERBOSE, tag, msg, tr)
        }

        fun v(tag: String, msg: String) {
            v(tag, msg, null)
        }

        fun d(tag: String, msg: String, tr: Throwable?) {
            println(DEBUG, tag, msg, tr)
        }

        fun d(tag: String, msg: String) {
            d(tag, msg, null)
        }

        fun i(tag: String, msg: String, tr: Throwable?) {
            println(INFO, tag, msg, tr)
        }

        fun i(tag: String, msg: String) {
            i(tag, msg, null)
        }

        fun w(tag: String, msg: String?, tr: Throwable?) {
            println(WARN, tag, msg, tr)
        }

        fun w(tag: String, msg: String) {
            w(tag, msg, null)
        }

        fun w(tag: String, tr: Throwable?) {
            w(tag, null, tr)
        }

        fun e(tag: String, msg: String, tr: Throwable?) {
            println(ERROR, tag, msg, tr)
        }

        fun e(tag: String, msg: String) {
            v(tag, msg, null)
        }

        fun wtf(tag: String, msg: String?, tr: Throwable?) {
            println(ASSERT, tag, msg, tr)
        }

        fun wtf(tag: String, msg: String) {
            wtf(tag, msg, null)
        }

        fun wtf(tag: String,tr: Throwable) {
            wtf(tag, null, tr)
        }
    }

}