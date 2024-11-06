package com.example.telemedycynaapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.location.LocationManager;

public class ServiceStateUtils { //klasa pomocnicza sprawdzająca czy moduł jest włączony

    public static boolean isLocationEnabled(Activity context) { // sprawdza czy Lokalizacja jest włączona
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isBluetoothEnabled(Activity context) { // sprawdza czy moduł Bluetooth jest włączony
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
}