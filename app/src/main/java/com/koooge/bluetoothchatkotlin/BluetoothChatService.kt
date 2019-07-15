package com.koooge.bluetoothchatkotlin

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import com.koooge.common.logger.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothChatService(context: Context, handler: Handler) {
    companion object {
        const val TAG = "BluetoothChatService"

        const val NAME_SECURE = "BluetoothChatSecure"
        const val NAME_INSECURE = "BluetoothChatInsecure"

        val MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB" ) // fa87c0d0-afac-11de-8a39-0800200c9a66")
        val MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

        const val STATE_NONE = 0
        const val STATE_LISTEN = 1
        const val STATE_CONNECTING = 2
        const val STATE_CONNECTED = 3
    }

    private val mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val mHandler: Handler = handler
    private var mSecureAcceptThread: AcceptThread? = null
    private var mInsecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int = STATE_NONE
    private var mNewState: Int = mState

    private @Synchronized fun updateUserInterfaceTitle() {
        mState = getState()
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState)
        mNewState = mState

        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget()
    }

    @Synchronized fun getState(): Int {
        return mState
    }

    @Synchronized fun start() {
        Log.d(TAG, "start")

        mConnectThread?.cancel()
        mConnectThread = null

        mConnectedThread?.cancel()
        mConnectedThread = null

        mSecureAcceptThread?.cancel()
        mSecureAcceptThread = null

        mInsecureAcceptThread?.cancel()
        mInsecureAcceptThread = null

        updateUserInterfaceTitle()
    }

    @Synchronized fun connect(device: BluetoothDevice, secure: Boolean) {
        Log.d(TAG, "connect to: " + device)

        if (mState == STATE_CONNECTING) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        mConnectedThread?.cancel()
        mConnectedThread = null

        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.start()

        updateUserInterfaceTitle()
    }

    @Synchronized fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        Log.d(TAG, "connected, Socket Type:" + socketType)

        mConnectThread?.cancel()
        mConnectThread = null

        mConnectedThread?.cancel()
        mConnectedThread = null

        mSecureAcceptThread?.cancel()
        mSecureAcceptThread = null

        mInsecureAcceptThread?.cancel()
        mInsecureAcceptThread = null

        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        val msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device.getName())
        msg.setData(bundle)
        mHandler.sendMessage(msg)
        updateUserInterfaceTitle()
    }

    @Synchronized fun stop() {
        Log.d(TAG, "stop")

        mConnectThread?.cancel()
        mConnectThread = null

        mConnectedThread?.cancel()
        mConnectedThread = null

        mSecureAcceptThread?.cancel()
        mSecureAcceptThread = null

        mInsecureAcceptThread?.cancel()
        mInsecureAcceptThread = null
        mState = STATE_NONE
        updateUserInterfaceTitle()
    }

    fun write(out: ByteArray) {
        var r: ConnectedThread? = null
        synchronized (this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        r!!.write(out)
    }

    private fun connectionFailed() {
        val msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg.setData(bundle)
        mHandler.sendMessage(msg)

        mState = STATE_NONE
        updateUserInterfaceTitle()

        start()
    }

    private fun connectionLost() {
        val msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg.setData(bundle)
        mHandler.sendMessage(msg)

        mState = STATE_NONE
        updateUserInterfaceTitle()

        start()
    }

    private inner class AcceptThread : Thread {
        private var mmServerSocket: BluetoothServerSocket? = null
        private var mSocketType: String? = null

        constructor(secure: Boolean) {
            var tmp: BluetoothServerSocket? = null
            val mSocketType = if (secure) "Secure" else "InSecure"

            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE)
                } else {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_INSECURE, MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e)
            }
            mmServerSocket = tmp
            mState = STATE_LISTEN
        }

        override fun run() {
            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this)
            setName("AcceptThread" + mSocketType)

            var socket: BluetoothSocket? = null

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "accept() failed", e)
                    break
                }

                if (socket != null) {
                    synchronized (this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING -> {
                                connected(socket, socket.getRemoteDevice(), mSocketType!!)
                            }
                            STATE_NONE, STATE_CONNECTED -> {
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "Could not close unwanted socket", e)
                                }
                            }
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType)
        }

        fun cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this)
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e)
            }
        }
    }

    private inner class ConnectThread : Thread {
        private var mmSocket: BluetoothSocket? = null
        private var mmDevice: BluetoothDevice? = null
        private var mSocketType: String? = null

        constructor(device: BluetoothDevice, secure: Boolean) {
            mmDevice = device
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE)
                } else {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }
            mmSocket = tmp
            mState = STATE_CONNECTING
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType)
            setName("ConnectThread" + mSocketType)

            mAdapter.cancelDiscovery()

            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2)
                }
                connectionFailed()
                return
            }

            synchronized(this@BluetoothChatService) {
                mConnectThread = null
            }

            connected(mmSocket!!, mmDevice!!, mSocketType!!)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e)
            }
        }
    }

    private inner class ConnectedThread : Thread {
        private var mmSocket: BluetoothSocket? = null
        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream?  = null

        constructor(socket: BluetoothSocket, socketType: String) {
            Log.d(TAG, "create connectedThread: " + socketType)
            mmSocket = socket
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket.getInputStream()
                tmpOut = socket.getOutputStream()
            } catch(e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer: ByteArray = ByteArray(1024)
            var bytes: Int = 0

            while (mState == STATE_CONNECTED) {
                try {
                    bytes = mmInStream!!.read(buffer)

                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)

                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
            } catch(e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }
}