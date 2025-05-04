/*// работа с акселерометром

package com.example.a3aaaa;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class SensorService implements SensorEventListener {
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final SensorDataListener listener;

    public interface SensorDataListener {
        void onSensorDataReceived(SensorData data);
    }

    public SensorService(Context context, SensorDataListener listener) {
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
    }

    public void start() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            SensorData data = new SensorData(x, y, z);
            listener.onSensorDataReceived(data);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Можно реализовать при необходимости
    }
}

 */
