package com.bob.bluetoothprinter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class BtPrint(context: Context) {

    // Define the caller context and activity (tips on optimization will be much appreciated :)
    var localContext = context
    private val activity = context as Activity
    private val sharedPrefs = context.getSharedPreferences(context.packageName + ".META", Context.MODE_PRIVATE)
    // Other initializer
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var printers = ArrayList<BluetoothDevice>()
    lateinit var printer: BluetoothDevice
    lateinit var socket: BluetoothSocket
    var status = ""

    fun preCheck() {
//        preCheckStart()
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("permission", "not granted")
                return
            }
        }
        // No bluetooth
        if (bluetoothAdapter == null) {
            status = "Perangkat ini tidak memiliki bluetooth"
//            printSwitch.isChecked = false
//            preCheckDone()
        } else {

            // Bluetooth inactive
            if (!bluetoothAdapter.isEnabled) {
                status = "Bluetooth tidak aktif"
//                printSwitch.isChecked = false
//                preCheckDone()
            } else {

                // Bluetooth available and active, so refresh printers list.
                refreshPrinters()
                if (printers.size > 0) {

                    // Loop the printers to crosscheck with sharedPrefs, and also to prepare arrays for printer selection
                    // dialog if needed.
                    val pNames = Array(printers.size) { "" }
                    val pAddrs = Array(printers.size) { "" }
                    var deviceFound = false
                    for (i in 0 until printers.size) {
                        pNames[i] = printers[i].name
                        pAddrs[i] = printers[i].address // How to do this "correctly" in Kotlin? :D

                        // Printer available in sharedPrefs, attempt connection and break.
                        if (printers[i].address == sharedPrefs.getString("lastPrinter", "")) {
                            deviceFound = true
                            printer = printers[i]
                            testConnection()
                            break
                        }
                    }

                    // If it gets here
                    if (!deviceFound) {

                        // Show printer selection dialog
                        val builder = AlertDialog.Builder(localContext)
                        builder.setTitle("Sambungkan ke Printer")
                            .setItems(
                                pNames
                            ) { _, which ->

                                // On selected, save and rerun preCheck()
                                sharedPrefs.edit().putString("lastPrinter", pAddrs[which]).apply()
                                preCheck()
                            }
                        builder.create()
                        builder.setCancelable(false)
                        builder.show()
                    }
                } else {
                    // No printers
                    status = "Mohon Sambungkan ke Printer"
//                    printSwitch.isChecked = false
//                    preCheckDone()
                }
            }
        }
        Log.d("p", status)
    }

    private fun refreshPrinters() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        // Clean up first, but I found out that sometimes the device must be reset, and/or Clear Data in
        // Bluetooth Share app, to really refresh the paired devices list, after we Forget/Add a bluetooth device.
        // This probably a device issue (or just me lacking experience).
        printers.clear()

        // Filter the paired devices for printers
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // this is my attempt to "really refresh" the list

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        if (pairedDevices != null) {
            for (i in pairedDevices) {
//                if (i.bluetoothClass.deviceClass.toString() == "1664") { // Identifier (or something) for printers.

                // Now we have a list of printers
                printers.add(i)

//                }
            }
        }
    }

    fun testConnection() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        // Make sure discovery isn't running
        bluetoothAdapter?.cancelDiscovery()

        // Socket connect will freeze the UI, so we're doing this in another thread and get the callback
        status = printer.name


        socketConnect { result ->

            // Other threads can't touch UI without runOnUiThread()
            activity.runOnUiThread {
                status = result["text"].toString()
//                printInfo.append(result["text"].toString())

                // Connection failed, delete from sharedPrefs
                if (result["success"] == false) {
                    sharedPrefs.edit().putString("lastPrinter", "").apply()
//                    printSwitch.isChecked = false
                } else {
//                    printSwitch.isChecked = true
                }
//                preCheckDone()
            }

            // Done checking. From here we assume the printer is active, nearby, and is ready for real printing.
            // We close the socket to allow other devices to use the printer although it might require some more coding
            // to imitate some kind of pooling service (no such service in my two test/cheap printers here).
            socket.close()
        }
    }

    fun socketConnect(callback: (HashMap<String, Any>) -> Unit) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S){
            if (ActivityCompat.checkSelfPermission(
                    localContext,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        // Make sure socket is closed
        if (::socket.isInitialized) socket.close()

        // Google for explanation on this :D
        socket = printer.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))

        thread(start = true) {
            val result = HashMap<String, Any>()
            try {
                socket.connect()
                Log.d("succes","")
                result["success"] = true
                result["text"] = "terhubung."
            } catch (e: IOException) {
                Log.e("error","")
                result["success"] = false
                result["text"] = e
            }
            callback(result)
        }
    }

    fun doPrint(stringToPrint: String, keepSocket: Boolean = false) {
        Log.d("kepanggil",stringToPrint)

        // ESC/POS default format
//        socket.outputStream.write(byteArrayOf(27, 33, 0))

        // Print your string
        socket.outputStream.write(stringToPrint.toByteArray())


        if (!keepSocket) {
            Log.d("kepanggil","lagii")
            socket.close()
        }
    }

}