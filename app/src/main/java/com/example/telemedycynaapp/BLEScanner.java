package com.example.telemedycynaapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;

import com.example.telemedycynaapp.Interfaces.IScanResultListener;

import java.util.ArrayList;
import java.util.List;


public class BLEScanner { //klasa do wyszukiwania urządzenia bluetooth mierzącego wilgotnosc
    private boolean scanning;
    private final BluetoothLeScanner bluetoothLeScanner;
    private final Handler handler = new Handler();
    private final String searchedDev;
    private boolean deviceFound=false;
    private IScanResultListener listener;

    public BLEScanner(Context context, String searchedDev) { //konstruktor inicjalizujacy obiekty klasy
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.searchedDev = searchedDev;
    }

    public void setOnDeviceFoundListener(IScanResultListener listener){
        this.listener=listener;
    }

    @SuppressLint("MissingPermission")
    public void startScan() { //rozpoczecie skanowania w poszukiwaniu urządzeń bluetooth
        if (scanning) {
            scanning = false;
            bluetoothLeScanner.stopScan(bleScanCallBack);
            return;
        }
        deviceFound = false;
        int SCAN_PERIOD = 5000;
        handler.postDelayed(() -> {
            scanning = false;
            bluetoothLeScanner.stopScan(bleScanCallBack);
            if(!deviceFound){
                listener.onScanNotFound();
            }
        }, SCAN_PERIOD);
        scanning = true;
        List<ScanFilter> filters = new ArrayList<>();
        ScanFilter filterByName = new ScanFilter.Builder()
                .setDeviceName(searchedDev)
                .build();
        filters.add(filterByName);
        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        bluetoothLeScanner.startScan(filters, scanSettings, bleScanCallBack);
    }

    @SuppressLint("MissingPermission")
    private void stopScan() { //zatrzymanie skanowania w poszukiwaniu urzadzenia mierzacego wilgotnosc
        if (!scanning) return;
        scanning = false;
        bluetoothLeScanner.stopScan(bleScanCallBack);
    }

    @SuppressLint("MissingPermission")
    private final ScanCallback bleScanCallBack = new ScanCallback() { //funkcja  wywołania zwrotnego uruchamiana jesli urządzenie zostało odnalezione
        @Override
        public void onScanResult(int callbackType, ScanResult result) { //akcja po wykryciu urządzenia
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null) {
                stopScan();
                deviceFound=true;
                listener.onScanSuccess(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) { //funkcja wywoływana jeśli skan nie może zostać włączony
            super.onScanFailed(errorCode);
            stopScan();
        }
    };
}