package com.alekss.kartveli;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
            Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        findViewById(R.id.history_back).setOnClickListener(v -> finish());
        LinearLayout list = findViewById(R.id.history_list);
        TextView empty = findViewById(R.id.history_empty);
        List<HistoryStore.Chat> chats = HistoryStore.list(this);
        empty.setVisibility(chats.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        SimpleDateFormat format = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
        for (HistoryStore.Chat chat : chats) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.bg_card);
            card.setPadding(28, 22, 28, 22);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            card.setLayoutParams(params);

            TextView title = new TextView(this);
            title.setText(chat.title);
            title.setTextColor(0xFFF4F7FF);
            title.setTextSize(16);
            card.addView(title);

            TextView meta = new TextView(this);
            meta.setText(Mode.find(chat.mode).title + " · " + format.format(new Date(chat.updated)));
            meta.setTextColor(0xFF9AA6C3);
            meta.setTextSize(12);
            meta.setPadding(0, 6, 0, 0);
            card.addView(meta);

            card.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("chat_id", chat.id);
                intent.putExtra("mode", chat.mode);
                startActivity(intent);
            });
            list.addView(card);
        }
    }
}
