package de.nanos87.sparkpedal

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BtService : Service() {
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mConnectThread: ConnectBtThread? = null
    private val mBinder: IBinder = BtBinder()

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }

    inner class BtBinder : Binder() {
        val service: BtService
            get() = this@BtService
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val btDevice: String = intent.getStringExtra("bluetooth_device")!!
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        connectToDevice(btDevice)
        SERVICE_CONNECTED = true
        return START_STICKY
    }

    @Synchronized
    public fun connectToDevice(macAddress: String) {
        val device: BluetoothDevice = mBluetoothAdapter!!.getRemoteDevice(macAddress)
        if (mState == BT_STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.interrupt() //.cancel()
            mConnectedThread = null
        }
        mConnectThread = ConnectBtThread(device)
        mConnectThread!!.start()
        setState(BT_STATE_CONNECTING)
    }

    private fun setState(state: Int) {
        mState = state
        if (mHandler != null) {
            //mHandler!!.obtainMessage(mState)
            mHandler!!.sendEmptyMessage(mState)
        }
    }

    fun setHandler(handler: Handler) {
        mHandler = handler
    }

    @Synchronized
    fun stop() {
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.interrupt() //.cancel()
            mConnectedThread = null
        }
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter!!.cancelDiscovery()
        }
        stopSelf()
        setState(BT_STATE_NONE)
        SERVICE_CONNECTED = false
    }

    fun sendData(message: String) {
        if (mConnectedThread != null) {
            mConnectedThread!!.write(message.toByteArray())
        } else {
            Toast.makeText(this@BtService, "Failed to send data", Toast.LENGTH_SHORT).show()
        }
    }
    fun sendData(msg: ByteArray) {
        if (mConnectedThread != null) {
            mConnectedThread!!.write(msg)
        } else {
            Toast.makeText(this@BtService, "Failed to send data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun stopService(name: Intent?): Boolean {
        setState(BT_STATE_NONE)
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }
        if (mConnectedThread != null) {
            mConnectedThread!!.interrupt() //.cancel()
            mConnectedThread = null
        }
        mBluetoothAdapter!!.cancelDiscovery()
        return super.stopService(name)
    }

    /*private synchronized void connected(BluetoothSocket mmSocket){

    if (mConnectThread != null){
        mConnectThread.cancel();
        mConnectThread = null;
    }
    if (mConnectedThread != null){
        mConnectedThread.cancel();
        mConnectedThread = null;
    }

    mConnectedThread = new ConnectedBtThread(mmSocket);
    mConnectedThread.start();


    setState(STATE_CONNECTED);
}*/
    private inner class ConnectBtThread(device: BluetoothDevice) : Thread() {
        private val mSocket: BluetoothSocket?
        private val mDevice: BluetoothDevice
        override fun run() {
            mBluetoothAdapter!!.cancelDiscovery()
            try {
                mSocket!!.connect()
                Log.d("service", "connect thread run method (connected)")
                val pre: SharedPreferences = getSharedPreferences("BT_NAME", 0)
                pre.edit().putString("bluetooth_connected", mDevice.name).apply()
            } catch (e: IOException) {
                try {
                    mSocket!!.close()
                    Log.d("service", "connect thread run method ( close function)")
                    setState(BT_STATE_NONE)
                } catch (e1: IOException) {
                    e1.printStackTrace()
                }
                e.printStackTrace()
            }
            //connected(mSocket);
            if (mSocket!!.isConnected) {
                mConnectedThread = ConnectedBtThread(mSocket)
                mConnectedThread!!.start()
                setState(BT_STATE_CONNECTED)
            } else {
                setState(BT_STATE_NONE)
            }
        }

        fun cancel() {
            try {
                mSocket!!.close()
                Log.d("service", "connect thread cancel method")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            mDevice = device
            var socket: BluetoothSocket? = null
            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(B_UUID)
                )
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mSocket = socket
        }
    }

    private inner class ConnectedBtThread(socket: BluetoothSocket?) : Thread() {
        private val cSocket: BluetoothSocket? = socket
        private val inS: InputStream?
        private val outS: OutputStream?
        private lateinit var buffer: ByteArray
        override fun run() {
            buffer = ByteArray(1024)
            val mByte: Int
            try {
                //mByte = inS!!.read(buffer)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            Log.d("BtService", "connected thread run method")
            try {
                while (!this.isInterrupted) {
                    if (inS == null) {
                        break
                    }
                    val bytesAvailable: Int = inS.available()
                    if (bytesAvailable > 0) {
                        val buffer = ByteArray(bytesAvailable)
                        val bytes: Int =  inS.read(buffer)
                        var hexString : String = ""
                        for (byte in buffer) {
                            hexString += String.format("%02X", byte)
                        }
                        mHandler!!.obtainMessage(BT_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();
                    }
                }
            } catch (e: IOException) {
                //
            }
        }

        fun write(buff: ByteArray?) {
            try {
                outS!!.write(buff)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        private fun cancel() {
            try {
                cSocket!!.close()
                Log.d("BTService", "connected thread cancel method")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket?.inputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            try {
                tmpOut = socket?.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            inS = tmpIn
            outS = tmpOut
        }
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    companion object {
        const val B_UUID = "00001101-0000-1000-8000-00805f9b34fb"
        // 00000000-0000-1000-8000-00805f9b34fb
        const val BT_STATE_NONE = 20
        const val BT_STATE_LISTEN = 21
        const val BT_STATE_CONNECTING = 22
        const val BT_STATE_CONNECTED = 23
        const val BT_MESSAGE_RECEIVED = 50
        private var mConnectedThread: ConnectedBtThread? = null
        var mHandler: Handler? = null
        var mState: Int = BT_STATE_NONE
        var deviceName: String? = null
        var sDevice: BluetoothDevice? = null
        var SERVICE_CONNECTED = false
    }
}