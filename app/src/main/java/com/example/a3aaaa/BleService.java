/*package com.example.a3aaaa;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.util.Log;

public class BleService {
    private static final String TAG = "BLE_SERVICE";
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback gattCallback;

    public void setGattCallback(BluetoothGattCallback callback) {
        this.gattCallback = callback;
    }

    public void connect(Context context, BluetoothDevice device) {
        try {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
            bluetoothGatt = device.connectGatt(context, false, gattCallback);
            Log.d(TAG, "Попытка подключения к устройству: " + device.getName());
        } catch (SecurityException e) {
            Log.e(TAG, "Ошибка подключения: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            try {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
                Log.d(TAG, "Отключено от BLE устройства");
            } catch (SecurityException e) {
                Log.e(TAG, "Ошибка отключения: " + e.getMessage());
            }
        }
    }

    public void startScan() {
        // Реализация сканирования перенесена в MainActivity
        // для лучшего контроля разрешений
    }

    public void stopScan() {
        // Остановка сканирования теперь обрабатывается в MainActivity
    }

    public void setDataListener(GraphActivity graphActivity) {
    }

    public interface DataListener {
    }
}

 */


