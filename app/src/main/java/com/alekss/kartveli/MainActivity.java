package com.alekss.kartveli;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
            Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_history).setOnClickListener(v ->
            startActivity(new Intent(this, HistoryActivity.class)));
        findViewById(R.id.btn_settings).setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        LinearLayout quick = findViewById(R.id.quick_row);
        addChip(quick, "დღის გეგმა", "plan");
        addChip(quick, "ემეილი", "email");
        addChip(quick, "კოდი", "code");
        addChip(quick, "სწავლა", "study");
        addChip(quick, "კრეატივი", "creative");
        addChip(quick, "გამოწერა", "plans");

        LinearLayout modes = findViewById(R.id.modes_container);
        String lastSection = "";
        for (Mode mode : Mode.all()) {
            if ("სწრაფი".equals(mode.section)) continue;
            if (!mode.section.equals(lastSection)) {
                TextView header = new TextView(this);
                header.setText(mode.section);
                header.setTextColor(0xFF9AA6C3);
                header.setTextSize(13);
                header.setTypeface(Typeface.DEFAULT_BOLD);
                header.setPadding(8, 22, 8, 8);
                modes.addView(header);
                lastSection = mode.section;
            }
            modes.addView(card(mode));
        }

        EditText input = findViewById(R.id.home_input);
        findViewById(R.id.home_send).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "ჯერ დაწერე რა გინდა", Toast.LENGTH_SHORT).show();
                return;
            }
            openChat("free", text);
        });
    }

    private void addChip(LinearLayout row, String label, String id) {
        TextView chip = new TextView(this);
        chip.setText(label);
        chip.setTextColor(0xFFF4F7FF);
        chip.setTextSize(14);
        chip.setBackgroundResource(R.drawable.bg_chip);
        chip.setPadding(28, 18, 28, 18);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 16, 8);
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> {
            if ("plans".equals(id)) startActivity(new Intent(this, PlansActivity.class));
            else openChat(id, null);
        });
        row.addView(chip);
    }

    private LinearLayout card(Mode mode) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(28, 24, 28, 24);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        card.setLayoutParams(params);

        TextView title = new TextView(this);
        title.setText(mode.title);
        title.setTextColor(0xFFF4F7FF);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText(mode.subtitle);
        sub.setTextColor(0xFF9AA6C3);
        sub.setTextSize(13);
        sub.setPadding(0, 6, 0, 0);
        card.addView(sub);
        card.setOnClickListener(v -> openChat(mode.id, null));
        return card;
    }

    private void openChat(String mode, String seed) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("mode", mode);
        if (seed != null) intent.putExtra("seed", seed);
        startActivity(intent);
    }
}
