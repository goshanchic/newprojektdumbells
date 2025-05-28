package com.example.a3aaaa;

import android.content.Context;
import android.util.Log;
import android.bluetooth.le.ScanResult;

import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import android.bluetooth.BluetoothGattCharacteristic;  // <- Обязательно
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class BluetoothHandler {

    private static final String TAG = "BluetoothHandler";
    private static BluetoothHandler instance;

    private final BluetoothCentralManager central;
    private AccelDataListener listener;

    // UUID платы
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHAR_UUID    = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    public interface AccelDataListener {
        void onAccelDataReceived(float x, float y, float z);
    }

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    private BluetoothHandler(Context context) {
        central = new BluetoothCentralManager(context, centralCallback, new android.os.Handler());
    }

    public void setAccelDataListener(AccelDataListener l) {
        listener = l;
    }

    public void startScanning() {
        central.scanForPeripheralsWithServices(new UUID[]{ SERVICE_UUID });
        Log.i(TAG, "Start scan");
    }

    public void stopScanning() {
        central.stopScan();
        Log.i(TAG, "Stop scan");
    }

    public void disconnectAll() {
        for (BluetoothPeripheral p : central.getConnectedPeripherals()) {
            p.cancelConnection();
            Log.i(TAG, "Disconnecting from " + p.getName());
        }
    }

    private final BluetoothCentralManagerCallback centralCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            if ("MPU6050_BLE".equals(peripheral.getName())) {
                central.stopScan();
                central.connectPeripheral(peripheral, peripheralCallback);
                Log.i(TAG, "Connecting to " + peripheral.getName());
            }
        }
        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            Log.i(TAG, "Connected");
        }
        @Override
        public void onConnectionFailed(BluetoothPeripheral peripheral, HciStatus status) {
            Log.e(TAG, "Connection failed: " + status);
        }
        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, HciStatus status) {
            Log.i(TAG, "Disconnected, retry...");
            central.autoConnectPeripheral(peripheral, peripheralCallback);
        }
        @Override
        public void onScanFailed(ScanFailure failure) {
            Log.e(TAG, "Scan failed: " + failure);
        }
    };

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
            peripheral.setNotify(SERVICE_UUID, CHAR_UUID, true);
            Log.i(TAG, "Notifications enabled");
        }
        @Override
        public void onCharacteristicUpdate(BluetoothPeripheral peripheral,
                                           byte[] value,
                                           BluetoothGattCharacteristic characteristic,
                                           GattStatus status) {
            if (status != GattStatus.SUCCESS || listener == null) return;
            if (!CHAR_UUID.equals(characteristic.getUuid()) || value.length < 36) return;

            ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
            buf.getFloat(); buf.getFloat(); buf.getFloat(); // gyroX,Y,Z
            float x = buf.getFloat();
            float y = buf.getFloat();
            float z = buf.getFloat();
            // пропускаем posX,Y,Z
            listener.onAccelDataReceived(x, y, z);
        }
    };
}





/*package com.example.a3aaaa;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BluetoothHandler {

    private static final UUID ACCEL_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID ACCEL_DATA_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");


    private BluetoothCentralManager central;
    private static BluetoothHandler instance;
    private final Context context;
    private final Handler handler = new Handler();
    private AccelDataListener dataListener;

    public void setAccelDataListener(AccelDataListener listener) {
        this.dataListener = listener;
    }

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            peripheral.setNotify(ACCEL_SERVICE_UUID, ACCEL_DATA_CHAR_UUID, true);
            Log.i("BluetoothHandler", "Notifications enabled for ACCEL_DATA_CHAR");
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral,
                                           @NotNull byte[] value,
                                           @NotNull BluetoothGattCharacteristic characteristic,
                                           @NotNull GattStatus status) {
            if (status != GattStatus.SUCCESS || dataListener == null) return;

            if (characteristic.getUuid().equals(ACCEL_DATA_CHAR_UUID)) {
                if (value.length == 36) {
                    BluetoothBytesParser parser = new BluetoothBytesParser(value);


                } else {
                    Log.e("BluetoothHandler", "Invalid data length: " + value.length);
                }
            }
        }
    };

    private final BluetoothCentralManagerCallback centralCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            if (peripheral.getName() != null && peripheral.getName().contains("ESP32_Accel")) {
                central.stopScan();
                central.connectPeripheral(peripheral, peripheralCallback);
                Log.i("BluetoothHandler", "Connecting to: " + peripheral.getName());
            }
        }

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Log.i("BluetoothHandler", "Connected to: " + peripheral.getName());
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            Log.e("BluetoothHandler", "Connection failed to " + peripheral.getName() + ", status: " + status);
            handler.postDelayed(() -> startScanning(), 3000);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            Log.i("BluetoothHandler", "Disconnected from: " + peripheral.getName());
            handler.postDelayed(() -> central.autoConnectPeripheral(peripheral, peripheralCallback), 5000);
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            Log.e("BluetoothHandler", "Scan failed: " + scanFailure);
            handler.postDelayed(() -> startScanning(), 3000);
        }
    };

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    private BluetoothHandler(Context context) {
        this.context = context;
        this.central = new BluetoothCentralManager(context, centralCallback, new Handler());
    }

    public void startScanning() {
        if (central != null) {
            Log.i("BluetoothHandler", "Starting BLE scan...");
            central.scanForPeripheralsWithServices(new UUID[]{ACCEL_SERVICE_UUID});
        }
    }

    public void stopScanning() {
        if (central != null) {
            central.stopScan();
            Log.i("BluetoothHandler", "BLE scan stopped");
        }
    }

    public void disconnect() {
        if (central != null) {
            for (BluetoothPeripheral peripheral : central.getConnectedPeripherals()) {
                peripheral.cancelConnection();
                Log.i("BluetoothHandler", "Disconnected from: " + peripheral.getName());
            }
        }
    }
}

 */

