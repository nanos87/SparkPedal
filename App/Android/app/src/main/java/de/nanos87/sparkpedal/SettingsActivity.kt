package de.nanos87.sparkpedal

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.settings_layout.*

class SettingsActivity : AppCompatActivity() {
    //general
    private lateinit var mContext: Context
    companion object {
        const val SHARED_SETTINGS : String = "SparkPedalSettings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        mContext = this

        val sharedPref = getSharedPreferences(SHARED_SETTINGS, Context.MODE_PRIVATE)
        val baudRate = sharedPref.getInt(R.id.cb_baud_rate.toString(), 9600)
        val dataBits = sharedPref.getInt(R.id.cb_data_bits.toString(), 8)
        val parity = sharedPref.getString(R.id.cb_parity.toString(), "none")
        val stopBits = sharedPref.getString(R.id.cb_stop_bits.toString(), "1")
        val flowControl = sharedPref.getString(R.id.cb_flow_control.toString(), "off")

        val autoConnect = sharedPref.getBoolean(R.id.ckb_auto_connect.toString(), true)

        val changeSpinnerSettings = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (parent!!.tag == "init") {
                    parent.tag = "ready"
                    return
                }

                val parentId : String = parent!!.id.toString()
                val newValue = parent!!.getItemAtPosition(position)

                with (sharedPref.edit()) {
                    when(newValue) {
                        is Int -> putInt(parentId, newValue)
                        is String -> putString(parentId, newValue)
                    }
                    commit()
                }
                Toast.makeText(mContext, "New setting: $newValue", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                return
            }
        }

        val listBaudRates = listOf<Int>(600, 1200,2400,4800,9600,14400,19200,28800, 38400,56000,57600,115200,128000,256000)
        val listDataBits = listOf<Int>(5,6,7,8)
        val listParity = listOf<String>("none","odd","even","mark","space")
        val listStopBits = listOf<String>("1","1.5","2")
        val listFlowControl = listOf<String>("off", "DSR/DTR", "RTS/CTS", "XON/XOFF")

        val adapterBaudRates = ArrayAdapter<Int>(this, android.R.layout.simple_spinner_dropdown_item, listBaudRates)
        val adapterDataBits = ArrayAdapter<Int>(this, android.R.layout.simple_spinner_dropdown_item, listDataBits)
        val adapterParity = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, listParity)
        val adapterStopBits = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, listStopBits)
        val adapterFlowControl = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, listFlowControl)

        adapterBaudRates.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterDataBits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterParity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterStopBits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapterFlowControl.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        cb_baud_rate.adapter = adapterBaudRates
        cb_data_bits.adapter = adapterDataBits
        cb_parity.adapter = adapterParity
        cb_stop_bits.adapter = adapterStopBits
        cb_flow_control.adapter = adapterFlowControl

        //set saved value initial
        cb_baud_rate.setSelection(adapterBaudRates.getPosition(baudRate))
        cb_baud_rate.tag = "init"
        cb_data_bits.setSelection(adapterDataBits.getPosition(dataBits))
        cb_data_bits.tag = "init"
        cb_parity.setSelection(adapterParity.getPosition(parity))
        cb_parity.tag = "init"
        cb_stop_bits.setSelection(adapterStopBits.getPosition(stopBits))
        cb_stop_bits.tag = "init"
        cb_flow_control.setSelection(adapterFlowControl.getPosition(flowControl))
        cb_flow_control.tag = "init"

        cb_baud_rate.onItemSelectedListener = changeSpinnerSettings
        cb_data_bits.onItemSelectedListener = changeSpinnerSettings
        cb_parity.onItemSelectedListener = changeSpinnerSettings
        cb_stop_bits.onItemSelectedListener = changeSpinnerSettings
        cb_flow_control.onItemSelectedListener = changeSpinnerSettings

        ckb_auto_connect.isChecked = autoConnect
        ckb_auto_connect.setOnClickListener {
            with (sharedPref.edit()) {
                putBoolean(R.id.ckb_auto_connect.toString(), ckb_auto_connect.isChecked)
                commit()
            }
            Toast.makeText(mContext, "AutoConnect: " + if (ckb_auto_connect.isChecked) "On" else "Off", Toast.LENGTH_SHORT).show()
        }

        settings_go_back.setOnClickListener {
            finish()
        }
    }
}