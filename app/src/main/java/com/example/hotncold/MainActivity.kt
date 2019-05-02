package com.example.hotncold

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


class MainActivity : AppCompatActivity() {

    private val LOG_TAG: String? = null // Just for logging purposes. Could be anything. Set to app_name
    private val REQUEST_ENABLE_BT = 99 // Any positive integer should work.
    private var mBluetoothAdapter: BluetoothAdapter? = null


    var pings = 0
    var playersW =0
    var playersL = 0
    var seekVal =0
    var color1 = arrayListOf(0, 219, 255)
    var color2 = arrayListOf(255, 0, 0)
    //this array is for the merged one
    var color3 = intArrayOf(0, 0, 0)

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();



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

    private fun enableBluetoothOnDevice() {
        if (mBluetoothAdapter == null) {
            Log.e(LOG_TAG, "This device does not have a bluetooth adapter")
            finish()
            // If the android device does not have bluetooth, just return and get out.
            // There's nothing the app can do in this case. Closing app.
        }

        // Check to see if bluetooth is enabled. Prompt to enable it
        if (mBluetoothAdapter!!.isEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == 0) {
                // If the resultCode is 0, the user selected "No" when prompt to
                // allow the app to enable bluetooth.
                // You may want to display a dialog explaining what would happen if
                // the user doesn't enable bluetooth.
                Toast.makeText(this, "The user decided to deny bluetooth access", Toast.LENGTH_LONG).show()
            } else
                Log.i(LOG_TAG, "User allowed bluetooth access!")
        }
    }

    private fun displayListOfFoundDevices() {
        var arrayOfFoundBTDevices = ArrayList<BluetoothObject>()

        // start looking for bluetooth devices
        mBluetoothAdapter!!.startDiscovery()

        // Discover new devices
        // Create a BroadcastReceiver for ACTION_FOUND
        val mReceiver = object : BroadcastReceiver() {

            @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND == action) {
                    // Get the bluetoothDevice object from the Intent
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    // Get the "RSSI" to get the signal strength as integer,
                    // but should be displayed in "dBm" units
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, java.lang.Short.MIN_VALUE).toInt()

                    // Create the device object and add it to the arrayList of devices
                    val bluetoothObject = BluetoothObject()
                    bluetoothObject.setBluetooth_name(device.name)
                    bluetoothObject.setBluetooth_address(device.address)
                    bluetoothObject.setBluetooth_state(device.bondState)
                    bluetoothObject.setBluetooth_type(device.type)    // requires API 18 or higher
                    bluetoothObject.setBluetooth_uuids(device.uuids)
                    bluetoothObject.setBluetooth_rssi(rssi)

                    arrayOfFoundBTDevices.add(bluetoothObject)

                    // 1. Pass context and data to the custom adapter
                    val adapter = FoundBTDevicesAdapter(applicationContext, arrayOfFoundBTDevices)

                    // 2. setListAdapter
                    setListAdapter(adapter)
                }
            }
        }
        // Register the BroadcastReceiver
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private fun getArrayOfAlreadyPairedBluetoothDevices(): ArrayList<BluetoothObject>? {
        var arrayOfAlreadyPairedBTDevices: ArrayList<BluetoothObject>? = null

        // Query paired devices
        val pairedDevices = mBluetoothAdapter!!.getBondedDevices()
        // If there are any paired devices
        if (pairedDevices.size > 0) {
            arrayOfAlreadyPairedBTDevices = ArrayList<BluetoothObject>()

            // Loop through paired devices
            for (device in pairedDevices) {
                // Create the device object and add it to the arrayList of devices
                val bluetoothObject = BluetoothObject()
                bluetoothObject.setBluetooth_name(device.name)
                bluetoothObject.setBluetooth_address(device.address)
                bluetoothObject.setBluetooth_state(device.bondState)
                bluetoothObject.setBluetooth_type(device.type)    // requires API 18 or higher
                bluetoothObject.setBluetooth_uuids(device.uuids)

                arrayOfAlreadyPairedBTDevices.add(bluetoothObject)
            }
        }

        return arrayOfAlreadyPairedBTDevices
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
            else -> super.onOptionsItemSelected(item)
        }
    }
}