/* package com.example.a3aaaa;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;
import com.welie.blessed.ScanFailure;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class BluetoothHandler {
    // UUID для сервиса и характеристики акселерометра ESP32
    private static final UUID ACCEL_SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID ACCEL_DATA_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID CCCD_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // Интерфейс для передачи данных акселерометра
    public interface AccelDataListener {
        void onAccelDataReceived(float x, float y, float z,float accelx, float accely, float accelz, float posx, float posy, float posz );
    }

    private BluetoothCentralManager central;
    private static BluetoothHandler instance;
    private final Context context;
    private final Handler handler = new Handler();
    private AccelDataListener dataListener;

    public void setAccelDataListener(AccelDataListener listener) {
        this.dataListener = listener;
    }

    // Callback для периферийных устройств
    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {
            // Включаем уведомления для характеристики акселерометра
            peripheral.setNotify(ACCEL_SERVICE_UUID, ACCEL_DATA_CHAR_UUID, true);
            Log.i("BluetoothHandler", "Notifications enabled for ACCEL_DATA_CHAR");
        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral,
                                           @NotNull byte[] value,
                                           @NotNull BluetoothGattCharacteristic characteristic,
                                           @NotNull GattStatus status) {
            if (status != GattStatus.SUCCESS || dataListener == null) return;

            if (characteristic.getUuid().equals(ACCEL_DATA_CHAR_UUID)) {
                // Парсим данные: ожидаем 12 байт (3 значения float)
                if (value.length == 36) {
                    BluetoothBytesParser parser = new BluetoothBytesParser(value);
                    float x = parser.getFloatValue(0);
                    float y = parser.getFloatValue(0);
                    float z = parser.getFloatValue(0); // углы
                    float accelx = parser.getFloatValue(0);
                    float accely = parser.getFloatValue(0);
                    float accez = parser.getFloatValue(0);
                    float posx = parser.getFloatValue(0); // ускорение
                    float posy = parser.getFloatValue(0) ;
                    float posz =  parser.getFloatValue(0); // позиция

                    public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
                        if (status != GattStatus.SUCCESS) return;
                    }
                    UUID characteristicUUID = characteristic.getUuid();
                    BluetoothBytesParser parser = new BluetoothBytesParser(value);
                    // Передаем данные слушателю
                    if (characteristicUUID.equals(BLOOD_PRESSURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                        BloodPressureMeasurement measurement = new BloodPressureMeasurement(value);
                        Intent intent = new Intent(MEASUREMENT_BLOODPRESSURE);
                        intent.putExtra(MEASUREMENT_BLOODPRESSURE_EXTRA, measurement);
                        sendMeasurement(intent, peripheral);
                        Timber.d("%s", measurement);
                    }
                } else {
                    Log.e("BluetoothHandler", "Invalid data length: " + value.length);
                }
            }
        }
    };



    // Callback для центрального устройства
    private final BluetoothCentralManagerCallback centralCallback = new BluetoothCentralManagerCallback() {
        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {
            // Подключаемся к ESP32 (ищем по имени или сервису)
            if (peripheral.getName() != null && peripheral.getName().contains("ESP32_Accel")) {
                central.stopScan();
                central.connectPeripheral(peripheral, peripheralCallback);
                Log.i("BluetoothHandler", "Connecting to: " + peripheral.getName());
            }
        }

        @Override
        public void onConnectedPeripheral(@NotNull BluetoothPeripheral peripheral) {
            Log.i("BluetoothHandler", "Connected to: " + peripheral.getName());
        }

        @Override
        public void onConnectionFailed(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            Log.e("BluetoothHandler", "Connection failed to " + peripheral.getName() + ", status: " + status);
            // Повторяем сканирование через 3 секунды
            handler.postDelayed(() -> startScanning(), 3000);
        }

        @Override
        public void onDisconnectedPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull HciStatus status) {
            Log.i("BluetoothHandler", "Disconnected from: " + peripheral.getName());
            // Попытка переподключения через 5 секунд
            handler.postDelayed(() -> central.autoConnectPeripheral(peripheral, peripheralCallback), 5000);
        }

        @Override
        public void onScanFailed(@NotNull ScanFailure scanFailure) {
            Log.e("BluetoothHandler", "Scan failed: " + scanFailure);
            // Повторяем сканирование через 3 секунды
            handler.postDelayed(() -> startScanning(), 3000);
        }
    };

    public static synchronized BluetoothHandler getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHandler(context.getApplicationContext());
        }
        return instance;
    }

    private BluetoothHandler(Context context) {
        this.context = context;
        this.central = new BluetoothCentralManager(context, centralCallback, new Handler());
    }

    public void startScanning() {
        if (central != null) {
            Log.i("BluetoothHandler", "Starting BLE scan...");
            // Сканируем устройства с нашим сервисом акселерометра
            central.scanForPeripheralsWithServices(new UUID[]{ACCEL_SERVICE_UUID});
        }
    }

    public void stopScanning() {
        if (central != null) {
            central.stopScan();
            Log.i("BluetoothHandler", "BLE scan stopped");
        }
    }

    public void disconnect() {
        if (central != null) {
            central.getConnectedPeripherals().forEach(peripheral -> {
                peripheral.cancelConnection();
                Log.i("BluetoothHandler", "Disconnected from: " + peripheral.getName());
            });
        }
    }
}

 */
