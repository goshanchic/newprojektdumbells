package com.example.a3aaaa;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MotionSensorData {

    public float gyroX;
    public float gyroY;
    public float gyroZ;

    public float accelX;
    public float accelY;
    public float accelZ;

    public float posX;
    public float posY;
    public float posZ;

    public static MotionSensorData fromByteArray(byte[] data) {
        MotionSensorData sensorData = new MotionSensorData();

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        sensorData.gyroX = buffer.getFloat();
        sensorData.gyroY = buffer.getFloat();
        sensorData.gyroZ = buffer.getFloat();

        sensorData.accelX = buffer.getFloat();
        sensorData.accelY = buffer.getFloat();
        sensorData.accelZ = buffer.getFloat();

        sensorData.posX = buffer.getFloat();
        sensorData.posY = buffer.getFloat();
        sensorData.posZ = buffer.getFloat();

        return sensorData;
    }

    @Override
    public String toString() {
        return "Gyro: (" + gyroX + ", " + gyroY + ", " + gyroZ + ") " +
                "Accel: (" + accelX + ", " + accelY + ", " + accelZ + ") " +
                "Pos: (" + posX + ", " + posY + ", " + posZ + ")";
    }
}


