package com.koooge.bluetoothchatkotlin

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ViewAnimator
import com.koooge.common.activities.SampleActivityBase
import com.koooge.common.logger.Log
import com.koooge.common.logger.LogFragment
import com.koooge.common.logger.LogWrapper
import com.koooge.common.logger.MessageOnlyLogFilter

class MainActivity : SampleActivityBase() {
    companion object {
        const val TAG = "mainActivity"
    }

    private var mLogShown: Boolean = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val transaction = getSupportFragmentManager().beginTransaction()
            val fragment = BluetoothChatFragment()
            transaction.replace(R.id.sample_content_fragment, fragment)
            transaction.commit();
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val logToggle = menu.findItem(R.id.menu_toggle_log)
        logToggle.setVisible(findViewById<View>(R.id.sample_output) is ViewAnimator)
        logToggle.setTitle(if (mLogShown) R.string.sample_hide_log else R.string.sample_show_log)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.getItemId()) {
            R.id.menu_toggle_log -> {
                mLogShown = !mLogShown
                val output: ViewAnimator = findViewById(R.id.sample_output)
                if (mLogShown) {
                    output.setDisplayedChild(1)
                } else {
                    output.setDisplayedChild(0)
                }
                supportInvalidateOptionsMenu()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun initializeLogging() {
        val logWrapper = LogWrapper()
        Log.setLogNode(logWrapper)

        val msgFilter = MessageOnlyLogFilter()
        logWrapper.setNext(msgFilter)

        val logFragment: LogFragment = getSupportFragmentManager().findFragmentById(R.id.log_fragment) as LogFragment
        msgFilter.setNext(logFragment.getLogView()!!)

        Log.i(this::class.java.simpleName, "Ready")
    }
}
