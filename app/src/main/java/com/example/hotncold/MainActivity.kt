package com.example.hotncold

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.TargetApi
import android.app.Activity
import android.bluetooth.*
import android.graphics.Color
import android.os.Vibrator
import android.os.Bundle
import android.support.v7.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.support.v7.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent;
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlin.collections.ArrayList as ArrayList1
import org.jetbrains.anko.toast
import java.io.*
import java.util.*
import kotlin.math.abs

/*
Chris Johnson
Matt Harvey

This is a  hide and seek game, we use bluetooth RSSI readings from
a selected paired device to estimate the range. The range is used to give
a color estimation(hot or cold) whether the user is close or far away.
Once the seeker is within 0.5 meters the seeker will get a win and the hider
will get a lose. The wins and losses are counted then saved to a file and
loads it when the user requests the count of wins and losses.

Credit to -
Proffessor Aaron Gordon
    - https://github.com/aaronjg50/TalkerM
    - we used Proffessor gordons bluetooth implementation and expanded
      on it to get RSSI data and send different values.
LUQMAN HAKEM from IOT AND ELECTRONICS
    - https://iotandelectronics.wordpress.com/2016/10/07/how-to-calculate-distance-from-the-rssi-value-of-the-ble-beacon/
    - we used the equation in this article to calculate meters from the bluetooth RSSI value


 */


class MainActivity : AppCompatActivity() {

    val fileName = "win.txt"

    var pings = 0

    var playersW = 0
    var playersL = 0



    var seekVal = 0
    var color1 = arrayListOf(0, 219, 255)
    var color2 = arrayListOf(255, 0, 0)
    //this array is for the merged one
    var color3 = intArrayOf(0, 0, 0)

    var device_to_find = "none"

    private var myUUID: UUID? = null
    private var server: AcceptThread? = null                //server object
    private var client:ConnectThread? = null

    var sent = "00"
    var disconect = false
    var win = false
    var lose = false
    var start = false

    var ten:Double = 10.00000000000000000000000000
    var measured_power = -69 // 1 meter rssi

