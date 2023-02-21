package com.bob.bluetoothprinter

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private lateinit var btPrint: BtPrint
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btPrint = BtPrint(this)

        val sw: Switch = findViewById(R.id.printSwitch)
        val btn: Button = findViewById(R.id.btn_test)
        val txt: TextView = findViewById(R.id.printInfo)


        sw.setOnClickListener {
            if (sw.isChecked){
                btPrint.preCheck()
            } else {

            }
        }

        btn.setOnClickListener {
            btPrint.socketConnect { result ->
                if ( result["success"] == false ){
                    Log.d("Hallo", "Hallo")

                    this.runOnUiThread {

                        txt.text = result["text"].toString()
                        sw.isChecked = false

                        Toast.makeText(this, "OOPS!!!", Toast.LENGTH_SHORT).show()

                        // TODO: Pooling?

                    }

                } else {

                    var stringToPrint =
                        "  ABCDEFGHIJKLMNOPQRSTUVWXYZ \n" +
                                "  abcdefghijklmnopqrstuvwxyz \n" +
                                "          0123456789 \n" +
                                " `~!@#$%^&*()-_=+[]{}|:;'<>? \n"
                    btPrint.doPrint( stringToPrint + "\n\n\n" )

                    // I'll share how I handle printing format for regular receipts next... :)

                }
            }
        }
    }


}