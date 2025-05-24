package com.example.a3aaaa;

import android.bluetooth.BluetoothAdapter;
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
                    float posy =  parser.getFloatValue(0);
                    float posz =  parser.getFloatValue(0); // позиция


                    // Передаем данные слушателю
                    dataListener.onAccelDataReceived(float x, float y,float z)
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
