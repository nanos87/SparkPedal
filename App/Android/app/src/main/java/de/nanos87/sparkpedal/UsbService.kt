package de.nanos87.sparkpedal

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import com.felhr.usbserial.CDCSerialDevice
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface.*
import java.io.UnsupportedEncodingException

class UsbService : Service() {
    private val mBinder: IBinder = UsbBinder()
    private var mContext: Context? = null
    private var usbManager: UsbManager? = null
    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var serialPort: UsbSerialDevice? = null
    private var serialPortConnected = false

    //Callback for received USB Data
    private val mCallback = UsbReadCallback { data ->
        try {
            //val data = String(arg0, Charsets.UTF_8)
            if (mHandler != null) mHandler!!.obtainMessage(USB_MESSAGE_RECEIVED, data).sendToTarget()
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
    }

    //State changes in the CTS line will be received here
    private val ctsCallback = UsbCTSCallback { if (mHandler != null) mHandler!!.obtainMessage(CTS_CHANGE).sendToTarget() }

    //State changes in the DSR line will be received here
    private val dsrCallback = UsbDSRCallback { if (mHandler != null) mHandler!!.obtainMessage(DSR_CHANGE).sendToTarget() }

    private val usbReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val granted = intent.extras!!.getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED)
                    if (granted) {
                        // User accepted our USB connection.
                        //val intent = Intent(ACTION_USB_PERMISSION_GRANTED)
                        //context.sendBroadcast(intent)
                        mHandler!!.sendEmptyMessage(USB_PERMISSION_GRANTED)
                        //Try to open the device as a serial port
                        connection = usbManager!!.openDevice(device)
                        ConnectionThread().start()
                    } else {
                        // User not accepted our USB connection.
                        //val intent = Intent(ACTION_USB_PERMISSION_NOT_GRANTED)
                        //context.sendBroadcast(intent)
                        mHandler!!.sendEmptyMessage(USB_PERMISSION_NOT_GRANTED)
                    }
                }
                ACTION_USB_ATTACHED -> {if (!serialPortConnected) findSerialPortDevice() }
                ACTION_USB_DETACHED -> {
                    //val intent = Intent(ACTION_USB_DISCONNECTED)
                    //context.sendBroadcast(intent)
                    mHandler!!.sendEmptyMessage(USB_DISCONNECTED)
                    if (serialPortConnected) {
                        serialPort!!.close()
                    }
                    serialPortConnected = false
                }
            }
        }
    }

    /*
     * onCreate will be executed when service is started. It configures an IntentFilter to listen for
     * incoming Intents (USB ATTACHED, USB DETACHED...) and it tries to open a serial port.
     */
    override fun onCreate() {
        mContext = this
        serialPortConnected = false
        SERVICE_CONNECTED = true
        setFilter()
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        findSerialPortDevice()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serialPort!!.close()
        unregisterReceiver(usbReceiver)
        SERVICE_CONNECTED = false
    }

    //Write Data through serial port
    fun write(data: ByteArray?) {
        if (serialPort != null) serialPort!!.write(data)
    }

    fun setHandler(handler: Handler?) {
        mHandler = handler
    }

    fun getUsbDevices(): HashMap<String, UsbDevice> {
        return usbManager!!.deviceList
    }

    private fun findSerialPortDevice() {
        val usbDevices = usbManager!!.deviceList
        if (usbDevices.isNotEmpty()) {

            // first, dump the hashmap for diagnostic purposes
            for ((_, value) in usbDevices) {
                device = value
                Log.d(TAG, String.format("USBDevice.HashMap (vid:pid) (%X:%X)-%b class:%X:%X name:%s",
                    device!!.vendorId, device!!.productId,
                    UsbSerialDevice.isSupported(device),
                    device!!.deviceClass, device!!.deviceSubclass,
                    device!!.deviceName))
            }
            for ((_, value) in usbDevices) {
                device = value
                val deviceVID = device!!.vendorId
                val devicePID = device!!.productId

//                if (deviceVID != 0x1d6b && (devicePID != 0x0001 && devicePID != 0x0002 && devicePID != 0x0003) && deviceVID != 0x5c6 && devicePID != 0x904c) {
                if (UsbSerialDevice.isSupported(device)) {
                    // There is a supported device connected - request permission to access it.
                    requestUserPermission()
                    break
                } else {
                    connection = null
                    device = null
                }
            }
            if (device == null) {
                // There are no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                //val intent = Intent(ACTION_NO_USB)
                //sendBroadcast(intent)
                mHandler!!.sendEmptyMessage(USB_NO_USB)
            }
        } else {
            Log.d(TAG, "findSerialPortDevice() usbManager returned empty device list.")
            // There is no USB devices connected. Send an intent to MainActivity
            //val intent = Intent(ACTION_NO_USB)
            //sendBroadcast(intent)
            mHandler!!.sendEmptyMessage(USB_NO_USB)

        }
    }

    private fun setFilter() {
        val filter = IntentFilter()
        filter.addAction(ACTION_USB_PERMISSION)
        filter.addAction(ACTION_USB_DETACHED)
        filter.addAction(ACTION_USB_ATTACHED)
        registerReceiver(usbReceiver, filter)
    }

    /*
     * Request user permission.
     */
    private fun requestUserPermission() {
        Log.d(TAG, String.format("requestUserPermission(%X:%X)", device!!.vendorId, device!!.productId))
        val mPendingIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager!!.requestPermission(device, mPendingIntent)
    }

    inner class UsbBinder : Binder() {
        val service: UsbService
            get() = this@UsbService
    }

    /*
     * A simple thread to open a serial port.
     */
    private inner class ConnectionThread : Thread() {
        override fun run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection)
            if (serialPort != null) {
                if (serialPort!!.open()) {
                    serialPortConnected = true

                    //set baud rate
                    serialPort!!.setBaudRate(baudRate!!)

                    //set data bits
                    when(dataBits) {
                        5 -> serialPort!!.setDataBits(DATA_BITS_5)
                        6 -> serialPort!!.setDataBits(DATA_BITS_6)
                        7 -> serialPort!!.setDataBits(DATA_BITS_7)
                        else -> serialPort!!.setDataBits(DATA_BITS_8)
                    }
                    //set parity
                    when(parity?.toLowerCase()) {
                        "even"-> serialPort!!.setParity(PARITY_EVEN)
                        "mark"-> serialPort!!.setParity(PARITY_MARK)
                        "odd"-> serialPort!!.setParity(PARITY_ODD)
                        "space"-> serialPort!!.setParity(PARITY_SPACE)
                        else -> serialPort!!.setParity(PARITY_NONE)
                    }
                    //set stop bits
                    when(stopBits) {
                        "1.5" -> serialPort!!.setStopBits(STOP_BITS_15)
                        "1" -> serialPort!!.setStopBits(STOP_BITS_2)
                        else -> serialPort!!.setStopBits(STOP_BITS_1)
                    }
                    //set flow control (only for cp2102 / ft232
                    when(flowControl?.toLowerCase()) {
                        "rts/cts" -> serialPort!!.setFlowControl(FLOW_CONTROL_RTS_CTS)
                        "xon/xoff" -> serialPort!!.setFlowControl(FLOW_CONTROL_XON_XOFF)
                        "dsr/dtr" -> serialPort!!.setFlowControl(FLOW_CONTROL_DSR_DTR)
                        else -> serialPort!!.setFlowControl(FLOW_CONTROL_OFF)
                    }
                    serialPort!!.read(mCallback)
                    serialPort!!.getCTS(ctsCallback)
                    serialPort!!.getDSR(dsrCallback)

                    //Could be needed for some micro controller
                    //Thread.sleep(2000);

                    // Everything went as expected. Send an intent to MainActivity
                    //val intent = Intent(ACTION_USB_READY)
                    //mContext!!.sendBroadcast(intent)
                    mHandler!!.sendEmptyMessage(USB_READY)
                } else {
                    // Serial port could not be opened
                    if (serialPort is CDCSerialDevice) {
                        //val intent = Intent(ACTION_CDC_DRIVER_NOT_WORKING)
                        //mContext!!.sendBroadcast(intent)
                        mHandler!!.sendEmptyMessage(CDC_DRIVER_NOT_WORKING)
                    } else {
                        //val intent = Intent(ACTION_USB_DEVICE_NOT_WORKING)
                        //mContext!!.sendBroadcast(intent)
                        mHandler!!.sendEmptyMessage(USB_DEVICE_NOT_WORKING)
                    }
                }
            } else {
                // No driver for given device
                //val intent = Intent(ACTION_USB_NOT_SUPPORTED)
                //mContext!!.sendBroadcast(intent)
                mHandler!!.sendEmptyMessage(USB_NOT_SUPPORTED)
            }
        }
    }

    companion object {
        const val TAG = "UsbService"
        const val USB_READY = 30
        const val USB_NOT_SUPPORTED = 31
        const val USB_NO_USB = 32
        const val USB_PERMISSION_GRANTED = 33
        const val USB_PERMISSION_NOT_GRANTED = 34
        const val USB_DISCONNECTED = 35
        const val CDC_DRIVER_NOT_WORKING = 36
        const val USB_DEVICE_NOT_WORKING = 37


        //const val ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY"
        const val ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED"
        const val ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED"
        //const val ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED"
        //const val ACTION_NO_USB = "com.felhr.usbservice.NO_USB"
        //const val ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED"
        //const val ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED"
        //const val ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED"
        //const val ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING"
        //const val ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING"
        const val USB_MESSAGE_RECEIVED = 60
        const val CTS_CHANGE = 1
        const val DSR_CHANGE = 2
        private const val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
        private const val BAUD_RATE = 9600 // BaudRate. Change this value if you need
        var SERVICE_CONNECTED = false
        var baudRate: Int? = null
        var dataBits: Int? = null
        var parity: String? = null
        var stopBits: String? = null
        var flowControl: String? = null
        var mHandler: Handler? = null
        var mState : Int = -1
    }
}