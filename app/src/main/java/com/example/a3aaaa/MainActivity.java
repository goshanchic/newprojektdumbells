package com.example.a3aaaa;

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
/*package com.example.a3aaaa;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final String TAG = "BLE_APP";

    private LineChart chart;
    private TextView feedbackText;
    private LinearLayout feedbackCard;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BleService bleService;

    private final UUID SERVICE_UUID = UUID.fromString("4FAFC201-1FB5-459E-8FCC-C5C9C331914B");
    private final UUID CHARACTERISTIC_UUID = UUID.fromString("BEB5483E-36E1-4688-B7F5-EA07361B26A8");

    private final List<Entry> xEntries = new ArrayList<>();
    private final List<Entry> yEntries = new ArrayList<>();
    private final List<Entry> zEntries = new ArrayList<>();
    private float timeIndex = 0;
    private float lastX, lastY, lastZ;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {

                    String deviceName = device.getName();
                    if (deviceName != null && deviceName.contains("ESP32")) {
                        bleScanner.stopScan(this);
                        bleService.connect(MainActivity.this, device);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices();
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Необходимо разрешение BLUETOOTH_CONNECT",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(SERVICE_UUID);
                if (service == null) {
                    Log.w(TAG, "Сервис не найден");
                    return;
                }

                BluetoothGattCharacteristic characteristic = service.getCharacteristic(CHARACTERISTIC_UUID);
                if (characteristic == null) {
                    Log.w(TAG, "Характеристика не найдена");
                    return;
                }

                try {
                    gatt.setCharacteristicNotification(characteristic, true);
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    );
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException: " + e.getMessage());
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            parseData(characteristic.getValue());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkBluetoothSupport();
        setupChart();
        checkPermissions();
    }

    private void initViews() {
        chart = findViewById(R.id.chart);
        feedbackText = findViewById(R.id.feedbackText);
        feedbackCard = findViewById(R.id.feedbackCard);
    }

    private void checkBluetoothSupport() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Устройство не поддерживает Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Включите Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        bleService = new BleService();
        bleService.setGattCallback(gattCallback);
    }

    private void setupChart() {
        LineDataSet xDataSet = new LineDataSet(xEntries, "X");
        xDataSet.setColor(0xFFFF0000);
        xDataSet.setDrawCircles(false);

        LineDataSet yDataSet = new LineDataSet(yEntries, "Y");
        yDataSet.setColor(0xFF00FF00);
        yDataSet.setDrawCircles(false);

        LineDataSet zDataSet = new LineDataSet(zEntries, "Z");
        zDataSet.setColor(0xFF0000FF);
        zDataSet.setDrawCircles(false);

        LineData data = new LineData(xDataSet, yDataSet, zDataSet);
        chart.setData(data);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    REQUEST_BLUETOOTH_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void startBleScan() {
        if (!checkPermissions()) return;

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().build());

        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED) {
                bleScanner.startScan(filters, settings, scanCallback);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    private void parseData(byte[] data) {
        runOnUiThread(() -> {
            try {
                String[] values = new String(data, StandardCharsets.UTF_8).trim().split(",");
                if (values.length < 3) {
                    Log.w(TAG, "Недостаточно данных: " + values.length);
                    return;
                }

                lastX = Float.parseFloat(values[0]);
                lastY = Float.parseFloat(values[1]);
                lastZ = Float.parseFloat(values[2]);

                updateChart();
                giveFeedback();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка парсинга данных: " + e.getMessage());
            }
        });
    }

    private void updateChart() {
        xEntries.add(new Entry(timeIndex, lastX));
        yEntries.add(new Entry(timeIndex, lastY));
        zEntries.add(new Entry(timeIndex, lastZ));
        timeIndex += 0.1f;

        if (xEntries.size() > 100) {
            xEntries.remove(0);
            yEntries.remove(0);
            zEntries.remove(0);
        }

        chart.getData().notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void giveFeedback() {
        float threshold = 1.0f;
        boolean isCorrect = Math.abs(lastX) < threshold &&
                Math.abs(lastY) < threshold &&
                Math.abs(lastZ) < threshold;

        feedbackCard.setBackgroundColor(getResources().getColor(
                isCorrect ? android.R.color.holo_green_light : android.R.color.holo_red_light
        ));
        feedbackText.setText(isCorrect ? "✅ Правильно" : "❌ Неправильно");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBleScan();
            } else {
                Toast.makeText(this, "Необходимы разрешения для работы BLE", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bleScanner != null) {
                bleScanner.stopScan(scanCallback);
            }
            bleService.disconnect();
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }
} /*

/*
package com.example.a3aaaa;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private GraphView graphView;
    private TextView feedbackText;
    private LinearLayout feedbackCard;

    private Handler dataHandler, checkHandler;
    private Runnable dataRunnable, checkRunnable;

    private float lastX = 0, lastY = 0, lastZ = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graphView = findViewById(R.id.graphView);
        feedbackText = findViewById(R.id.feedbackText);
        feedbackCard = findViewById(R.id.feedbackCard);

        ApiService apiService = RetrofitClient.getApiService();

        // Получение данных каждые 100 мс
        dataHandler = new Handler();
        dataRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLiveData(apiService);
                dataHandler.postDelayed(this, 100);
            }
        };
        dataHandler.post(dataRunnable);

        // Проверка выполнения раз в 10 секунд
        checkHandler = new Handler();
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                validateExercise();
                checkHandler.postDelayed(this, 10000);
            }
        };
        checkHandler.post(checkRunnable);
    }

    private void fetchLiveData(ApiService apiService) {
        apiService.getExerciseData().enqueue(new Callback<ExerciseData>() {
            @Override
            public void onResponse(Call<ExerciseData> call, Response<ExerciseData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ExerciseData data = response.body();
                    lastX = data.getX();
                    lastY = data.getY();
                    lastZ = data.getZ();

                    graphView.addPoint(lastX, lastY, lastZ);
                } else {
                    feedbackText.setText("Ошибка загрузки данных!");
                }
            }

            @Override

            public void onFailure(Call<ExerciseData> call, Throwable t) {
                feedbackText.setText("Ошибка сети: " + t.getMessage());
            }
        });
    }

    private void validateExercise() {
        float threshold = 1.0f; // можно вынести в настройку

        boolean isCorrect = Math.abs(lastX) < threshold &&
                Math.abs(lastY) < threshold &&
                Math.abs(lastZ) < threshold;

        if (isCorrect) {
            feedbackCard.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            feedbackText.setText("✅ Правильно");
        } else {
            feedbackCard.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            feedbackText.setText("❌ Неправильно");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dataHandler.removeCallbacks(dataRunnable);
        checkHandler.removeCallbacks(checkRunnable);
    }
}

 */
