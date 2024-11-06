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
    private val requestedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) //tablica z nazwami uprawnien wymaganymi do dzialania takich funkcji jak bluetooth
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
        binding.connectButton.setOnClickListener { //przypisanie do przycisku funkcji gwarantujacej mozliwosc polaczenia sie z urzadzeniem mierzącym wilgotnosc
            blePart()
        }
    }

    private fun blePart() { //funkcja wywolywana po wcisnieciu przycisku "Połącz się" odpowiada za procedure wyszukiwania i laczenia sie z urzadzeniem mierzacym wilgotnosc
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
            override fun onScanSuccess(device: BluetoothDevice) { //funkcja wywoływana przy znalezienu urządzenia podczas skanowania
                gattManager = GattManager(this@MainActivity, device)
                gattManager.setOnConnectListener(this@MainActivity)
                Handler(Looper.getMainLooper()).postDelayed({
                    gattManager.connectToDevice()
                }, 200)
            }

            override fun onScanNotFound() { //funkcja wywoływana gdy urządzenie nie zostało znalezione podczas skanowania
                AlertDialog.Builder(this@MainActivity).apply { //wyświetl okno dialogowe o braku urządzenia
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

    override fun onConnect() { //funkcja wywoływana przy pomyślnym połączeniu z urządzeniem
        chartLauncher.launch(Intent(this, ChartActivity::class.java))
    }

    private fun setChartLauncher() { //funkcja ustawiajaca dla obiektu chartLauncher funkcje ktora przy powrocie zwraca stan aktywnosci 
        chartLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_CANCELED) {
                    gattManager.disconnect()
                    Toast.makeText(this, "Połączenie zakończone", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun checkPermissions(): Boolean { //funkcja sprawdzająca czy zostały przyznane uprawnienia
        return requestedPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}