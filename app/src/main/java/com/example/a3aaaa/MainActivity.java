package com.example.a3aaaa;

import android.Manifest;
import android.content.Context;              // <- Добавлено
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BluetoothHandler.AccelDataListener {

    private static final int VIBRATE_PERMISSION_CODE    = 1;
    private static final int BLUETOOTH_PERMISSION_CODE = 2;
    private static final int REQUEST_ENABLE_BT         = 3;

    private GraphView graphView;
    private Button    statusButton;

    private MediaPlayer correctSound, errorSound;
    private final Handler handler = new Handler();

    private int   correctCount = 0;
    private int   errorCount   = 0;
    private long  startTime;

    private float accelX = 0, accelY = 0, accelZ = 9.8f;
    private final float[] history = new float[10];
    private int historyIdx = 0;

    private BluetoothHandler btHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkAllPermissions();

        graphView    = findViewById(R.id.graphView);
        statusButton = findViewById(R.id.statusButton);
        initializeSounds();

        startTime = System.currentTimeMillis();

        btHandler = BluetoothHandler.getInstance(this);
        btHandler.setAccelDataListener(this);

        findViewById(R.id.backButton).setOnClickListener(v -> showStatsAndFinish());

        handler.post(this::scanBleAndStartUpdates);
    }

    private void scanBleAndStartUpdates() {
        if (checkBlePermissions()) {
            btHandler.startScanning();
        }
        startUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        btHandler.stopScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        btHandler.disconnectAll();
        // вместо несуществующего releaseSounds()
        if (correctSound != null) correctSound.release();
        if (errorSound   != null) errorSound.release();
    }

    @Override
    public void onAccelDataReceived(float x, float y, float z) {
        accelX = x; accelY = y; accelZ = z;
        history[historyIdx] = (float)Math.sqrt(x*x + y*y + z*z);
        historyIdx = (historyIdx + 1) % history.length;
    }

    private void startUpdates() {
        handler.postDelayed(() -> {
            graphView.updateData(accelX, accelY, accelZ);
            startUpdates();
        }, 100);

        handler.postDelayed(this::checkForm, 1000);
    }

    private void checkForm() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xy    = (float)Math.sqrt(accelX*accelX + accelY*accelY);
        float var   = calcVariability();

        boolean ok = zDiff < 2.0f && xy > 0.5f && xy < 2.5f && var < 1.0f;
        if (ok) correctCount++;
        else { errorCount++; giveFeedback(); }

        runOnUiThread(() -> {
            statusButton.setText(ok
                    ? String.format(Locale.getDefault(), "✓ Правильно (%d)", correctCount)
                    : String.format(Locale.getDefault(), "✗ Ошибка (%d)", errorCount));
            statusButton.setBackgroundColor(ok
                    ? Color.parseColor("#4CAF50")
                    : Color.parseColor("#F44336"));
        });
    }

    private float calcVariability() {
        float sum=0, sumSq=0;
        for (float v : history) { sum+=v; sumSq+=v*v; }
        float m = sum/history.length;
        return (float)Math.sqrt(sumSq/history.length - m*m);
    }

    private void giveFeedback() {
        safeVibrate();
        playError();
        showToast(generateTip());
    }

    private String generateTip() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xy    = (float)Math.sqrt(accelX*accelX + accelY*accelY);
        if (zDiff > 3.0f)   return "Держите спину ровнее";
        if (xy   > 2.5f)    return "Контролируйте амплитуду";
        if (xy   < 0.5f)    return "Увеличьте амплитуду";
        return "Скорректируйте положение тела";
    }

    private void showStatsAndFinish() {
        long secs = (System.currentTimeMillis() - startTime)/1000;
        String stats = String.format(Locale.getDefault(),
                "Статистика:\nВремя: %d мин %d сек\nПравильно: %d\nОшибок: %d",
                secs/60, secs%60, correctCount, errorCount);
        showToast(stats);
        finish();
    }

    private void checkAllPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.VIBRATE },
                    VIBRATE_PERMISSION_CODE);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)   != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)!= PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT },
                        BLUETOOTH_PERMISSION_CODE);
            }
        }
        BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();
        if (bt!=null && !bt.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
        }
    }

    private boolean checkBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)   == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)== PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void initializeSounds() {
        correctSound = MediaPlayer.create(this, R.raw.correct);
        errorSound   = MediaPlayer.create(this, R.raw.error);
        if (correctSound!=null) correctSound.setVolume(0.7f,0.7f);
        if (errorSound  !=null) errorSound.setVolume(0.7f,0.7f);
    }

    private void safeVibrate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE)
                == PackageManager.PERMISSION_GRANTED) {
            Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
            if (v!=null) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
                    v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                else
                    v.vibrate(150);
            }
        }
    }

    private void playError() {
        if (errorSound!=null) {
            if (errorSound.isPlaying()) errorSound.seekTo(0);
            errorSound.start();
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,@NonNull int[] grants) {
        super.onRequestPermissionsResult(req, perms, grants);
        if (req == BLUETOOTH_PERMISSION_CODE && checkBlePermissions()) {
            btHandler.startScanning();
        }
    }
}




