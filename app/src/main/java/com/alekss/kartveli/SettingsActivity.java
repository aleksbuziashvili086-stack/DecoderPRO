package com.alekss.kartveli;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] TONES = {"მეგობრული", "ოფიციალური", "მოკლე", "მასწავლებელი"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
            Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settings_back).setOnClickListener(v -> finish());

        EditText key = findViewById(R.id.api_key);
        EditText model = findViewById(R.id.model_name);
        Spinner tone = findViewById(R.id.tone_spinner);
        Switch dark = findViewById(R.id.dark_switch);

        key.setText(Prefs.apiKey(this));
        model.setText(Prefs.model(this));
        dark.setChecked(Prefs.dark(this));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, TONES);
        tone.setAdapter(adapter);
        String savedTone = Prefs.tone(this);
        for (int i = 0; i < TONES.length; i++) {
            if (TONES[i].equals(savedTone)) tone.setSelection(i);
        }

        findViewById(R.id.save_settings).setOnClickListener(v -> {
            boolean nextDark = dark.isChecked();
            Prefs.of(this).edit()
                .putString(Prefs.KEY_API, key.getText().toString().trim())
                .putString(Prefs.KEY_MODEL, model.getText().toString().trim())
                .putString(Prefs.KEY_TONE, String.valueOf(tone.getSelectedItem()))
                .putBoolean(Prefs.KEY_DARK, nextDark)
                .apply();
            AppCompatDelegate.setDefaultNightMode(
                nextDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            Toast.makeText(this, "შენახულია", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.clear_history).setOnClickListener(v -> {
            HistoryStore.clear(this);
            Toast.makeText(this, "ისტორია წაიშალა", Toast.LENGTH_SHORT).show();
        });
    }
}
