package com.example.telemedycynaapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class GattManager {
    private final UUID serviceUUID;
    private final UUID characteristicUUID;
    private final UUID descriptorUUID;
    private final Context context;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothDevice bluetoothDevice;
    private IConnect listener;

    public GattManager(Context context, BluetoothDevice device) {
        this.context = context;
        this.bluetoothDevice = device;
        this.serviceUUID = UUID.fromString(context.getString(R.string.mainService));
        this.characteristicUUID = UUID.fromString(context.getString(R.string.mainCharacteristic));
        this.descriptorUUID = UUID.fromString(context.getString(R.string.baseDescriptor));
    }

    public void addListener(IConnect listener) {
        this.listener = listener;
    }

    public void connectToDevice() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_AUTO);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("AAA", "Disconecting from GATT");
                gatt.close();
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                Log.v("CONNECTING", "CONNECTING");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                Log.v("DISCONNECTING", "DISCONNECTING");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService serv : gatt.getServices()) {
                    Log.i("TAG", "Service: " + serv.getUuid());
                    for (BluetoothGattCharacteristic characteristic : serv.getCharacteristics()) {
                        Log.i("TAG", "Characteristic: " + characteristic.getUuid());
                    }
                    listener.onConnect();
                }
                BluetoothGattCharacteristic characteristic = gatt.getService(serviceUUID)
                        .getCharacteristic(characteristicUUID);
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUUID);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    gatt.writeDescriptor(descriptor);
                }
                gatt.readCharacteristic(characteristic);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] receivedBytes = characteristic.getValue();
            int[] bytesConverted = convertToUnsigned(receivedBytes);
            double humidity=parseData(bytesConverted);
            sendBroadcast(humidity);
        }
    };

    private double parseData(int[] data) {
        return 0.0 ;//do wyciagniecia wilgotnosci
    }

    private void sendBroadcast(double data) {
        Intent intent = new Intent("DATA_RECEIVED");
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }

    private int[] convertToUnsigned(byte[] bytes) {
        int[] unsignedResult = new int[bytes.length];
        for (int i = 0; i < bytes.length; i = i + 1) {
            unsignedResult[i] = bytes[i] & 0xFF;
        }
        return unsignedResult;
    }
}