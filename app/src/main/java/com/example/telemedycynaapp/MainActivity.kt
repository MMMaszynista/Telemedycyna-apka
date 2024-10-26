package com.example.telemedycynaapp

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.telemedycynaapp.Interfaces.IConnect
import com.example.telemedycynaapp.Interfaces.IScanResultListener
import com.example.telemedycynaapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), IConnect {
    private lateinit var binding: ActivityMainBinding
    private lateinit var chartLauncher: ActivityResultLauncher<Intent>
    private lateinit var gattManager: GattManager
    private val requestedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permanentlyDenied = false
            permissions.entries.forEach { entry ->
                if (!entry.value) {
                    if (!shouldShowRequestPermissionRationale(entry.key)) {
                        permanentlyDenied = true
                    }
                }
            }

            when {
                permanentlyDenied -> {
                    DialogManager.showSettingsDialog(this)
                }

                checkPermissions() -> {
                    blePart()
                }

                else -> {
                    DialogManager.showDialogPermDenied(this,::blePart)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setChartLauncher()
        binding.connectButton.setOnClickListener {
            blePart()
        }
    }

    private fun blePart() {
        if (!ServiceStateUtils.isLocationEnabled(this) || !ServiceStateUtils.isBluetoothEnabled(this)) {
            DialogManager.showBtLocationDialog(this)
            return
        }
        if (!checkPermissions()) {
            requestMultiplePermissions.launch(requestedPermissions)
            return
        }
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

    private fun checkPermissions(): Boolean {
        return requestedPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}