package de.nanos87.sparkpedal

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import kotlinx.android.synthetic.main.control_layout.*


class ControlActivity : AppCompatActivity() {
    var usbService: UsbService? = ConnectActivity.usbService
    var btServices: BtService? = ConnectActivity.btService
    var commandFromApp: String = ""
    lateinit var mContext: Context
    private var lastUsbMessage: ByteArray? = null
    private val sparkCommands: Map<String, ByteArray> = hashMapOf(
        "ch1" to ubyteArrayOf(0x01U,0xfeU,0x00U,0x00U,0x53U,0xfeU,0x1aU,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0xf0U,0x01U,0x24U,0x00U,0x01U,0x38U,0x00U,0x00U,0x00U,0xf7U).toByteArray(),
        "ch2" to ubyteArrayOf(0x01U,0xfeU,0x00U,0x00U,0x53U,0xfeU,0x1aU,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0xf0U,0x01U,0x24U,0x00U,0x01U,0x38U,0x00U,0x00U,0x01U,0xf7U).toByteArray(),
        "ch3" to ubyteArrayOf(0x01U,0xfeU,0x00U,0x00U,0x53U,0xfeU,0x1aU,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0xf0U,0x01U,0x24U,0x00U,0x01U,0x38U,0x00U,0x00U,0x02U,0xf7U).toByteArray(),
        "ch4" to ubyteArrayOf(0x01U,0xfeU,0x00U,0x00U,0x53U,0xfeU,0x1aU,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0xf0U,0x01U,0x24U,0x00U,0x01U,0x38U,0x00U,0x00U,0x03U,0xf7U).toByteArray(),
        "sparkOK" to ubyteArrayOf(0x01U,0xfeU,0x00U,0x00U,0x41U,0xffU,0x17U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0x00U,0xf0U,0x01U,0x24U,0x00U,0x04U,0x38U,0xf7U).toByteArray()
    )
    private lateinit var channelUiElements: Map<String, Button>
    private val channelFullName: Map<String, String> = mapOf(
        "ch1" to "Channel 1",
        "ch2" to "Channel 2",
        "ch3" to "Channel 3",
        "ch4" to "Channel 4"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_layout)

        mContext = this

        channelUiElements = mapOf(
        "ch1" to led_channel_1,
        "ch2" to led_channel_2,
        "ch3" to led_channel_3,
        "ch4" to led_channel_4
        )

        val msgHandler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                var debugString : String = ""
                when(msg.what) {
                    BtService.BT_STATE_NONE -> Toast.makeText(mContext, "BT_STATE_NONE", Toast.LENGTH_SHORT).show()
                    BtService.BT_STATE_CONNECTING -> Toast.makeText(mContext, "BT_STATE_CONNECTING", Toast.LENGTH_SHORT).show()
                    BtService.BT_STATE_CONNECTED -> Toast.makeText(mContext, "BT_STATE_CONNECTED", Toast.LENGTH_SHORT).show()
                    BtService.BT_STATE_LISTEN -> Toast.makeText(mContext, "BT_STATE_LISTEN", Toast.LENGTH_SHORT).show()
                    //Handle Message from Spark
                    BtService.BT_MESSAGE_RECEIVED ->  {
                        val btMsg: ByteArray = msg.obj as ByteArray
                        debugString = getCommandName(btMsg)
                        if (!commandFromApp.isNullOrBlank()) {  //if the app wants to change channel send same message to both devices
                            Log.i("BT-Message(App)", debugString)
                            //Toast.makeText(mContext, "BT-Message(App): $debugString", Toast.LENGTH_SHORT ).show()
                            if (btMsg.contentEquals(sparkCommands["sparkOK"]!!)) {
                                switchChannel(commandFromApp)
                                usbService!!.write(sparkCommands[commandFromApp])
                            }
                            commandFromApp = ""
                        } else { //otherwise just pass the incoming message

                            usbService!!.write(btMsg)
                            Log.i("BT-Message(passThrough)", debugString)
                            //Toast.makeText(mContext, "BT-Message(passThrough): $debugString", Toast.LENGTH_SHORT ).show()

                            //set button_settings color in app
                            if (lastUsbMessage != null) {
                                for ((key,value) in sparkCommands) {
                                    if (lastUsbMessage!!.contentEquals(value)) {
                                        switchChannel(key)

                                    }
                                }
                                lastUsbMessage = null
                            }
                            if (debugString.startsWith("sparkch")) {
                                switchChannel(debugString)
                            }
                        }
                    }
                    UsbService.USB_MESSAGE_RECEIVED -> {
                        lastUsbMessage = msg.obj as ByteArray
                        debugString = getCommandName(lastUsbMessage!!)

                        btServices!!.sendData(lastUsbMessage!!)
                        Log.i("USB-Message", debugString)
                        //Toast.makeText(mContext, "USB-Message: $debugString", Toast.LENGTH_SHORT ).show()

                    }
                    UsbService.CTS_CHANGE -> Toast.makeText(mContext, "CTS_CHANGE", Toast.LENGTH_LONG).show()
                    UsbService.DSR_CHANGE -> Toast.makeText(mContext, "DSR_CHANGE", Toast.LENGTH_LONG).show()
                }
            }
        }

        usbService!!.setHandler(msgHandler)
        btServices!!.setHandler(msgHandler)

        control_channel_1.setOnClickListener { sendCommandFromApp("ch1") }
        control_channel_2.setOnClickListener { sendCommandFromApp("ch2") }
        control_channel_3.setOnClickListener { sendCommandFromApp("ch3") }
        control_channel_4.setOnClickListener { sendCommandFromApp("ch4") }

        if (ConnectActivity.mUsbState != UsbService.USB_READY) {
            control_channel_1.callOnClick()
        }

        //Disconnect
        control_go_to.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage("com.positivegrid.spark")
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
        control_go_back.setOnClickListener {
            finish()
        }
    }

    private fun sendCommandFromApp(command: String) {
        Log.i("ControlButton", command)
        val msg:ByteArray = sparkCommands[command]!!
        btServices!!.sendData(msg)
        commandFromApp = command
    }
    private fun switchChannel(command: String) {
        val cmd = command.replace("spark", "")
        for ((key, value) in channelUiElements) {
            if (key != cmd) {
                value.background = setLedState(false)
            } else {
                value.background = setLedState(true)
            }

        }
        if (!this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                Toast.makeText(mContext, "SparkPedal: "+channelFullName[cmd], Toast.LENGTH_SHORT).show()
            }

    }

    private fun getCommandName(byteArray: ByteArray) : String {
        var debugString = ""
        for ((key,value) in sparkCommands) {
            if (byteArray.contentEquals(value)) {
                debugString = key
                break
            } else if (value.takeLast(5).toByteArray().contentEquals(byteArray.takeLast(5).toByteArray())) {
                debugString = "spark$key"
                break
            }
        }
        if (debugString.isNullOrBlank()) {
            debugString = String(byteArray, Charsets.UTF_8)
        }
        return debugString
    }
    private fun setLedState(state:Boolean) : GradientDrawable {
        var color = resources.getColor(R.color.colorLedOff)
        if (state) {
            color = resources.getColor(R.color.colorLedOn)
        }
        val led = GradientDrawable()
        led.shape = GradientDrawable.OVAL
        led.color = ColorStateList.valueOf(color)
        return led
    }
}