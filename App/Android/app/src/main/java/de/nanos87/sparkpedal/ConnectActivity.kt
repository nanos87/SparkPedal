package de.nanos87.sparkpedal

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.graphics.Color
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.connect_layout.*
import java.lang.Exception

class ConnectActivity : AppCompatActivity() {
    //general
    private lateinit var mContext: Context

    //bluetooth
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val SETTING_BT_DEFAULT = "de.nanos87.sparkpedal.bt_default_device"
    private lateinit var mSharedPref:SharedPreferences
    companion object {
        lateinit var BT_DEVICE: BluetoothDevice
        var usbService: UsbService? = null
        var btService: BtService? = null
        var mBtState: Int = -1
        var mUsbState: Int = -1
    }

    private val msgHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            var debugString: String = ""
            var usbBackgroundColor:Int = -1
            var btBackgroundColor:Int = -1

            when (msg.what) {
                UsbService.USB_PERMISSION_GRANTED -> {debugString = "USB_PERMISSION_GRANTED"; mUsbState=msg.what}
                UsbService.USB_PERMISSION_NOT_GRANTED -> {debugString = "USB_PERMISSION_NOT_GRANTED"; mUsbState=msg.what; usbBackgroundColor=Color.RED}
                UsbService.USB_DISCONNECTED -> {debugString = "USB_DISCONNECTED"; mUsbState=msg.what; usbBackgroundColor=Color.LTGRAY}
                UsbService.USB_NO_USB -> {debugString = "USB_NO_USB"; mUsbState=msg.what}
                UsbService.USB_NOT_SUPPORTED -> {debugString = "USB_NOT_SUPPORTED"; mUsbState=msg.what; usbBackgroundColor=Color.RED}
                UsbService.USB_DEVICE_NOT_WORKING -> {debugString = "USB_DEVICE_NOT_WORKING"; mUsbState=msg.what; usbBackgroundColor=Color.RED}
                UsbService.USB_READY -> {
                    usbDeviceList()
                    debugString = "USB_READY"
                    mUsbState=msg.what
                    usbBackgroundColor = resources.getColor(R.color.connectedGreen)
                }

                BtService.BT_STATE_NONE -> {debugString = "BT_STATE_NONE"
                    mBtState=msg.what
                    btBackgroundColor = resources.getColor( R.color.disconnectedRed)
                    loading_bluetooth.visibility = View.INVISIBLE
                }
                BtService.BT_STATE_CONNECTING -> {
                    debugString = "BT_STATE_CONNECTING"
                    mBtState=msg.what
                    loading_bluetooth.visibility = View.VISIBLE
                }
                BtService.BT_STATE_LISTEN -> {
                    debugString = "BT_STATE_LISTEN"
                    mBtState=msg.what
                    loading_bluetooth.visibility = View.VISIBLE
                }
                BtService.BT_STATE_CONNECTED -> {
                    debugString = "BT_STATE_CONNECTED"
                    mBtState=msg.what
                    btBackgroundColor = resources.getColor(R.color.connectedGreen)
                    loading_bluetooth.visibility = View.INVISIBLE
                }
            }
            if (btBackgroundColor != -1) {
                cb_bluetooth.setBackgroundColor(btBackgroundColor)
            }

            if (usbBackgroundColor != -1) {
                cb_usb.setBackgroundColor(usbBackgroundColor)
            }

            //check if auto switch is possible
            if (mSharedPref.getBoolean(R.id.ckb_auto_connect.toString(), false) &&
                (msg.what == BtService.BT_STATE_CONNECTED || msg.what == UsbService.USB_READY)) {
                if (mBtState == BtService.BT_STATE_CONNECTED && mUsbState == UsbService.USB_READY) {
                    btn_connect.callOnClick()
                }
            }


