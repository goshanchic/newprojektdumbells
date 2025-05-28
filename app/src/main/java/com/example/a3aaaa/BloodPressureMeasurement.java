/*package com.example.a3aaaa;

import androidx.annotation.NonNull;

import com.welie.blessed.BluetoothBytesParser;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;


public class BloodPressureMeasurement implements Serializable {

    @Nullable
    public Integer userID;
    public Float x;
    public Float y;
    public Float z;

    public BloodPressureMeasurement(byte[] value) {
        BluetoothBytesParser parser = new BluetoothBytesParser(value);

        // Get systolic, diastolic and mean arterial pressure
        float x = parser.getFloatValue();
        float y = parser.getFloatValue();
        float z = parser.getFloatValue();
        float accelx = parser.getFloatValue();
        float accely = parser.getFloatValue();
        float accelz = parser.getFloatValue();
        float posx = parser.getFloatValue();
        float posy = parser.getFloatValue();
        float posz = parser.getFloatValue();
        dataListener.onAccelDataReceived(x, y, z, accelx, accely, accelz, posx, posy, posz);

        // Read timestamp
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"x: %.0f, y: %.0f, z: %.0f", x, y, z);
    }
}


 */