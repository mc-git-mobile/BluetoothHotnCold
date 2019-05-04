package com.example.hotncold

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.AlertDialog
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent;
import android.content.IntentFilter
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import android.widget.*

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlin.collections.ArrayList as ArrayList1
import org.jetbrains.anko.toast
import java.util.*


class MainActivity : AppCompatActivity() {



    var pings = 0
    var playersW =0
    var playersL = 0
    var seekVal =0
    var color1 = arrayListOf(0, 219, 255)
    var color2 = arrayListOf(255, 0, 0)
    //this array is for the merged one
    var color3 = intArrayOf(0, 0, 0)

    private lateinit var m_paired_devices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1
    //private var server: AcceptThread? = null                //server object
    //private var client:ConnectThread? = null


    var show_list = false

    var device_list = arrayListOf<String>("")
    private var m_bluetooth_adapter: BluetoothAdapter? = null //holds the Bluetooth Adapter
    private var arrayAdapter: ArrayAdapter<String>? = null // adapter for list view if needed
    private var listView: ListView? = null // list view for action bar to show data if needed





    //private val mReceiver = object : BroadcastReceiver() {
        //@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        //override fun onReceive(context: Context, intent: Intent) {
        //    handleBTDevice(intent)
        //}
    //}


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.device_list)
        arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, device_list)
        listView?.adapter = arrayAdapter


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




        ping.setOnClickListener{
            if (pings < 5){
            pings++
            pingCount.text = pings.toString()
            }else{
                pingCount.text = "LOSER"
                playersL++
                Toast.makeText(applicationContext,
                    "You've lost " + playersL + " times", Toast.LENGTH_SHORT).show()
                heatView.setBackgroundColor(Color.rgb(170,0,0))
                smile.setImageResource(R.drawable.loser)
                pings = 0

            }
            if(seekVal > 99){
                heatView.setBackgroundColor(Color.rgb(105,190,40))
                smile.setImageResource(R.drawable.trophy)
            }

        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBarBlue: SeekBar?) {
            }

            override fun onStartTrackingTouch(seekBarBlue: SeekBar?) {
            }


            override fun onProgressChanged(seekBarBlue: SeekBar, i: Int, b: Boolean) {
                seekVal = i
                mergeValues()
                heatView.setBackgroundColor(Color.rgb(color3[0], color3[1], color3[2]))
                if(seekVal < 25) {
                    smile.setImageResource(R.drawable.smile3)
                }else if (seekVal > 75){
                    smile.setImageResource(R.drawable.smile1)
                } else {
                    smile.setImageResource(R.drawable.smile2)
                }
                pingCount.text = seekVal.toString()


            }

        })

    }

    private fun pairedDeviceList (){
        m_paired_devices = m_bluetooth_adapter!!.bondedDevices
        val list : ArrayList<BluetoothDevice> = ArrayList()
        var device_list:ListView = findViewById(R.id.device_list)

        if (!m_paired_devices.isEmpty()) {
            for (device:BluetoothDevice in m_paired_devices) {
                list.add(device)
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
            ping.visibility = View.VISIBLE
            seekBar.visibility = View.VISIBLE
            heatView.visibility = View.VISIBLE
            pingCount.visibility = View.VISIBLE
            coldView.visibility = View.VISIBLE
            heatView.visibility = View.VISIBLE
            smile.visibility = View.VISIBLE
            device_list.visibility = View.INVISIBLE
            refresh.visibility = View.INVISIBLE



            //val intent = Intent(this, ControlActivity::class.java)
            //intent.putExtra(EXTRA_ADRESS, address)
            //startActivity(intent)
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




    fun mergeValues(){
        //0,50,100
        //50,150,200
        val percent1 = (100 - seekVal)
        val percent2 = seekVal
        color3[0] = ((percent2 * color2[0]) + (percent1 * color1[0])) / 100
        color3[1] = ((percent2 * color2[1]) + (percent1 * color1[1])) / 100
        color3[2] = ((percent2 * color2[2]) + (percent1 * color1[2])) / 100
        //For proof that the percents are right for color merging
        //val coast= Toast.makeText(applicationContext, "$percent1 , $percent2", Toast.LENGTH_LONG)
        // coast.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_About ->{
                //val toast= Toast.makeText(applicationContext, "Buzzzzzz", Toast.LENGTH_LONG)
                //toast.show()
                val builder = AlertDialog.Builder(this)
                builder.setTitle("About Hot n Cold")
                builder.setMessage("1. Connect to your friend's device via bluetooth\n"  +
                        "2. Find companion following beeps and colors\n" +
                        "3. Win by turning the box green")
                //builder.setPositiveButton("OK", DialogInterface.OnClickListener(function = x))

                //builder.setPositiveButton(android.R.string.yes) { dialog, which ->
                    //Toast.makeText(applicationContext,
                  //      android.R.string.yes, Toast.LENGTH_SHORT).show()
                //}

                //builder.setNegativeButton(android.R.string.no) { dialog, which ->
                    //Toast.makeText(applicationContext,
                  //      android.R.string.no, Toast.LENGTH_SHORT).show()
                //}

                builder.setNeutralButton("Continue") { dialog, which ->
                    Toast.makeText(applicationContext,
                        "Alright", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            true}

            R.id.connect -> {

                /*
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
                arrayAdapter?.notifyDataSetChanged()

                var device_list:ListView = findViewById(R.id.device_list)
                device_list.adapter = arrayAdapter

                device_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    val device: BluetoothDevice = list[position]
                    val address: String = device.address

                    //val intent = Intent(this, ControlActivity::class.java)
                    //intent.putExtra(EXTRA_ADRESS, address)
                    //startActivity(intent)
                }*/

                //pairedDeviceList()

                var device_list:ListView = findViewById(R.id.device_list)

                ping.visibility = View.INVISIBLE
                seekBar.visibility = View.INVISIBLE
                heatView.visibility = View.INVISIBLE
                pingCount.visibility = View.INVISIBLE
                coldView.visibility = View.INVISIBLE
                heatView.visibility = View.INVISIBLE
                smile.visibility = View.INVISIBLE
                device_list.visibility = View.VISIBLE
                refresh.visibility = View.VISIBLE




                pairedDeviceList()



                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
