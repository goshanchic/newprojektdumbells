package com.example.a3aaaa;

import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.welie.blessed.*;

import java.util.UUID;

public class GraphView extends View {

    private static final String TAG = "GraphViewBLE";

    private static final UUID SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");

    private final int[] dataX = new int[50];
    private final int[] dataY = new int[50];
    private final int[] dataZ = new int[50];

    private final Paint axisPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint linePaint = new Paint();
    private final Paint fillPaint = new Paint();

    private BluetoothCentralManager central;
    private BluetoothPeripheral connectedPeripheral;

    public GraphView(Context context) {
        super(context);
        init(context);
    }

    public GraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        setupPaints();
        setupBLE(context);
    }

    private void setupPaints() {
        axisPaint.setColor(Color.DKGRAY);
        axisPaint.setStrokeWidth(2);

        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1);

        textPaint.setColor(Color.GRAY);
        textPaint.setTextSize(32);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5);
        linePaint.setAntiAlias(true);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
    }

    private void setupBLE(Context context) {
        central = new BluetoothCentralManager(context, bluetoothCentralCallback, context.getMainLooper());
        central.scanForPeripherals();
    }

    private final BluetoothCentralCallback bluetoothCentralCallback = new BluetoothCentralCallback() {
        @Override
        public void onDiscoveredPeripheral(BluetoothPeripheral peripheral, ScanResult scanResult) {
            if ("MPU_6050".equals(peripheral.getName())) {
                Log.i(TAG, "Найден MPU_6050");
                central.stopScan();
                central.connectPeripheral(peripheral, peripheralCallback);
            }
        }

        @Override
        public void onConnectedPeripheral(BluetoothPeripheral peripheral) {
            Log.i(TAG, "Подключено к " + peripheral.getName());
            connectedPeripheral = peripheral;
        }

        @Override
        public void onDisconnectedPeripheral(BluetoothPeripheral peripheral, HciStatus status) {
            Log.w(TAG, "BLE отключено");
        }
    };

    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(BluetoothPeripheral peripheral) {
            peripheral.setNotify(SERVICE_UUID, CHAR_UUID, true);
        }

        @Override
        public void onNotificationReceived(BluetoothPeripheral peripheral, UUID characteristic, byte[] value) {
            String raw = new String(value).trim();
            String[] parts = raw.split(",");
            if (parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int z = Integer.parseInt(parts[2]);
                    addSensorData(x, y, z);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка парсинга данных: " + raw);
                }
            }
        }
    };

    public void addSensorData(int x, int y, int z) {
        shiftArray(dataX, x);
        shiftArray(dataY, y);
        shiftArray(dataZ, z);
        postInvalidate(); // обновить отрисовку
    }

    private void shiftArray(int[] array, int newValue) {
        System.arraycopy(array, 1, array, 0, array.length - 1);
        array[array.length - 1] = newValue;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGraph(canvas, dataX, Color.RED);
        drawGraph(canvas, dataY, Color.GREEN);
        drawGraph(canvas, dataZ, Color.BLUE);
        drawAxes(canvas);
    }

    private void drawAxes(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        // Сетка
        for (int i = 0; i <= 10; i++) {
            float y = h * i / 10f;
            canvas.drawLine(0, y, w, y, gridPaint);
        }

        for (int i = 0; i <= 10; i++) {
            float x = w * i / 10f;
            canvas.drawLine(x, 0, x, h, gridPaint);
        }

        // Оси
        canvas.drawLine(0, h / 2f, w, h / 2f, axisPaint); // ось X
        canvas.drawLine(0, 0, 0, h, axisPaint); // ось Y
    }

    private void drawGraph(Canvas canvas, int[] data, int color) {
        int w = getWidth();
        int h = getHeight();
        float stepX = (float) w / (data.length - 1);

        linePaint.setColor(color);

        Path path = new Path();
        float midY = h / 2f;

        path.moveTo(0, midY - data[0] / 4f);

        for (int i = 1; i < data.length; i++) {
            float x = i * stepX;
            float y = midY - data[i] / 4f;
            path.lineTo(x, y);
        }

        canvas.drawPath(path, linePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (connectedPeripheral != null) {
            connectedPeripheral.cancelConnection();
        }
    }
}


// успешно работает на случайных числах

