package com.koooge.common.activities

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.koooge.common.logger.Log
import com.koooge.common.logger.LogWrapper

open class SampleActivityBase : FragmentActivity() {
    companion object {
        const val TAG = "SampleActivityBase"
    }

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override protected fun onStart() {
        super.onStart()
        initializeLogging()
    }

    open fun initializeLogging() {
        val logWrapper = LogWrapper()
        Log.setLogNode(logWrapper)

        Log.i(TAG, "Ready")
    }
}