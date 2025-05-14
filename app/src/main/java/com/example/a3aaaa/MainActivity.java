package com.example.a3aaaa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    // Константы
    private static final int VIBRATE_PERMISSION_CODE = 1;
    private static final int BLUETOOTH_PERMISSION_CODE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("beb5433e-36e1-4688-b775-ea07361b2688");
    private static final UUID CCCD_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // UI элементы
    private GraphView graphView;
    private Button statusButton;

    // Обработчики
    private final Handler handler = new Handler();
    private MediaPlayer correctSound, errorSound;
    private int correctCount = 0, errorCount = 0;
    private float accelX = 0, accelY = 0, accelZ = 9.8f;
    private boolean isCorrect = true;
    private final float[] movementHistory = new float[10];
    private int historyIndex = 0;
    private long exerciseStartTime;

    // BLE
    private BluetoothGatt bluetoothGatt;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация
        graphView = findViewById(R.id.graphView);
        statusButton = findViewById(R.id.statusButton);
        checkPermissions();
        initializeSounds();
        exerciseStartTime = System.currentTimeMillis();

        findViewById(R.id.backButton).setOnClickListener(v -> showStatisticsAndFinish());

        startUpdates();
        initBleConnection();
    }

    // ==================== BLE МЕТОДЫ ====================
    @SuppressLint("MissingPermission")
    private void initBleConnection() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast("Bluetooth не поддерживается");
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            startBleScan();
        }
    }

    @SuppressLint("MissingPermission")
    private void startBleScan() {
        // Здесь должна быть реализация сканирования BLE устройств
        // После нахождения устройства вызываем connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                    BLUETOOTH_PERMISSION_CODE
            );
            return;
        }
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        showToast("Подключаемся...");
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> showToast("Устройство подключено"));
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> showToast("Устройство отключено"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service != null) {
                    BluetoothGattCharacteristic characteristic =
                            service.getCharacteristic(CHARACTERISTIC_UUID);
                    if (characteristic != null) {
                        enableNotifications(gatt, characteristic);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            processBleData(data);
        }
    };

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic) {
        gatt.setCharacteristicNotification(characteristic, true);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }
    }

    private void processBleData(byte[] data) {
        if (data != null && data.length >= 12) {
            accelX = bytesToFloat(data, 0);
            accelY = bytesToFloat(data, 4);
            accelZ = bytesToFloat(data, 8);
        }
    }

    private float bytesToFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(
                (bytes[offset] & 0xFF) |
                        ((bytes[offset+1] & 0xFF) << 8) |
                        ((bytes[offset+2] & 0xFF) << 16) |
                        ((bytes[offset+3] & 0xFF) << 24)
        );
    }

    // ==================== ОРИГИНАЛЬНЫЕ МЕТОДЫ ====================
    private void startUpdates() {
        handler.postDelayed(() -> {
            graphView.updateData(accelX, accelY, accelZ);
            handler.postDelayed(this::startUpdates, 100);
        }, 100);

        handler.postDelayed(() -> {
            checkExerciseQuality();
            handler.postDelayed(this::startUpdates, 1000);
        }, 1000);
    }

    private void checkExerciseQuality() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xyMagnitude = (float) Math.sqrt(accelX*accelX + accelY*accelY);
        float variability = calculateMovementVariability();

        isCorrect = zDiff < 2.0f && xyMagnitude > 0.5f && xyMagnitude < 2.5f && variability < 1.0f;

        if(isCorrect) {
            correctCount++;
        } else {
            errorCount++;
            provideFeedback();
        }
        updateStatusButton();
    }

    private float calculateMovementVariability() {
        float sum = 0, sumSq = 0;
        for (float v : movementHistory) {
            sum += v;
            sumSq += v * v;
        }
        float mean = sum / movementHistory.length;
        return (float) Math.sqrt(sumSq/movementHistory.length - mean*mean);
    }

    private void updateStatusButton() {
        runOnUiThread(() -> {
            statusButton.setText(isCorrect ?
                    "✓ Правильно (" + correctCount + ")" :
                    "✗ Ошибка (" + errorCount + ")");
            statusButton.setBackgroundColor(isCorrect ?
                    Color.parseColor("#4CAF50") :
                    Color.parseColor("#F44336"));
        });
    }

    private void provideFeedback() {
        runOnUiThread(() -> {
            safeVibrate();
            playErrorSound();
            showToast(generateAdvice());
        });
    }

    private String generateAdvice() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xyMagnitude = (float) Math.sqrt(accelX*accelX + accelY*accelY);
        if (zDiff > 3.0f) return "Держите спину ровнее";
        if (xyMagnitude > 2.5f) return "Контролируйте амплитуду";
        if (xyMagnitude < 0.5f) return "Увеличьте амплитуду";
        return "Скорректируйте положение тела";
    }

    private void showStatisticsAndFinish() {
        long durationSec = (System.currentTimeMillis() - exerciseStartTime)/1000;
        showToast(String.format(
                "Статистика:\nВремя: %d мин %d сек\nПравильно: %d\nОшибок: %d",
                durationSec/60, durationSec%60, correctCount, errorCount
        ));
        finish();
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    VIBRATE_PERMISSION_CODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        BLUETOOTH_PERMISSION_CODE);
            }
        }
    }

    private void initializeSounds() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct);
            errorSound = MediaPlayer.create(this, R.raw.error);
            if (correctSound != null) correctSound.setVolume(0.7f, 0.7f);
            if (errorSound != null) errorSound.setVolume(0.7f, 0.7f);
        } catch (Exception e) {
            showToast("Ошибка загрузки звуков");
        }
    }

    private void safeVibrate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(150);
                }
            }
        }
    }

    private void playErrorSound() {
        if (errorSound != null) {
            try {
                if (errorSound.isPlaying()) errorSound.seekTo(0);
                errorSound.start();
            } catch (Exception e) {
                showToast("Ошибка воспроизведения звука");
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        releaseMediaPlayers();
        disconnectBluetooth();
    }

    private void releaseMediaPlayers() {
        if (correctSound != null) correctSound.release();
        if (errorSound != null) errorSound.release();
    }

    @SuppressLint("MissingPermission")
    private void disconnectBluetooth() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                startBleScan();
            } else {
                showToast("Bluetooth должен быть включен");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBleConnection();
            }
        }
    }
}
/*package com.example.a3aaaa;

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    // Константы для разрешений
    private static final int VIBRATE_PERMISSION_CODE = 1;
    private static final int BLUETOOTH_PERMISSION_CODE = 2;

    // UI элементы
    private GraphView graphView;
    private Button statusButton;

    // Обработчики
    private final Handler handler = new Handler();

    // Звуки
    private MediaPlayer correctSound, errorSound;

    // Статистика
    private int correctCount = 0;
    private int errorCount = 0;
    private long exerciseStartTime;

    // Данные с датчиков
    private float accelX = 0;
    private float accelY = 0;
    private float accelZ = 9.8f;
    private boolean isCorrect = true;

    // Анализ движений
    private final float[] movementHistory = new float[10];
    private int historyIndex = 0;

    // BLE
    private BluetoothGatt bluetoothGatt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Проверка разрешений
        checkPermissions();

        // Инициализация UI
        graphView = findViewById(R.id.graphView);
        statusButton = findViewById(R.id.statusButton);

        // Инициализация звуков
        initializeSounds();

        exerciseStartTime = System.currentTimeMillis();

        findViewById(R.id.backButton).setOnClickListener(v -> showStatisticsAndFinish());

        startUpdates();
    }

    private void checkPermissions() {
        // Проверка разрешения на вибрацию
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.VIBRATE},
                    VIBRATE_PERMISSION_CODE);
        }

        // Проверка Bluetooth разрешений для Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_SCAN
                        },
                        BLUETOOTH_PERMISSION_CODE);
            }
        }
    }

    private void initializeSounds() {
        try {
            correctSound = MediaPlayer.create(this, R.raw.correct);
            errorSound = MediaPlayer.create(this, R.raw.error);
            if (correctSound != null) correctSound.setVolume(0.7f, 0.7f);
            if (errorSound != null) errorSound.setVolume(0.7f, 0.7f);
        } catch (Exception e) {
            showToast("Ошибка загрузки звуков");
        }
    }

    private void startUpdates() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                graphView.updateData(accelX, accelY, accelZ);
                handler.postDelayed(this, 100);
            }
        }, 100);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkExerciseQuality();
                handler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void checkExerciseQuality() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xyMagnitude = (float) Math.sqrt(accelX*accelX + accelY*accelY);
        float variability = calculateMovementVariability();

        isCorrect = zDiff < 2.0f && xyMagnitude > 0.5f && xyMagnitude < 2.5f && variability < 1.0f;

        if(isCorrect) {
            correctCount++;
        } else {
            errorCount++;
            provideFeedback();
        }

        updateStatusButton();
    }

    private float calculateMovementVariability() {
        float sum = 0, sumSq = 0;
        for (float v : movementHistory) {
            sum += v;
            sumSq += v * v;
        }
        float mean = sum / movementHistory.length;
        return (float) Math.sqrt(sumSq / movementHistory.length - mean * mean);
    }

    private void updateStatusButton() {
        runOnUiThread(() -> {
            statusButton.setText(isCorrect ?
                    "✓ Правильно (" + correctCount + ")" :
                    "✗ Ошибка (" + errorCount + ")");
            statusButton.setBackgroundColor(isCorrect ?
                    Color.parseColor("#4CAF50") :
                    Color.parseColor("#F44336"));
        });
    }

    private void provideFeedback() {
        runOnUiThread(() -> {
            safeVibrate();
            playErrorSound();
            showToast(generateAdvice());
        });
    }

    private void safeVibrate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(150);
                }
            }
        }
    }

    private void playErrorSound() {
        if (errorSound != null) {
            try {
                if (errorSound.isPlaying()) {
                    errorSound.seekTo(0);
                }
                errorSound.start();
            } catch (Exception e) {
                showToast("Ошибка воспроизведения звука");
            }
        }
    }

    private String generateAdvice() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xyMagnitude = (float) Math.sqrt(accelX*accelX + accelY*accelY);
        float variability = calculateMovementVariability();

        if (zDiff > 3.0f) return "Держите спину ровнее";
        if (xyMagnitude > 2.5f) return "Контролируйте амплитуду";
        if (variability > 1.2f) return "Выполняйте медленнее";
        if (xyMagnitude < 0.5f) return "Увеличьте амплитуду";
        return "Скорректируйте положение тела";
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showStatisticsAndFinish() {
        long durationSec = (System.currentTimeMillis() - exerciseStartTime) / 1000;
        String stats = String.format(
                "Статистика:\nВремя: %d мин %d сек\nПравильно: %d\nОшибок: %d",
                durationSec / 60, durationSec % 60, correctCount, errorCount
        );
        showToast(stats);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        releaseMediaPlayers();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        disconnectBluetooth();
    }

    private void releaseMediaPlayers() {
        if (correctSound != null) {
            correctSound.release();
            correctSound = null;
        }
        if (errorSound != null) {
            errorSound.release();
            errorSound = null;
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void disconnectBluetooth() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }
}

 */
