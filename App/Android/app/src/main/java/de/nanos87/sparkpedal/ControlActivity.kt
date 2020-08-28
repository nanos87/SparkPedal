package de.nanos87.sparkpedal

import android.app.PictureInPictureParams
import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Point
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.Rational
import android.view.Display
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import kotlinx.android.synthetic.main.control_layout.*
import kotlinx.android.synthetic.main.settings_layout.*


class ControlActivity : AppCompatActivity() {
    lateinit var mSharedPref: SharedPreferences
    lateinit var mLayoutParamsDefault: Map<ConstraintLayout.LayoutParams, ConstraintLayout.LayoutParams>
    var usbService: UsbService? = ConnectActivity.usbService
    var btServices: BtService? = ConnectActivity.btService
    var commandFromApp: String = ""
    lateinit var mContext: Context
    private var lastUsbMessage: ByteArray? = null
    private val sparkCommands: Map<String, ByteArray> = hashMapOf(
        "ch1" to ubyteArrayOf(
            0x01U,
            0xfeU,
            0x00U,
            0x00U,
            0x53U,
            0xfeU,
            0x1aU,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0xf0U,
            0x01U,
            0x24U,
            0x00U,
            0x01U,
            0x38U,
            0x00U,
            0x00U,
            0x00U,
            0xf7U
        ).toByteArray(),
        "ch2" to ubyteArrayOf(
            0x01U,
            0xfeU,
            0x00U,
            0x00U,
            0x53U,
            0xfeU,
            0x1aU,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0xf0U,
            0x01U,
            0x24U,
            0x00U,
            0x01U,
            0x38U,
            0x00U,
            0x00U,
            0x01U,
            0xf7U
        ).toByteArray(),
        "ch3" to ubyteArrayOf(
            0x01U,
            0xfeU,
            0x00U,
            0x00U,
            0x53U,
            0xfeU,
            0x1aU,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0xf0U,
            0x01U,
            0x24U,
            0x00U,
            0x01U,
            0x38U,
            0x00U,
            0x00U,
            0x02U,
            0xf7U
        ).toByteArray(),
        "ch4" to ubyteArrayOf(
            0x01U,
            0xfeU,
            0x00U,
            0x00U,
            0x53U,
            0xfeU,
            0x1aU,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0xf0U,
            0x01U,
            0x24U,
            0x00U,
            0x01U,
            0x38U,
            0x00U,
            0x00U,
            0x03U,
            0xf7U
        ).toByteArray(),
        "sparkOK" to ubyteArrayOf(
            0x01U,
            0xfeU,
            0x00U,
            0x00U,
            0x41U,
            0xffU,
            0x17U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0x00U,
            0xf0U,
            0x01U,
            0x24U,
            0x00U,
            0x04U,
            0x38U,
            0xf7U
        ).toByteArray()
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
        mSharedPref = getSharedPreferences(SettingsActivity.SHARED_SETTINGS, Context.MODE_PRIVATE)

        mLayoutParamsDefault = mapOf(
            control_channel_1.layoutParams as ConstraintLayout.LayoutParams to led_channel_1.layoutParams as ConstraintLayout.LayoutParams,
            control_channel_2.layoutParams as ConstraintLayout.LayoutParams to led_channel_2.layoutParams as ConstraintLayout.LayoutParams,
            control_channel_3.layoutParams as ConstraintLayout.LayoutParams to led_channel_3.layoutParams as ConstraintLayout.LayoutParams,
            control_channel_4.layoutParams as ConstraintLayout.LayoutParams to led_channel_4.layoutParams as ConstraintLayout.LayoutParams
        )

        channelUiElements = mapOf(
            "ch1" to led_channel_1,
            "ch2" to led_channel_2,
            "ch3" to led_channel_3,
            "ch4" to led_channel_4
        )

        val msgHandler: Handler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                var debugString: String = ""
                when (msg.what) {
                    BtService.BT_STATE_NONE -> Toast.makeText(
                        mContext,
                        "BT_STATE_NONE",
                        Toast.LENGTH_SHORT
                    ).show()
                    BtService.BT_STATE_CONNECTING -> Toast.makeText(
                        mContext,
                        "BT_STATE_CONNECTING",
                        Toast.LENGTH_SHORT
                    ).show()
                    BtService.BT_STATE_CONNECTED -> Toast.makeText(
                        mContext,
                        "BT_STATE_CONNECTED",
                        Toast.LENGTH_SHORT
                    ).show()
                    BtService.BT_STATE_LISTEN -> Toast.makeText(
                        mContext,
                        "BT_STATE_LISTEN",
                        Toast.LENGTH_SHORT
                    ).show()
                    //Handle Message from Spark
                    BtService.BT_MESSAGE_RECEIVED -> {
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
                                for ((key, value) in sparkCommands) {
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
                    UsbService.CTS_CHANGE -> Toast.makeText(
                        mContext,
                        "CTS_CHANGE",
                        Toast.LENGTH_LONG
                    ).show()
                    UsbService.DSR_CHANGE -> Toast.makeText(
                        mContext,
                        "DSR_CHANGE",
                        Toast.LENGTH_LONG
                    ).show()
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

        control_go_to.setOnClickListener() {

            val usePip = mSharedPref.getBoolean(R.id.ckb_pip.toString(), true)
            if (usePip) {
                //this.enterPictureInPictureMode()
                val d: Display = windowManager.getDefaultDisplay()
                val p: Point = Point()
                d.getSize(p)
                val width: Int = p.x
                val height: Int = p.y

                val ratio: Rational = Rational(width, height)
                val pip_Builder : PictureInPictureParams.Builder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    PictureInPictureParams.Builder()
                } else {
                    TODO("VERSION.SDK_INT < O")
                }
                pip_Builder.setAspectRatio(ratio).build()
                enterPictureInPictureMode(pip_Builder.build())
            }
            val launchIntent = packageManager.getLaunchIntentForPackage("com.positivegrid.spark")
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }


        control_go_back.setOnClickListener {
            finish()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {

            val layoutParams: Map<ConstraintLayout.LayoutParams, ConstraintLayout.LayoutParams> = mapOf(
                ConstraintLayout.LayoutParams(control_channel_1.layoutParams as ConstraintLayout.LayoutParams)
                        to ConstraintLayout.LayoutParams(led_channel_1.layoutParams as ConstraintLayout.LayoutParams),
                ConstraintLayout.LayoutParams(control_channel_2.layoutParams as ConstraintLayout.LayoutParams)
                        to ConstraintLayout.LayoutParams(led_channel_2.layoutParams as ConstraintLayout.LayoutParams),
                ConstraintLayout.LayoutParams(control_channel_3.layoutParams as ConstraintLayout.LayoutParams)
                        to ConstraintLayout.LayoutParams(led_channel_3.layoutParams as ConstraintLayout.LayoutParams),
                ConstraintLayout.LayoutParams(control_channel_4.layoutParams as ConstraintLayout.LayoutParams)
                        to ConstraintLayout.LayoutParams(led_channel_4.layoutParams as ConstraintLayout.LayoutParams)
            )

            for ((ctrlParam, ledParam) in layoutParams) {
                ctrlParam.width = 100
                ctrlParam.height = 100
                ctrlParam.topMargin = 10
                ctrlParam.bottomMargin = 0
                ledParam.width /= 2
                ledParam.height /= 2
            }

            //setContentView(R.layout.control_layout_pip)
            control_title.visibility = View.GONE
            control_go_to.visibility = View.GONE
            control_go_back.visibility = View.GONE


            control_channel_1.layoutParams = layoutParams.keys.elementAt(0)
            led_channel_1.layoutParams = layoutParams.values.elementAt(0)
            control_channel_1.text = getString(R.string.control_channel_1_pip)

            control_channel_2.layoutParams = layoutParams.keys.elementAt(1)
            led_channel_2.layoutParams = layoutParams.values.elementAt(1)
            control_channel_2.text = getString(R.string.control_channel_2_pip)

            control_channel_3.layoutParams = layoutParams.keys.elementAt(2)
            led_channel_3.layoutParams = layoutParams.values.elementAt(2)
            control_channel_3.text = getString(R.string.control_channel_3_pip)

            control_channel_4.layoutParams = layoutParams.keys.elementAt(3)
            led_channel_4.layoutParams = layoutParams.values.elementAt(3)
            control_channel_4.text = getString(R.string.control_channel_4_pip)

        } else {
            //setContentView(R.layout.control_layout)
            control_title.visibility = View.VISIBLE
            control_go_to.visibility = View.VISIBLE
            control_go_back.visibility = View.VISIBLE

            control_channel_1.layoutParams = mLayoutParamsDefault.keys.elementAt(0)
            led_channel_1.layoutParams = mLayoutParamsDefault.values.elementAt(0)
            control_channel_1.text = getString(R.string.control_channel_1)

            control_channel_2.layoutParams = mLayoutParamsDefault.keys.elementAt(1)
            led_channel_2.layoutParams = mLayoutParamsDefault.values.elementAt(1)
            control_channel_2.text = getString(R.string.control_channel_2)

            control_channel_3.layoutParams = mLayoutParamsDefault.keys.elementAt(2)
            led_channel_3.layoutParams = mLayoutParamsDefault.values.elementAt(2)
            control_channel_3.text = getString(R.string.control_channel_3)

            control_channel_4.layoutParams = mLayoutParamsDefault.keys.elementAt(3)
            led_channel_4.layoutParams = mLayoutParamsDefault.values.elementAt(3)
            control_channel_4.text = getString(R.string.control_channel_4)
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
        if (!this.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !mSharedPref.getBoolean(R.id.ckb_pip.toString(), true)) {
                Toast.makeText(mContext, "SparkPedal: " + channelFullName[cmd], Toast.LENGTH_SHORT).show()
            }

    }

    private fun getCommandName(byteArray: ByteArray) : String {
        var debugString = ""
        for ((key, value) in sparkCommands) {
            if (byteArray.contentEquals(value)) {
                debugString = key
                break
            } else if (value.takeLast(5).toByteArray().contentEquals(
                    byteArray.takeLast(5).toByteArray()
                )) {
                debugString = "spark$key"
                break
            }
        }
        if (debugString.isNullOrBlank()) {
            debugString = String(byteArray, Charsets.UTF_8)
        }
        return debugString
    }
    private fun setLedState(state: Boolean) : GradientDrawable {
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