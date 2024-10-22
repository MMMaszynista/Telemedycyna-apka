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

import java.util.ArrayList;
import java.util.List;


public class BLEScanner {
    private boolean scanning;
    private final BluetoothLeScanner bluetoothLeScanner;
    private final Handler handler = new Handler();
    private final String searchedDev;
    private IScanResultListener listener;

    public BLEScanner(Context context, String searchedDev) {
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.searchedDev = searchedDev;
    }

    public void setOnDeviceFoundListener(IScanResultListener listener){
        this.listener=listener;
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (scanning) {
            scanning = false;
            bluetoothLeScanner.stopScan(bleScanCallBack);
            return;
        }
        int SCAN_PERIOD = 10000;
        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                scanning = false;
                bluetoothLeScanner.stopScan(bleScanCallBack);
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
    private void stopScan() {
        if (!scanning) return;
        scanning = false;
        bluetoothLeScanner.stopScan(bleScanCallBack);
    }

    @SuppressLint("MissingPermission")
    private final ScanCallback bleScanCallBack = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device.getName() != null) {
                stopScan();
                listener.onScanSuccess(device);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            stopScan();
        }
    };
}