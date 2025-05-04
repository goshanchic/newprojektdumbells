package com.example.a3aaaa;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;
import java.util.Queue;

public class GraphView extends View {
    private Paint linePaint, axisPaint, textPaint, shadowPaint, fillPaint, gridPaint;
    private Queue<Float> dataX = new LinkedList<>();
    private Queue<Float> dataY = new LinkedList<>();
    private Queue<Float> dataZ = new LinkedList<>();
    private static final int DATA_SIZE = 50;

    public GraphView(Context context) {
        super(context);
        init();
    }

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        for (int i = 0; i < DATA_SIZE; i++) {
            dataX.add(0f);
            dataY.add(0f);
            dataZ.add(9.8f);
        }
        setupPaints();
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

    public void updateData(float x, float y, float z) {
        if (dataX.size() >= DATA_SIZE) {
            dataX.poll();
            dataY.poll();
            dataZ.poll();
        }
        dataX.add(x);
        dataY.add(y);
        dataZ.add(z);
        invalidate();
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
        canvas.drawText("Ускорение, м/с²", padding / 2, height / 2, textPaint);
        canvas.restore();
    }

    private void drawGraph(Canvas canvas, Queue<Float> data, int color, int padding, int chartWidth, int chartHeight) {
        Path path = new Path();
        Path fillPath = new Path();
        float step = (float) chartWidth / (DATA_SIZE - 1);

        Float[] values = data.toArray(new Float[0]);
        float x = padding;
        float y = padding + chartHeight - (normalize(values[0]) * chartHeight);

        path.moveTo(x, y);
        fillPath.moveTo(x, padding + chartHeight);
        fillPath.lineTo(x, y);

        for (int i = 1; i < values.length; i++) {
            x = padding + step * i;
            y = padding + chartHeight - (normalize(values[i]) * chartHeight);

            path.lineTo(x, y);
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
        canvas.drawPath(path, linePaint);
    }

    private float normalize(float value) {
        return (value + 20f) / 40f;
    }

    private void setupGradient(int color, int padding, int chartHeight) {
        LinearGradient gradient = new LinearGradient(
                0, padding,
                0, padding + chartHeight,
                Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)),
                Color.argb(20, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP);
        fillPaint.setShader(gradient);
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
