package com.example.hotncold

import android.app.Activity
import android.graphics.Color
import android.graphics.Color.rgb
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import android.support.v7.app.AlertDialog
import android.widget.SeekBar
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log;

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.nio.file.Files.size
import java.util.*
import kotlin.collections.ArrayList as ArrayList1
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.widget.AdapterView
import android.widget.ArrayAdapter
import org.jetbrains.anko.toast


class SelectDeviceActivity : AppCompatActivity() {

    private var m_bluetooth_adapter:BluetoothAdapter? = null
    private lateinit var m_paired_devices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    companion object {
        val EXTRA_SSRI:String = "SSRI"
        val EXTRA_ADRESS: String = "Device_address"
    }
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter()
        if (m_bluetooth_adapter == null) {
            toast ("this device doesnt support bluetooth")
            return
        }
        if(!m_bluetooth_adapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }
        refresh.setOnClickListener{ pairedDeviceList()}


    }

    private fun pairedDeviceList(){
        m_paired_devices = m_bluetooth_adapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()

        if (!m_paired_devices.isEmpty()) {
            for (device:BluetoothDevice in m_paired_devices) {
                list.add(device)
                Log.i("device", ""+device)
            }
        }
        else {
            toast("no paired bluetooth devices found")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        device_list.adapter = adapter
        device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val device: BluetoothDevice = list[position]
            val address: String = device.address

            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(EXTRA_ADRESS, address)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode:Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if(resultCode == Activity.RESULT_OK) {
                if(m_bluetooth_adapter!!.isEnabled) {
                    toast("Bluetooth has been enabled")
                }
                else {
                    toast("Bluetooth has been disabled")
                }
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                toast("Bluetooth enabling has been cancelled")
            }
        }
    }


}
