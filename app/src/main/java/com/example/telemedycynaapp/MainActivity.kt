package com.example.telemedycynaapp

import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.telemedycynaapp.databinding.ActivityMainBinding

class MainActivity : ComponentActivity(), IConnect {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chartLauncher: ActivityResultLauncher<Intent>
    private lateinit var gattManager: GattManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setChartLauncher()
        binding.connectButton.setOnClickListener {

            val bleScanner = BLEScanner(this, getString(R.string.devName))

            bleScanner.setOnDeviceFoundListener(object : IScanResultListener {
                override fun onScanSuccess(device: BluetoothDevice) {
                    gattManager = GattManager(this@MainActivity, device)
                    gattManager.setOnConnectListener(this@MainActivity)
                    Handler(Looper.getMainLooper()).postDelayed({
                        gattManager.connectToDevice()
                    }, 200)
                }

                override fun onScanNotFound() {
                    AlertDialog.Builder(this@MainActivity).apply {
                        setTitle("Wystąpił błąd")
                        setMessage("Nie znaleziono urządzenia. Spróbuj ponownie.")
                        setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        create()
                        show()
                    }
                }
            })
            bleScanner.startScan()
        }
    }

    override fun onConnect() {
        chartLauncher.launch(Intent(this, ChartActivity::class.java))
    }

    private fun setChartLauncher() {
        chartLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CANCELED) {
                    gattManager.disconnect()
                    Toast.makeText(this, "Połączenie zakończone", Toast.LENGTH_LONG).show()
                }
            }
    }
}