/*package com.example.a3aaaa;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements BluetoothHandler.AccelDataListener {
    // Константы для разрешений
    private static final int VIBRATE_PERMISSION_CODE = 1;
    private static final int BLUETOOTH_PERMISSION_CODE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // UI элементы
    private GraphView graphView;
    private Button statusButton;

    // Обработчики
    private final Handler handler = new Handler();
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

    // Bluetooth
    private BluetoothHandler bluetoothHandler;

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

        // Инициализация Bluetooth
        bluetoothHandler = BluetoothHandler.getInstance(this);
        bluetoothHandler.setAccelDataListener(this);

        findViewById(R.id.backButton).setOnClickListener(v -> showStatisticsAndFinish());

        startUpdates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Начинаем сканирование BLE устройств при возобновлении активности
        if (checkBluetoothPermissions()) {
            bluetoothHandler.startScanning();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Останавливаем сканирование при паузе
        bluetoothHandler.stopScanning();
    }

    // ==================== BluetoothHandler.AccelDataListener ====================
    @Override
    public void onAccelDataReceived(float x, float y, float z,
                                    float accelx, float accely, float accelz,
                                    float posx, float posy, float posz) {
        // Обновляем данные акселерометра
        accelX = accelx;
        accelY = accely;
        accelZ = accelz;

        // Добавляем данные в историю для анализа
        movementHistory[historyIndex] = (float) Math.sqrt(accelX*accelX + accelY*accelY + accelZ*accelZ);
        historyIndex = (historyIndex + 1) % movementHistory.length;
    }

    // ==================== ОСНОВНЫЕ МЕТОДЫ ====================
    private void startUpdates() {
        // Обновление графика каждые 100 мс
        handler.postDelayed(() -> {
            graphView.updateData(accelX, accelY, accelZ);
            handler.postDelayed(this::startUpdates, 100);
        }, 100);

        // Проверка качества выполнения упражнения каждую секунду
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

    private String generateAdvice() {
        float zDiff = Math.abs(accelZ - 9.8f);
        float xyMagnitude = (float) Math.sqrt(accelX*accelX + accelY*accelY);

        if (zDiff > 3.0f) return "Держите спину ровнее";
        if (xyMagnitude > 2.5f) return "Контролируйте амплитуду";
        if (xyMagnitude < 0.5f) return "Увеличьте амплитуду";
        return "Скорректируйте положение тела";
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================
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

        // Проверка включенного Bluetooth
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
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
        bluetoothHandler.disconnect();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                bluetoothHandler.startScanning();
            } else {
                showToast("Для работы приложения требуется Bluetooth");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BLUETOOTH_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                bluetoothHandler.startScanning();
            } else {
                showToast("Для работы приложения требуются Bluetooth разрешения");
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