    private val mReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onReceive(context: Context, intent: Intent) {
            handleBTDevice(intent)
        }
    }

    private lateinit var m_paired_devices: Set<BluetoothDevice>
    private val REQUEST_ENABLE_BLUETOOTH = 1

    var device_list = arrayListOf<String>("")
    var device_list1 = arrayListOf<String>("")

    private var m_bluetooth_adapter: BluetoothAdapter? = null //holds the Bluetooth Adapter
    private var arrayAdapter: ArrayAdapter<String>? = null // adapter for list view if needed
    private var listView: ListView? = null // list view for action bar to show data if needed

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        save(playersW, playersL)
        val vibratorService = getSystemService(Context.VIBRATOR_SERVICE ) as Vibrator


        supportActionBar?.setDisplayShowTitleEnabled(false)
        myUUID = UUID.fromString(MY_UUID_STRING)
        listView = findViewById(R.id.device_list_m)
        arrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, device_list)
        listView?.adapter = arrayAdapter
        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter()

        seekBar.setProgress(100)

        if (m_bluetooth_adapter == null) {
            toast ("this device doesnt support bluetooth")
            return
        }
        if(!m_bluetooth_adapter!!.isEnabled) {
            val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BLUETOOTH)
        }


        ping.setOnClickListener{

            if (pings < 5){
                pings++
                pingCount.text = pings.toString()
            }else{

                pingCount.text = "LOSER"
                load()
                playersL++
                save(playersW, playersL)


                Toast.makeText(applicationContext,
                    "You've lost " + playersL + " times", Toast.LENGTH_LONG).show()
                vibratorService.vibrate(500)

                heatView.setBackgroundColor(Color.rgb(170,0,0))
                lose = true
                smile.setImageResource(R.drawable.loser)
                pings = 0
                //sent = "01"

            }
            if(seekVal > 98){
                heatView.setBackgroundColor(Color.rgb(105,190,40))
                smile.setImageResource(R.drawable.trophy)
                load()
                playersW ++
                save(playersW, playersL)
                Toast.makeText(applicationContext,
                    "You've won " + playersW + " time(s)", Toast.LENGTH_LONG).show()
                vibratorService.vibrate(500)
                pings = 0
                win = true
                //sent = "99"
            }
            if (device_to_find !== "none") {
                setUpBroadcastReceiver()
            }
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)

            registerReceiver(mReceiver, filter)
            m_bluetooth_adapter!!.startDiscovery()

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

            }

        })

    }
    fun load() {

        if(fileName.toString()!=null && fileName.toString().trim()!=""){
            var fileInputStream: FileInputStream? = null
            fileInputStream = openFileInput(fileName)

            var inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder: StringBuilder = StringBuilder()
            var text: String = ""
            text = bufferedReader.readLine()
            Log.i(TCLIENT, text)



            Log.i(TCLIENT, "$$$$$$$$$$$$$$$$$$$$$$")
            Log.i(TCLIENT, text)
            var split_words = text?.split(Regex("#"))
            playersW = split_words!![0].toInt()
            playersL = split_words!![1].toInt()
            Log.i(TCLIENT, "@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")

            Log.i(TCLIENT, playersW.toString())
            Log.i(TCLIENT, playersL.toString())


        }else{
            Toast.makeText(applicationContext,"file name cannot be blank",Toast.LENGTH_LONG).show()
        }

    }

    fun save(win:Int, lose:Int) {
        Log.i(TCLIENT, "iiiiinnnn   savvee")


        val fileOutputStream:FileOutputStream
        var win1 = win.toString()
        var lose1 = lose.toString()
        var combined = win1 + "#" + lose1

        try {
            fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(combined.toByteArray())
            Log.i(TCLIENT, combined)
            Log.i(TCLIENT, "combined stringin save")

            //Toast.makeText(applicationContext,"data saved",Toast.LENGTH_LONG).show()

        }catch (e: FileNotFoundException){
            Log.i(TCLIENT, "file not found")
            e.printStackTrace()
        }catch (e: NumberFormatException){
            Log.i(TCLIENT, "number format exception")

            e.printStackTrace()
        }catch (e: IOException){
            Log.i(TCLIENT, "IO exception")

            e.printStackTrace()
        }catch (e: Exception){
            Log.i(TCLIENT, "Exception")

            e.printStackTrace()
        }

    }

    override fun onActivityResult(requestCode:Int, resultCode: Int, data: Intent?) {
        Log.i(LOG_TAG, "onActivityResult(): requestCode = $requestCode")
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                Log.i(LOG_TAG, "  --    Bluetooth is enabled")
                getPairedDevices() //find already known paired devices
                setUpBroadcastReceiver()
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
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.reset -> {
                reset()
                true
            }

            R.id.discoverable -> {
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, N_SECONDS)
                startActivity(discoverableIntent)
                //create server thread
                server = AcceptThread()
                if (server != null) {   //start server thread
                    Log.i(TSERVER, "Connect Button spawning server thread")
                    server!!.start()     //calls AcceptThread's run() method
                } else {
                    Log.i(TSERVER, "setupButtons(): server is null")
                }

                true

            }
            R.id.disconnect -> {
                disconect = true
                sent = "00"
                if (device_to_find !== "none") {
                    setUpBroadcastReceiver()
                }
                val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
                registerReceiver(mReceiver, filter)
                m_bluetooth_adapter!!.startDiscovery()
                true
            }

            R.id.action_About ->{
                val builder = AlertDialog.Builder(this)
                builder.setTitle("About Hot n Cold")
                builder.setMessage("1. Connect to your friend's device via bluetooth\n"  +
                        "2. Find companion following beeps and colors\n" +
                        "3. Win by turning the box green")

                builder.setNeutralButton("Continue") { dialog, which ->
                    Toast.makeText(applicationContext,
                        "Alright", Toast.LENGTH_SHORT).show()
                }
                builder.show()
            true}
            R.id.action_results ->{
                message()
                true
            }
            R.id.connect -> {

                start = true

                var list: ArrayList<BluetoothDevice> = ArrayList()
                var list2: ArrayList<String> = ArrayList()
                m_paired_devices = m_bluetooth_adapter!!.bondedDevices

                if (m_paired_devices.isNotEmpty()) {
                    for (device: BluetoothDevice in m_paired_devices) {
                        list.add(device)
                        list2.add(device.name.toString())

                    }
                }


                ping.visibility = View.INVISIBLE
                heat.visibility = View.INVISIBLE
                seekBar.visibility = View.INVISIBLE
                heatView.visibility = View.INVISIBLE
                pingCount.visibility = View.INVISIBLE
                coldView.visibility = View.INVISIBLE
                heatView.visibility = View.INVISIBLE
                smile.visibility = View.INVISIBLE
                device_list_m.visibility = View.VISIBLE
                back.visibility = View.VISIBLE


                //////////////////////////////////////////////////////////////////////////////////////////////

                val adapter2 = ArrayAdapter(this, android.R.layout.simple_list_item_1, list2)
                device_list_m.adapter = adapter2


                device_list_m.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                    device_to_find = list2[position]

                    Log.i(TCLIENT, "getting device to find<<<<--------------------" + device_to_find)

                    device_list_m.visibility = View.INVISIBLE

                    ping.visibility = View.VISIBLE
                    seekBar.visibility = View.VISIBLE
                    heatView.visibility = View.VISIBLE
                    pingCount.visibility = View.VISIBLE
                    coldView.visibility = View.VISIBLE
                    heatView.visibility = View.VISIBLE
                    smile.visibility = View.VISIBLE
                    heat.visibility = View.VISIBLE
                    back.visibility = View.INVISIBLE


                    list.clear()

                    list2.clear()

                    getPairedDevices()
                    setUpBroadcastReceiver()

                }

                back.setOnClickListener {

                device_list_m.visibility = View.INVISIBLE
                ping.visibility = View.VISIBLE
                seekBar.visibility = View.VISIBLE
                heatView.visibility = View.VISIBLE
                pingCount.visibility = View.VISIBLE
                coldView.visibility = View.VISIBLE
                heatView.visibility = View.VISIBLE
                smile.visibility = View.VISIBLE
                heat.visibility = View.VISIBLE
                back.visibility = View.INVISIBLE

                }


                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onResume() {
        super.onResume()
        m_bluetooth_adapter = BluetoothAdapter.getDefaultAdapter()
        Log.i(LOG_TAG, "onResume()")
        if (m_bluetooth_adapter == null) {
            // Device does not support Bluetooth
            Log.i(LOG_TAG, "No Bluetooth on this device")
            Toast.makeText(baseContext,
                "No Bluetooth on this device", Toast.LENGTH_LONG).show()
        } else if (!m_bluetooth_adapter!!.isEnabled) {
            Log.i(LOG_TAG, "enabling Bluetooth")
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
        Log.i(LOG_TAG, "End of onResume()")
    }

    private fun getPairedDevices() {//find already known paired devices
        val pairedDevices = m_bluetooth_adapter!!.bondedDevices
        Log.i(TCLIENT, "--------------\ngetPairedDevices() - Known Paired Devices")
        // If there are paired devices
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                Log.i(TCLIENT, device.name + "\n" + device)
                device_list1.add("" + device.name + "\n" + device + "\n")
            }
        }
        Log.i(TCLIENT, "getPairedDevices() - End of Known Paired Devices\n------")
    }

    fun reset(){
        heatView.setBackgroundColor(Color.rgb(0,0,0))
        pings = 0
        pingCount.text = pings.toString()
        smile.setImageResource(R.drawable.smile3)
    }




    fun message (){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Win/Loss Results")
        builder.setMessage("You have won $playersW time(s)\n"
        + "You have lost $playersL time(s)")

        builder.setNeutralButton("Continue") { dialog, which ->
            Toast.makeText(applicationContext,
                "Alright", Toast.LENGTH_SHORT).show()

        }
        builder.show()
    }

    /**
     * Client scans for nearby Bluetooth devices
     */
    private fun setUpBroadcastReceiver() {
        // Create a BroadcastReceiver for ACTION_FOUND
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED)    {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION)
            Log.i(TCLIENT,"Getting Permission")
            return
            //Discovery will be setup in onRequestPermissionResult() if permission is granted
        }
        setupDiscovery()
    }

    /**
     * Callback when request for permission is addressed by the user.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED) {
                    Log.i(LOG_TAG, "Fine_Location Permission granted")
                    setupDiscovery()
                } else {    //tracking won't happen since user denied permission
                    Log.i(LOG_TAG, "Fine_Location Permission refused")
                }
                return
            }
        }
    }

    /**
     * Activate Bluetooth discovery for the client
     */
    private fun setupDiscovery() {
        Log.i(TCLIENT,"Activating Discovery")
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(mReceiver, filter)
        m_bluetooth_adapter!!.startDiscovery()
    }

    //******************************************************************************************
    /**
     * called by BroadcastReceiver callback when a new BlueTooth device is found
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun handleBTDevice(intent: Intent) {

        Log.i(TCLIENT, "handleBRDevice() -- starting   <<<<--------------------")
        val action = intent.action
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND == action) {
            // Get the BluetoothDevice object from the Intent
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            var rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE)

            val deviceName  =
                if (device.name != null) {
                    device.name.toString()
                } else {
                    "--no name--"
                }
            Log.i(TCLIENT, deviceName + "iiijhkjhkhn\n" + device + "\ndevice to find" + device_to_find)


            if (device_to_find == deviceName){
                var na = abs(rssi.toInt())


                var part = (measured_power - rssi)/(ten*2).toDouble()

                var distance = Math.pow(ten, part)
                var new_dist = distance*10
                var new_dist1 = distance*100
                var new_dist2 = new_dist*2

                Log.i(TCLIENT,"RSSI dist- " +  distance + "meters")


                Log.i(TCLIENT,"new RSSI dist *10- " +  new_dist.toInt() + "meters")
                Log.i(TCLIENT,"new RSSI dist *100- " +  new_dist1.toInt() + "meters")

                Log.i(TCLIENT,"new RSSI dist*10*2- " +  new_dist2.toInt() + "meters")

                Log.i(TCLIENT,"RSSI- " +  rssi.toInt())
                if (new_dist2.toInt() < 100 && new_dist2.toInt() > 0){
                    var reverse = 100 - new_dist2.toInt()
                    if (reverse <10 && reverse > 0 ){
                        Log.i(TCLIENT,"RSSI in range-  " +  abs(rssi.toInt()))

                        if (!disconect) {
                            sent = "0" + reverse.toString()
                        }
                    }
                    else {
                        Log.i(TCLIENT,"RSSI in range-  " +  abs(rssi.toInt()))

                        if (!disconect && !start ) {
                            sent = reverse.toString()
                        }


                    }

                    seekBar.setProgress(reverse)
                    Log.i(TCLIENT,"RSSI in range-  " +  abs(rssi.toInt()))

                }
                else if (new_dist2.toInt() <= 0 ) {


                    if (!disconect && !start) {
                        sent = "99"
                        seekBar.setProgress(99)

                    }


                    Log.i(TCLIENT,"RSSI too far <0  " +  abs(rssi.toInt()))
                }
                else if (new_dist2.toInt() >= 100 ) {

                    if (!disconect && !start) {
                        sent = "02"
                        seekBar.setProgress(2)

                    }

                    Log.i(TCLIENT,"RSSI too far >0  " +  abs(rssi.toInt()))
                }
                if (win) {
                    sent = "01"
                }
                if (lose){
                    sent = "99"
                }
                if(start) {
                    sent = "mm"

                }
                start = false
                win = false
                win = false
                disconect = false

                Log.i(TCLIENT,"Canceling Discovery")
                m_bluetooth_adapter!!.cancelDiscovery()
                Log.i(TCLIENT,"Connecting")
                client = ConnectThread(device)  //FIX** remember and reconnect if interrupted?
                Log.i(TCLIENT,"Running Connect Thread")
                client?.start()

            }
        }
    }
    //****************************************************************************************************

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onStop() {
        super.onStop()
        m_bluetooth_adapter!!.cancelDiscovery()    //stop looking for Bluetooth devices
        client?.cancel()
    }

    /**
     * Called from server thread to display received message.
     * This action is specific to this App.
     * @param msg The received info to display
     */
    fun echoMsg(msg: String) {
        Toast.makeText(applicationContext,
            "You've won!-  $msg", Toast.LENGTH_LONG).show()
        }

    ////////////////// Client Thread to talk to Server here ///////////////////

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class ConnectThread(mmDevice: BluetoothDevice):Thread(){//from android developer
    private var mmSocket: BluetoothSocket? = null

        init {

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            Log.i(TCLIENT, "ConnectThread: init()")
            try {
                // myUUID is the app's UUID string, also used by the server code
                mmSocket = mmDevice.createRfcommSocketToServiceRecord(myUUID)
            } catch (e: IOException) {
                Log.i(TCLIENT, "IOException when creating RFcommSocket\n $e")
            }
        }

        override fun run() {
            // Cancel discovery because it will slow down the connection
            Log.i(TCLIENT, "ConnectThread: run()")
            Log.i(TCLIENT, "in ClientThread - Canceling Discovery")
            m_bluetooth_adapter!!.cancelDiscovery()
            if (mmSocket == null) {
                Log.e(TCLIENT,"ConnectThread:run(): mmSocket is null")
            }
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception after 12 seconds (or so)
                Log.i(TCLIENT, "Connecting to server")
                mmSocket!!.connect()
            } catch (connectException: IOException) {
                Log.i(TCLIENT,
                    "Connect IOException when trying socket connection\n $connectException")
                // Unable to connect; close the socket and get out
                try {
                    mmSocket!!.close()
                } catch (closeException: IOException) {
                    Log.i(TCLIENT,
                        "Close IOException when trying socket connection\n $closeException")
                }

                return
            }
            Log.i(TCLIENT, "Connection Established")
            val sock = mmSocket!!
            // Do work to manage the connection (in a separate thread)
            manageConnectedSocket(sock)      //talk to server
        }

        //manage the connection over the passed-in socket
        private fun manageConnectedSocket(socket: BluetoothSocket) {

            val out: OutputStream
            //val theMessage = "ABC"      //test message: send actual message here
            //val msg = theMessage.toByteArray()
            val sentB = sent.toByteArray()
            try {
                Log.i(TCLIENT, "Sending the message: [$sent]")

                out = socket.outputStream
                out.write(sentB)
            } catch (ioe: IOException) {
                Log.e(TCLIENT, "IOException when opening outputStream\n $ioe")
                return
            }

        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */
        fun cancel() {
            try {
                mmSocket!!.close()
            } catch (ioe: IOException) {
                Log.e(TCLIENT, "IOException when closing outputStream\n $ioe")
            }
        }
    }


    ///////////////////////////////  ServerSocket stuff here ///////////////////////////

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class AcceptThread : Thread() {  //from android developer
        private var mmServerSocket: BluetoothServerSocket? = null
        var socket: BluetoothSocket? = null

        init {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is supposed to be final
            val tmp: BluetoothServerSocket
            try {
                // myUUID is the app's UUID string, also used by the client code
                tmp = m_bluetooth_adapter!!.listenUsingRfcommWithServiceRecord(SERVICE_NAME, myUUID)
                Log.i(TSERVER, "AcceptThread registered the server\n")
                mmServerSocket = tmp
            } catch (e: IOException) {
                Log.e(TSERVER, "AcceptThread registering the server failed\n $e")
            }
        }

        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                Log.i(TSERVER, "AcceptTread.run(): Server Looking for a Connection")
                try {
                    socket = mmServerSocket!!.accept()  //block until connection made or exception
                    Log.i(TSERVER, "Server socket accepting a connection")
                } catch (e: IOException) {
                    Log.e(TSERVER, "socket accept threw an exception\n $e")
                    break
                }

                // If a connection was accepted
                if (socket != null) {
                    Log.i(TSERVER, "Server Thread run(): Connection accepted")
                    // Do work to manage the connection (in a separate thread)
                    manageConnectedSocket(socket!!)
                    break
                } else {
                    Log.i(TSERVER, "Server Thread run(): The socket is null")
                }
            }
        }

        //manage the Server's end of the conversation on the passed-in socket
        @RequiresApi(Build.VERSION_CODES.M)
        fun manageConnectedSocket(socket: BluetoothSocket) {
            Log.i(TSERVER, "\nManaging the Socket\n")
            val inSt: InputStream
            val nBytes: Int
            val msg = ByteArray(2) //arbitrary size

            try {

                inSt = socket.inputStream
                nBytes = inSt.read(msg)
                Log.i(TSERVER, "\nServer Received $nBytes \n")
            } catch (ioe: IOException) {
                Log.e(TSERVER, "IOException when opening inputStream\n $ioe")
                return
            }

            try {

                val msgString = msg.toString(Charsets.UTF_8)
                Log.i(TSERVER, "\nServer Received  $nBytes, Bytes:  [$msgString]\n")
                Log.i(TSERVER, msgString.toString() + "+++++++++++++++++++++++")


                while (msgString.toString() != "00") {
                    val msgString = msg.toString(Charsets.UTF_8)
                    var seek:SeekBar = findViewById(R.id.seekBar)


                    if (msgString.toString() == "99") {
                        load()
                        playersW += 1
                        save(playersW, playersL)
                        seek.setProgress(2)
                        runOnUiThread(Runnable{
                            smile.setImageResource(R.drawable.trophy)
                            Toast.makeText(applicationContext,
                                "You've won " , Toast.LENGTH_LONG).show()
                            val vibratorService = getSystemService(Context.VIBRATOR_SERVICE ) as Vibrator
                            vibratorService.vibrate(500)
                            pings = 0
                            pingCount.text = pings.toString()
                        })

                    }
                    else if(msgString.toString() == "01"){
                        load()
                        playersL +=1
                        save(playersW, playersL)
                        seek.setProgress(97)
                        runOnUiThread(Runnable{
                            smile.setImageResource(R.drawable.loser)
                            Toast.makeText(applicationContext,
                                "You've Lost " , Toast.LENGTH_LONG).show()
                            val vibratorService = getSystemService(Context.VIBRATOR_SERVICE ) as Vibrator

                            vibratorService.vibrate(500)

                            pings = 0
                            pingCount.text = pings.toString()
                        })

                    }
                    else if(msgString.toString() == "mm"){
                        var startup = "startup routine"
                    }
                    else{
                        seek.setProgress(msgString.toInt())
                    }

                    run()
                }


            } catch (uee: UnsupportedEncodingException) {
                Log.e(TSERVER,
                    "UnsupportedEncodingException when converting bytes to String\n $uee")
            } finally {
                cancel()        //for this App - close() after 1 (or no) message received
            }
        }

        /**
         * Will cancel the listening socket, and cause the thread to finish
         */
        fun cancel() {
            try {
                mmServerSocket!!.close()
            } catch (ioe: IOException) {
                Log.e(TSERVER, "IOException when canceling serverSocket\n $ioe")
            }
        }
    }

    //////////////////////////////////  companion object ///////////////////////////////////////
    companion object {
        private const val ACCESS_FINE_LOCATION = 1
        private const val N_SECONDS = 255
        private const val TCLIENT = "--Talker Client--"  //for Log.X
        private const val TSERVER = "--Talker SERVER--"  //for Log.X
        private const val REQUEST_ENABLE_BT = 3313  //our own code used with Intents
        private const val MY_UUID_STRING = "12ce62cb-60a1-4edf-9e3a-ca889faccd6c"
        private const val SERVICE_NAME = "Talker"
        private const val LOG_TAG = "--Talker----"
    }



}
