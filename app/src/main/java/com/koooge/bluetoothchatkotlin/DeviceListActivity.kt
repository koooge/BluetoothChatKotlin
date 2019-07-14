package com.koooge.bluetoothchatkotlin

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.*
import com.koooge.common.logger.Log

class DeviceListActivity : Activity() {
    companion object {
        const val TAG = "DeviceListActivity"

        var EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private var mBtAdapter: BluetoothAdapter? = null

    private var mNewDevicesArrayAdapter: ArrayAdapter<String>? = null

    override protected fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        setContentView(R.layout.activity_device_list)

        setResult(Activity.RESULT_CANCELED)

        val scanButton: Button =  findViewById(R.id.button_scan)
        scanButton.setOnClickListener(View.OnClickListener() {
            fun onClick(v: View) {
                doDiscovery()
                v.setVisibility(View.GONE)
            }
        })

        val pairedDevicesArrayAdapter = ArrayAdapter<String>(this, R.layout.device_name)
        mNewDevicesArrayAdapter = ArrayAdapter<String>(this, R.layout.device_name)

        val pairedListView: ListView = findViewById(R.id.paired_devices)
        pairedListView.setAdapter(pairedDevicesArrayAdapter)
        pairedListView.setOnItemClickListener(mDeviceClickListener)

        val newDevicesListView: ListView = findViewById(R.id.new_devices)
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter)
        newDevicesListView.setOnItemClickListener(mDeviceClickListener)

        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)

        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)

        mBtAdapter = BluetoothAdapter.getDefaultAdapter()

        val pairedDevices = mBtAdapter?.getBondedDevices()

        if (pairedDevices!!.size > 0) {
            findViewById<TextView>(R.id.title_paired_devices).setVisibility(View.VISIBLE)
            for (device: BluetoothDevice in pairedDevices) {
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress())
            }
        } else {
            val noDevices = getResources().getText(R.string.none_paired).toString()
            pairedDevicesArrayAdapter.add(noDevices)
        }
    }

    override protected fun onDestroy() {
        super.onDestroy()

        mBtAdapter?.cancelDiscovery()

        this.unregisterReceiver(mReceiver)
    }

    private fun doDiscovery() {
        Log.d(this::class.java.simpleName, "doDiscovery()")

        setProgressBarIndeterminateVisibility(true)
        setTitle(R.string.scanning)

        findViewById<TextView>(R.id.title_new_devices).setVisibility(View.VISIBLE)

        if (mBtAdapter?.isDiscovering() == true) {
            mBtAdapter?.cancelDiscovery()
        }

        mBtAdapter?.startDiscovery()
    }

    private val mDeviceClickListener = (object: AdapterView.OnItemClickListener {
        override fun onItemClick(av: AdapterView<*>, v: View, arg2: Int, arg3: Long) {
            mBtAdapter?.cancelDiscovery()

            val info = (v as TextView).getText().toString()
            val address = info.substring(info.length - 17)

            val intent = Intent()
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address)

            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    })

    private val mReceiver: BroadcastReceiver = (object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.getAction()

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                val device: BluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesArrayAdapter?.add(device.getName() + "\n" + device.getAddress())
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false)
                setTitle(R.string.select_device)
                if (mNewDevicesArrayAdapter?.getCount() == 0) {
                    val noDevices = getResources().getText(R.string.none_found).toString()
                    mNewDevicesArrayAdapter?.add(noDevices)
                }
            }
        }
    })
}

