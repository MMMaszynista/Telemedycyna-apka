package com.example.telemedycynaapp

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.example.telemedycynaapp.databinding.ActivityMainBinding

class MainActivity : ComponentActivity(),IConnect {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.connectButton.setOnClickListener {

            val bleScanner = BLEScanner(this, getString(R.string.devName))

            bleScanner.setOnDeviceFoundListener(object : IScanResultListener {
                override fun onScanSuccess(device: BluetoothDevice) {
                    val gattManager = GattManager(this@MainActivity, device)
                    gattManager.addListener(this@MainActivity)
                    Handler(Looper.getMainLooper()).postDelayed({
                        gattManager.connectToDevice()
                    }, 2000)
                    gattManager.connectToDevice()
                }
            })
            bleScanner.startScan()
        }
    }

    override fun onConnect() {
        startActivity(Intent(this,ChartActivity::class.java))
    }
}