/*package com.example.a3aaaa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.animation.ValueAnimator;

public class GraphView extends View {

    private Paint linePaint;
    private Paint axisPaint;
    private Paint textPaint;
    private Paint shadowPaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private int[] dataX, dataY, dataZ;
    private static final int DATA_SIZE = 20;
    private float animationPhase = 0f;
    private ValueAnimator animator;

    // Параметры для генерации данных
    private float phaseX, phaseY, phaseZ;
    private float amplitudeX = 50f;
    private float amplitudeY = 50f;
    private final float amplitudeZ = 250f;
    private int timeCounter;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dataX = new int[DATA_SIZE];
        dataY = new int[DATA_SIZE];
        dataZ = new int[DATA_SIZE];
        initializeData();
        setupPaints();
        setupAnimation();
    }

    private void setupPaints() {
        axisPaint = new Paint();
        axisPaint.setColor(Color.parseColor("#607D8B"));
        axisPaint.setStrokeWidth(2);
        axisPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#78909C"));
        textPaint.setTextSize(36);
        textPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStrokeWidth(1);

        shadowPaint = new Paint();
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(8);
        shadowPaint.setAntiAlias(true);
        shadowPaint.setAlpha(60);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);
    }

    private void setupAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1500);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            animationPhase = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void initializeData() {
        phaseX = phaseY = phaseZ = 0f;
        for (int i = 0; i < DATA_SIZE; i++) {
            dataX[i] = 250 + (int) (Math.sin(phaseX) * amplitudeX);
            dataY[i] = 250 + (int) (Math.sin(phaseY) * amplitudeY);
            dataZ[i] = (int) ((Math.sin(phaseZ) + 1) * 250);

            phaseX += 0.05;
            phaseY += 0.07;
            phaseZ += 0.1;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int padding = 80;
        int chartHeight = height - 2 * padding;
        int chartWidth = width - 2 * padding;

        canvas.drawColor(Color.parseColor("#FAFAFA"));
        drawGrid(canvas, width, height, padding, chartWidth, chartHeight);
        drawAxes(canvas, width, height, padding);

        drawGraph(canvas, dataX, Color.parseColor("#FF5722"), padding, chartWidth, chartHeight);
        drawGraph(canvas, dataY, Color.parseColor("#4CAF50"), padding, chartWidth, chartHeight);
        drawGraph(canvas, dataZ, Color.parseColor("#2196F3"), padding, chartWidth, chartHeight);

        drawAxisLabels(canvas, width, height, padding);
    }

    private void drawGrid(Canvas canvas, int width, int height, int padding, int chartWidth, int chartHeight) {
        for (int i = 0; i < 10; i++) {
            float x = padding + (chartWidth) * i / 9;
            canvas.drawLine(x, padding, x, height - padding, gridPaint);
        }

        for (int i = 0; i < 6; i++) {
            float y = padding + (chartHeight) * i / 5;
            canvas.drawLine(padding, y, width - padding, y, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas, int width, int height, int padding) {
        canvas.drawLine(padding, height - padding, width - padding, height - padding, axisPaint);
        canvas.drawLine(padding, height - padding, padding, padding, axisPaint);

        Path arrowPath = new Path();
        arrowPath.moveTo(width - padding, height - padding);
        arrowPath.lineTo(width - padding - 15, height - padding - 10);
        arrowPath.moveTo(width - padding, height - padding);
        arrowPath.lineTo(width - padding - 15, height - padding + 10);
        arrowPath.moveTo(padding, padding);
        arrowPath.lineTo(padding - 10, padding + 15);
        arrowPath.moveTo(padding, padding);
        arrowPath.lineTo(padding + 10, padding + 15);
        canvas.drawPath(arrowPath, axisPaint);
    }

    private void drawAxisLabels(Canvas canvas, int width, int height, int padding) {
        canvas.drawText("Время", width - padding - 100, height - padding / 2, textPaint);
        canvas.save();
        canvas.rotate(-90, padding / 2, height / 2);
        canvas.drawText("Значения", padding / 2, height / 2, textPaint);
        canvas.restore();
    }

    private void drawGraph(Canvas canvas, int[] data, int color, int padding, int chartWidth, int chartHeight) {
        if (data == null || data.length < 2) return;

        Path path = new Path();
        Path fillPath = new Path();
        float step = (float) chartWidth / (data.length - 1);

        float firstX = padding;
        float firstY = padding + chartHeight - (data[0] * chartHeight / 500f * animationPhase);
        path.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, padding + chartHeight);
        fillPath.lineTo(firstX, firstY);

        for (int i = 1; i < data.length; i++) {
            float x = padding + step * i;
            float y = padding + chartHeight - (data[i] * chartHeight / 500f * animationPhase);

            float prevX = padding + step * (i - 1);
            float prevY = padding + chartHeight - (data[i - 1] * chartHeight / 500f * animationPhase);
            float ctrlX = (prevX + x) / 2;

            path.cubicTo(ctrlX, prevY, ctrlX, y, x, y);
            fillPath.lineTo(x, y);
        }

        fillPath.lineTo(padding + chartWidth, padding + chartHeight);
        fillPath.close();

        setupGradient(color, padding, chartHeight);

        shadowPaint.setColor(color);
        canvas.drawPath(path, shadowPaint);
        canvas.drawPath(fillPath, fillPaint);

        linePaint = new Paint();
        linePaint.setColor(color);
        linePaint.setStrokeWidth(4);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPath(path, linePaint);
    }

    private void setupGradient(int color, int padding, int chartHeight) {
        LinearGradient gradient = new LinearGradient(
                0, padding,
                0, padding + chartHeight,
                Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(20, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);
    }

    public void updateData() {
        System.arraycopy(dataX, 1, dataX, 0, dataX.length - 1);
        System.arraycopy(dataY, 1, dataY, 0, dataY.length - 1);
        System.arraycopy(dataZ, 1, dataZ, 0, dataZ.length - 1);

        dataX[dataX.length - 1] = 250 + (int) (Math.sin(phaseX) * amplitudeX);
        dataY[dataY.length - 1] = 250 + (int) (Math.sin(phaseY) * amplitudeY);
        dataZ[dataZ.length - 1] = (int) ((Math.sin(phaseZ) + 1) * 250);

        phaseX += 0.05;
        phaseY += 0.07;
        phaseZ += 0.1;

        if (timeCounter++ > 100) {
            amplitudeX = 200f;
            amplitudeY = 200f;
        }

        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) {
            animator.cancel();
        }
    }
}

 */
