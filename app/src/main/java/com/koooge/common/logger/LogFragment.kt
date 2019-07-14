package com.koooge.common.logger

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView

class LogFragment() : Fragment() {
    private var mLogView: LogView? = null
    private var mScrollView: ScrollView? = null

    fun inflateViews(): View? {
        mScrollView = ScrollView(getActivity())
        val scrollParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT)
        mScrollView?.setLayoutParams(scrollParams)

        val mLogView = LogView(getActivity() as Context)
        val logParams = ViewGroup.LayoutParams(scrollParams)
        logParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        mLogView.setLayoutParams(logParams)
        mLogView.setClickable(true)
        mLogView.setFocusable(true)
        mLogView.setTypeface(Typeface.MONOSPACE)

        val paddingDips = 16
        val scale = getResources().getDisplayMetrics().density
        val paddingPixels: Int = ((paddingDips * scale) + .5) as Int
        mLogView.setPadding(paddingPixels, paddingPixels, paddingPixels, paddingPixels)
        mLogView.setCompoundDrawablePadding(paddingPixels)

        mLogView.setGravity(Gravity.BOTTOM)
        mLogView.setTextAppearance(getActivity(), android.R.style.TextAppearance_Holo_Medium)

        mScrollView?.addView(mLogView)
        return mScrollView
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val result: View? = inflateViews()

        mLogView?.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                mScrollView?.fullScroll(ScrollView.FOCUS_DOWN)
            }
        })
        return result
    }

    fun getLogView(): LogView? {
        return mLogView
    }
}