            if (!debugString.isNullOrBlank()) {
                //Toast.makeText(mContext, debugString, Toast.LENGTH_SHORT).show()
                Log.i("ConnectionService:msgHandler", debugString)
            }
        }
    }

    private val usbConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            usbService = (binder as UsbService.UsbBinder).service
            usbDeviceList()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            usbService = null
        }
    }

    private val btConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            btService = (binder as BtService.BtBinder).service
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            btService = null
        }
    }

    /**
     * OnCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connect_layout)
        mContext = this
        mSharedPref = getSharedPreferences(SettingsActivity.SHARED_SETTINGS, Context.MODE_PRIVATE)

        //Settings
        btn_settings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        /**
         * init bluetooth
         */
        //fill paired Bluetooth Devices in Spinner
        getPairedBtDevices(true)
        btn_bluetooth_refresh.setOnClickListener { getPairedBtDevices(false) }
        openBt(BT_DEVICE)

        /**
         * init serial port
         */
        //setUsbFilters()
        startUsbService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        btn_usb_refresh.setOnClickListener { usbDeviceList() }

        //go to control activity
        btn_connect.setOnClickListener {
            if (btService != null && mBtState == BtService.BT_STATE_CONNECTED) {
                if (usbService != null && mUsbState != UsbService.USB_READY) {
                    Toast.makeText(mContext, "No USB device connected", Toast.LENGTH_SHORT).show()
                }
                val intent = Intent(this, ControlActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(mContext, "No bluetooth device connected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        btService?.setHandler(msgHandler)
        usbService?.setHandler(msgHandler)
    }
    /* We need services in background as well
    public override fun onResume() {
        super.onResume()
        setUsbFilters() // Start listening notifications from UsbService
        startUsbService(UsbService::class.java, usbConnection, null) // Start UsbService(if it was not started before) and Bind it
        startBtService(BluetoothServices::class.java, btConnection)
    }
    public override fun onPause() {
        super.onPause()
        unregisterReceiver(mUsbReceiver)
        unbindService(usbConnection)
        unbindService(btConnection)
    }*/

    override fun onDestroy() {
        stopService(Intent(this, UsbService::class.java))
        stopService(Intent(this, BtService::class.java))
        super.onDestroy()
    }

    /**
     * bluetooth functions
     */
    //fill in paired Bluetooth devices and select default Device
    private fun getPairedBtDevices(useDefault: Boolean) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Toast.makeText(mContext, "No bluetooth support available", Toast.LENGTH_SHORT).show()
            return
        }
        if(!btAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH)
        }
        val btDevices: Set<BluetoothDevice> = btAdapter!!.bondedDevices

        val deviceList : ArrayList<BluetoothDevice?> = ArrayList()

        var defaultDeviceAddress : String? = null

        if (useDefault) {
            defaultDeviceAddress = mSharedPref.getString(SETTING_BT_DEFAULT, "08:EB:ED:E1:A8:39")
        }

        if (btDevices.isNotEmpty()) {
            if (useDefault && !defaultDeviceAddress.isNullOrBlank()) {
                try {
                    deviceList.add(btDevices.first { x -> x.address == defaultDeviceAddress })
                } catch (e: Exception) {
                    Toast.makeText(mContext, "couldn't find default device", Toast.LENGTH_SHORT).show()
                }
            }
            for (device: BluetoothDevice in btDevices) {
                if (!deviceList.contains(device)) {
                    deviceList.add(device)
                }
                Log.i("device", device.name)
            }
        } else {
            Toast.makeText(mContext, "no paired bluetooth devices found", Toast.LENGTH_SHORT).show()
        }

        BT_DEVICE = deviceList[0]!!

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, deviceList.map{it!!.name})
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cb_bluetooth.adapter = adapter
        cb_bluetooth.prompt = "Select bluetooth device"
        var btInit = false
        cb_bluetooth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!btInit) {
                    btInit = true
                    return
                }
                cb_bluetooth.setBackgroundColor(Color.LTGRAY)
                Toast.makeText(mContext, adapter.getItem(position) + " selected", Toast.LENGTH_SHORT).show()
                BT_DEVICE = deviceList[position]!!

                with (mSharedPref.edit()) {
                    putString(SETTING_BT_DEFAULT, deviceList[position]!!.address)
                    commit()
                }
                btService!!.connectToDevice(BT_DEVICE.address)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
                return
            }
        }
    }

    // If Bluetooth is not enabled
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                    Toast.makeText(mContext, "Bluetooth has been enabled", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(mContext, "Bluetooth has been disabled", Toast.LENGTH_SHORT).show()
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(mContext, "Bluetooth enabling has been canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openBt(device: BluetoothDevice) {
        if(device != null) {
            val extras:Bundle = Bundle()
            extras.putString("bluetooth_device", device.address)
            startBtService(BtService::class.java, btConnection, extras)
            Log.i("BtService", "BluetoothService started with %s - %s".format(device.name, device.address))
        } else {
            Toast.makeText(mContext, "No bluetooth device selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startBtService(service: Class<BtService>, serviceConnection: ServiceConnection, extras: Bundle?) {
        val intent = Intent(this, service)
        if (!BtService.SERVICE_CONNECTED) {

            if (extras != null && !extras.isEmpty) { //Pass settings to Service
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    intent.putExtra(key, extra)
                }
            }
            BtService.mHandler = msgHandler
            startService(intent)
        }
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    //serial port functions
    /*
    private fun setUsbFilters() {
        val filter = IntentFilter()
        //filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED)
        //filter.addAction(UsbService.ACTION_NO_USB)
        //filter.addAction(UsbService.ACTION_USB_DISCONNECTED)
        //filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED)
        //filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED)
        //registerReceiver(mUsbReceiver, filter)
    }
     */
    private fun startUsbService(service: Class<UsbService>, serviceConnection: ServiceConnection, extras: Bundle?) {
        if (!UsbService.SERVICE_CONNECTED) {
            //Pass settings to UsbService over comapnion object
            UsbService.baudRate = mSharedPref.getInt(R.id.cb_baud_rate.toString(), 9600)
            UsbService.dataBits = mSharedPref.getInt(R.id.cb_data_bits.toString(), 8)
            UsbService.parity = mSharedPref.getString(R.id.cb_parity.toString(), "None")
            UsbService.stopBits = mSharedPref.getString(R.id.cb_stop_bits.toString(), "1")
            UsbService.flowControl = mSharedPref.getString(R.id.cb_flow_control.toString(), "off")
            UsbService.mHandler = msgHandler


            val intent = Intent(this, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    intent.putExtra(key, extra)
                }
            }
            startService(intent)
        }
        val bindingIntent = Intent(this, service)
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }


    private fun usbDeviceList() {
        var usbList: MutableList<String> = mutableListOf()
        var usbDevices = usbService?.getUsbDevices()
        if (usbDevices != null) {
            for ((key, value) in usbService!!.getUsbDevices()) {
                if (value.productName != null) {
                    usbList.add(value.productName!!)
                } else {
                    usbList.add(key)
                }
            }
        }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, usbList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cb_usb.adapter = adapter
    }
}