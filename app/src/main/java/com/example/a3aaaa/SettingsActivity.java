/*package com.example.a3aaaa;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    private Switch darkModeSwitch;
    private SeekBar fontSizeSeekBar;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        saveButton = findViewById(R.id.saveSettingsButton);

        // Загрузка сохраненных настроек
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        darkModeSwitch.setChecked(prefs.getBoolean("DarkMode", false));
        fontSizeSeekBar.setProgress(prefs.getInt("FontSize", 16));

        // Сохранение настроек
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("DarkMode", darkModeSwitch.isChecked());
            editor.putInt("FontSize", fontSizeSeekBar.getProgress());
            editor.apply();

            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show();
            finish(); // Закрываем активность
        });
    }
}

 */