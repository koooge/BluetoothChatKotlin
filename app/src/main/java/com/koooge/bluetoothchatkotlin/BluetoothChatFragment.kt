package com.koooge.bluetoothchatkotlin

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.koooge.common.logger.Log

class BluetoothChatFragment : Fragment() {
    companion object {
        const val TAG = "BluetoothChatFragment"

        const val REQUEST_CONNECT_DEVICE_SECURE = 1
        const val REQUEST_CONNECT_DEVICE_INSECURE = 2
        const val REQUEST_ENABLE_BT = 3
    }

    private var mConversationView: ListView? = null
    private var mOutEditText: EditText? = null
    private var mSendButton: Button? = null

    private var mConnectedDeviceName: String? = null

    private var mConversationArrayAdapter: ArrayAdapter<String>? = null

    private var mOutStringBuffer: StringBuffer? = null

    private var mBluetoothAdapter: BluetoothAdapter? = null

    private var mChatService: BluetoothChatService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (mBluetoothAdapter == null) {
            val activity: FragmentActivity? = getActivity()
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            activity?.finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mBluetoothAdapter?.isEnabled() == false) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else if (mChatService == null) {
            setupChat()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mChatService?.stop()
    }

    override fun onResume() {
        super.onResume()

        if (mChatService?.getState() == BluetoothChatService.STATE_NONE) {
            mChatService?.start()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceStte: Bundle?) {
        mConversationView = view.findViewById(R.id.in_conversation)
        mOutEditText = view.findViewById(R.id.edit_text_out)
        mSendButton = view.findViewById(R.id.button_send)
    }

    private fun setupChat() {
        Log.d(TAG, "setupChat()")

        mConversationArrayAdapter = ArrayAdapter<String>(getActivity(), R.layout.message)

        mConversationView?.setAdapter(mConversationArrayAdapter)

        mOutEditText?.setOnEditorActionListener(mWriteListener)

        mSendButton?.setOnClickListener(View.OnClickListener() {
            fun onClick(v: View) {
                val view: View? = getView()
                if (null != view) {
                    val textView: TextView = view.findViewById(R.id.edit_text_out)
                    val message: String = textView.getText().toString()
                    sendMessage(message)
                }
            }
        })

        mChatService = BluetoothChatService(getActivity()!!, mHandler)

        mOutStringBuffer = StringBuffer("")
    }

    private fun ensureDiscoverable() {
        if (mBluetoothAdapter?.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
        }
    }

    private fun sendMessage(message: String) {
        if (mChatService?.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show()
            return
        }

        if (message.length > 0) {
            val send: ByteArray = message.toByteArray()
            mChatService?.write(send)

            mOutStringBuffer?.setLength(0)
            mOutEditText?.setText(mOutStringBuffer)
        }
    }


    private val mWriteListener = (object: TextView.OnEditorActionListener {
        override fun onEditorAction(view: TextView?, actionId: Int, event: KeyEvent?): Boolean {
            if (actionId == EditorInfo.IME_NULL && event?.getAction() == KeyEvent.ACTION_UP) {
                val message = view?.getText().toString()
                sendMessage(message)
            }
            return true
        }
    })

    private fun setStatus(resId: Int) {
        val activity = getActivity()
        if (null == activity) {
            return
        }
        val actionBar = activity.getActionBar()
        if (null == actionBar) {
            return
        }
        actionBar.setSubtitle(resId)
    }

    private fun setStatus(subTitle: CharSequence) {
        val activity = getActivity()
        if (null == activity) {
            return
        }
        val actionBar = activity.getActionBar()
        if (null == actionBar) {
            return
        }
        actionBar.setSubtitle(subTitle)
    }

    private val mHandler = (object: Handler() {
        override fun handleMessage(msg: Message) {
            val activity = getActivity()
            when (msg.what) {
                Constants.MESSAGE_STATE_CHANGE -> {
                    when (msg.arg1) {
                        BluetoothChatService.STATE_CONNECTED -> {
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName))
                            mConversationArrayAdapter?.clear()
                        }
                        BluetoothChatService.STATE_CONNECTING -> {
                            setStatus(R.string.title_connecting)
                        }
                        BluetoothChatService.STATE_LISTEN, BluetoothChatService.STATE_NONE -> {
                            setStatus(R.string.title_not_connected)
                        }
                    }
                }
                Constants.MESSAGE_WRITE -> {
                    val writeBuf: ByteArray = msg.obj as ByteArray
                    val writeMessage = String(writeBuf)
                    mConversationArrayAdapter?.add("Me: " + writeMessage)
                }
                Constants.MESSAGE_READ -> {
                    val readBuf: ByteArray = msg.obj as ByteArray
                    val readMessage = String(readBuf, 0, msg.arg1)
                    mConversationArrayAdapter?.add(mConnectedDeviceName + ": " + readMessage)
                }
                Constants.MESSAGE_DEVICE_NAME -> {
                    mConnectedDeviceName= msg.getData().getString(Constants.DEVICE_NAME)
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to" + mConnectedDeviceName, Toast.LENGTH_SHORT).show()
                    }
                }
                Constants.MESSAGE_TOAST -> {
                    if(null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    })

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true)
                }
            }
            REQUEST_CONNECT_DEVICE_INSECURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false)
                }
            }
            REQUEST_ENABLE_BT -> {
                if (resultCode == Activity.RESULT_OK) {
                    setupChat()
                } else {
                    Log.d(TAG, "BT not enabled")
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show()
                    getActivity()?.finish()
                }
            }
        }
    }

    private fun connectDevice(data: Intent, secure: Boolean) {
        val address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS)
        val device = mBluetoothAdapter?.getRemoteDevice(address)
        mChatService?.connect(device!!, secure)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        when (item.getItemId()) {
            R.id.secure_connect_scan -> {
                val serverIntent = Intent(getActivity(), DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
                return true
            }
            R.id.insecure_connect_scan -> {
                val serverIntent = Intent(getActivity(), DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE)
                return true
            }
            R.id.discoverable -> {
                ensureDiscoverable()
                return true
            }
        }
        return false
    }
}