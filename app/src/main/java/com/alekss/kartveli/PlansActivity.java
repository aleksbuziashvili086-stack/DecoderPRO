package com.alekss.kartveli;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class PlansActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
            Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plans);
        findViewById(R.id.plans_back).setOnClickListener(v -> finish());
        TextView toggle = findViewById(R.id.toggle_pro);
        refresh(toggle);
        toggle.setOnClickListener(v -> {
            boolean next = !Prefs.pro(this);
            Prefs.of(this).edit().putBoolean(Prefs.KEY_PRO, next).apply();
            refresh(toggle);
            Toast.makeText(this, next ? "სატესტო Pro ჩაირთო. ფული არ ჩამოგეჭრა." : "დაბრუნდი უფასო გეგმაზე.", Toast.LENGTH_SHORT).show();
        });
    }

    private void refresh(TextView toggle) {
        toggle.setText(Prefs.pro(this) ? "სატესტო Pro ჩართულია — გამორთე" : "სატესტო Pro ჩართე");
    }
}
