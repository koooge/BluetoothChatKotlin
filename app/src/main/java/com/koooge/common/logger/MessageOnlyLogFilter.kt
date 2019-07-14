package com.koooge.common.logger

class MessageOnlyLogFilter : LogNode {
    var mNext: LogNode? = null

    constructor(next: LogNode) {
        mNext = next
    }

    constructor() {}

    override fun println(priority: Int, tag: String?, msg: String?, tr: Throwable?) {
        if (mNext != null) {
            getNext()?.println(Log.NONE, null, msg, null)
        }
    }

    fun getNext(): LogNode? {
        return mNext
    }

    fun setNext(node: LogNode) {
        mNext = node
    }
}