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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressLint("MissingPermission")
public class GattManager { //klaaa odpowiedzialna za połączenie z urządzeniem
    private final UUID serviceUUID;
    private final UUID characteristicUUID;
    private final UUID descriptorUUID;
    private final Context context;
    private BluetoothGatt bluetoothGatt;
    private final BluetoothDevice bluetoothDevice;
    private IConnect scanListener;
    private boolean isConnected = false;
    private float readingGlobal=0;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public GattManager(Context context, BluetoothDevice device) { //inicjalizacja pó
        this.context = context;
        this.bluetoothDevice = device;
        this.serviceUUID = UUID.fromString(context.getString(R.string.mainService));
        this.characteristicUUID = UUID.fromString(context.getString(R.string.mainCharacteristic));
        this.descriptorUUID = UUID.fromString(context.getString(R.string.baseDescriptor));
        scheduler.scheduleWithFixedDelay(() -> {
            if(readingGlobal!=0) {
                sendBroadcast(readingGlobal);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public void setOnConnectListener(IConnect listener) {
        this.scanListener = listener;
    }

    public void connectToDevice() { //funkcja do łączenia się z urządzeniem
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
        }
        bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
    }

    public void disconnect() { //funkcja do rozłączenia się z urządzeniem
        bluetoothGatt.disconnect();
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() { //wywołanie zwrotne w przypadku pomyślnego połączenia

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) { //funkcja wywoływana podczas zmianu stanu połączenia z urządzeniem
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close();
                if (scheduler != null && !scheduler.isShutdown()) {
                    scheduler.shutdown();
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) { //funckja wywoływana gdy zostaną odnalezione charakterystyki urządzenia
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
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) { //funcja wywoływana gdy charakterystyka (dane) urządzenia ulegają zmianie
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] receivedBytes = characteristic.getValue();
            readingGlobal = ByteBuffer.wrap(receivedBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
    };

    private void sendBroadcast(float data) { //funkcja rozsylajaca nowo zmierzona wartosc wilgotnosci do innych aktywnosci
        Intent intent = new Intent("DATA_RECEIVED");
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }
}