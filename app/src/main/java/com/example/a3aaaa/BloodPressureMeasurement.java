package com.example.a3aaaa;

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
        x = parser.getFloatValue(0);
        y = parser.getFloatValue(0);
        z = parser.getFloatValue(0);

        // Read timestamp
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"x: %.0f, y: %.0f, z: %.0f", x, y, z);
    }
}
