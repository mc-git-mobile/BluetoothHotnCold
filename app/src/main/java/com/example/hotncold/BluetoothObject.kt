package com.example.hotncold

import android.os.ParcelUuid

class BluetoothObject {
    private var name: String = ""
    private var address: String = ""
    private var state: Int = 0
    private var type: Int = 0
    private var uuids: Array<ParcelUuid>? = null
    private var rssi: Int = 0

    //bluetoothObject.setBluetooth_name(device.name)
    //bluetoothObject.setBluetooth_address(device.address)
    //bluetoothObject.setBluetooth_state(device.bondState)
    //bluetoothObject.setBluetooth_type(device.type)    // requires API 18 or higher
    //bluetoothObject.setBluetooth_uuids(device.uuids)
    // bluetoothObject.setBluetooth_rssi(rssi)


    fun setBluetooth_name (new_name: String){
        name = new_name
    }
    fun setBluetooth_address (new_adress: String){
        address = new_adress
    }
    fun setBluetooth_state (new_state: Int){
        state = new_state
    }
    fun setBluetooth_type (new_type: Int){
        type = new_type
    }
    fun setBluetooth_uuids (new_uuids: Array<ParcelUuid>?){
        uuids = new_uuids
    }
    fun setBluetooth_rssi (new_rssi: Int){
        rssi = new_rssi
    }





}
