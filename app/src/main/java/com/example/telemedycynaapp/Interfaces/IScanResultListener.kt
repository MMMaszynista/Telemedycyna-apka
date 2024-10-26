package com.example.telemedycynaapp.Interfaces

import android.bluetooth.BluetoothDevice

interface IScanResultListener {
    fun onScanSuccess(device: BluetoothDevice)
    fun onScanNotFound()
}