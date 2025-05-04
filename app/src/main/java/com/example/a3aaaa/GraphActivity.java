/*package com.example.a3aaaa;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity implements BleService.DataListener {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private LineChart chart;
    private BleService bleService;

    private List<Entry> xEntries = new ArrayList<>();
    private List<Entry> yEntries = new ArrayList<>();
    private List<Entry> zEntries = new ArrayList<>();
    private float timeIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chart = findViewById(R.id.chart);
        setupChart();

        bleService = new BleService();
        bleService.setDataListener(this);

        if (checkPermissions()) {
            startBle();
        }
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    REQUEST_BLUETOOTH_PERMISSION);
            return false;
        }
        return true;
    }

    private void setupChart() {
        LineDataSet xDataSet = new LineDataSet(xEntries, "X");
        xDataSet.setColor(0xFFFF0000); // Красный
        xDataSet.setDrawCircles(false);

        LineDataSet yDataSet = new LineDataSet(yEntries, "Y");
        yDataSet.setColor(0xFF00FF00); // Зеленый
        yDataSet.setDrawCircles(false);

        LineDataSet zDataSet = new LineDataSet(zEntries, "Z");
        zDataSet.setColor(0xFF0000FF); // Синий
        zDataSet.setDrawCircles(false);

        LineData data = new LineData(xDataSet, yDataSet, zDataSet);
        chart.setData(data);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private void startBle() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Пожалуйста, включите Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        bleService.startScan();
    }


    public void onCoordinatesReceived(float x, float y, float z) {
        runOnUiThread(() -> {
            // Добавляем новые точки данных
            xEntries.add(new Entry(timeIndex, x));
            yEntries.add(new Entry(timeIndex, y));
            zEntries.add(new Entry(timeIndex, z));
            timeIndex += 0.1f;

            // Ограничиваем количество точек на графике
            if (xEntries.size() > 100) {
                xEntries.remove(0);
                yEntries.remove(0);
                zEntries.remove(0);
            }

            // Обновляем график
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
            chart.invalidate();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startBle();
            } else {
                Toast.makeText(this, "Для работы приложения необходимы разрешения Bluetooth", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleService.disconnect();
    }
}

 */

/* package com.example.a3aaaa;

import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GraphActivity extends AppCompatActivity {

    private GraphView graphView; // Наш кастомный View для графика
    private TextView feedbackText; // Текст для отображения правильности выполнения
    private Handler handler;
    private Runnable runnable;
    private List<Float> idealCoordinatesX, idealCoordinatesY, idealCoordinatesZ;
    private float threshold;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // activity_graph.xml

        graphView = findViewById(R.id.graphView); // Получаем ссылку на кастомный GraphView
        feedbackText = findViewById(R.id.feedbackText); // Для текста о правильности выполнения

        handler = new Handler();
        ApiService apiService = RetrofitClient.getApiService();

        fetchExerciseData(apiService); // Загружаем эталонные данные для координат

        runnable = new Runnable() {
            @Override
            public void run() {
                checkExercise(); // Проверяем, правильно ли выполняется упражнение
                handler.postDelayed(this, 100); // Повторяем каждую 100 миллисекунд
            }
        };
        handler.post(runnable);
    }

    private void fetchExerciseData(ApiService apiService) {
        apiService.getExerciseData().enqueue(new Callback<ExerciseData>() {
            @Override
            public void onResponse(Call<ExerciseData> call, Response<ExerciseData> response) {
                if (response.isSuccessful() && response.body() != null) {
                    idealCoordinatesX = response.body().getIdealCoordinatesX();
                    idealCoordinatesY = response.body().getIdealCoordinatesY();
                    idealCoordinatesZ = response.body().getIdealCoordinatesZ();
                    threshold = response.body().getThreshold();
                    updateGraph(); // Обновляем график с этими данными
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

    private void updateGraph() {
        // Обновляем график с новыми данными (X, Y, Z)
        graphView.updateData(idealCoordinatesX, idealCoordinatesY, idealCoordinatesZ);
    }

    private void checkExercise() {
        // Логика для проверки выполнения упражнения
        if (idealCoordinatesX == null || idealCoordinatesY == null || idealCoordinatesZ == null) {
            feedbackText.setText("Загрузка данных...");
            return;
        }

        // Проверяем текущие координаты с эталонными
        List<Float> currentCoordinatesX = graphView.getCurrentDataX();
        List<Float> currentCoordinatesY = graphView.getCurrentDataY();
        List<Float> currentCoordinatesZ = graphView.getCurrentDataZ();

        if (compareCoordinates(currentCoordinatesX, idealCoordinatesX) &&
                compareCoordinates(currentCoordinatesY, idealCoordinatesY) &&
                compareCoordinates(currentCoordinatesZ, idealCoordinatesZ)) {
            feedbackText.setText("✅ Упражнение выполнено правильно!");
        } else {
            feedbackText.setText("⚠️ Ошибка в технике!");
        }
    }

    private boolean compareCoordinates(List<Float> current, List<Float> ideal) {
        if (current.size() != ideal.size()) return false;

        for (int i = 0; i < current.size(); i++) {
            if (Math.abs(current.get(i) - ideal.get(i)) > threshold) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }
}

 */






