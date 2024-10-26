package com.example.telemedycynaapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import com.example.telemedycynaapp.Interfaces.IConnect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class GattManager {
    private final UUID serviceUUID;
    private final UUID characteristicUUID;
    private final UUID descriptorUUID;
    private final Context context;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothDevice bluetoothDevice;
    private IConnect scanListener;
    private boolean isConnected = false;

    public GattManager(Context context, BluetoothDevice device) {
        this.context = context;
        this.bluetoothDevice = device;
        this.serviceUUID = UUID.fromString(context.getString(R.string.mainService));
        this.characteristicUUID = UUID.fromString(context.getString(R.string.mainCharacteristic));
        this.descriptorUUID = UUID.fromString(context.getString(R.string.baseDescriptor));
    }

    public void setOnConnectListener(IConnect listener) {
        this.scanListener = listener;
    }

    public void connectToDevice() {
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void disconnect() {
        bluetoothGatt.disconnect();
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!isConnected) {
                    scanListener.onConnect();
                }
                isConnected = true;
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
            float reading = ByteBuffer.wrap(receivedBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            sendBroadcast(reading);
        }
    };

    private void sendBroadcast(float data) {
        Intent intent = new Intent("DATA_RECEIVED");
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }
}