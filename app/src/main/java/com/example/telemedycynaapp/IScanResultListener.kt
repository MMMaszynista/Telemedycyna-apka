package com.example.telemedycynaapp

import android.bluetooth.BluetoothDevice

interface IScanResultListener {
    fun onScanSuccess(device: BluetoothDevice)
}