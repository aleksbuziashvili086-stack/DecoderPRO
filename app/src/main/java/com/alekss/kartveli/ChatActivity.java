package com.alekss.kartveli;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private HistoryStore.Chat chat;
    private Mode mode;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(
            Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mode = Mode.find(getIntent().getStringExtra("mode"));
        String existing = getIntent().getStringExtra("chat_id");
        if (existing != null) chat = HistoryStore.open(this, existing);
        if (chat == null) chat = HistoryStore.create(this, mode.id, mode.title);

        TextView title = findViewById(R.id.chat_title);
        title.setText(mode.title);
        messages = findViewById(R.id.messages);
        scroll = findViewById(R.id.chat_scroll);
        input = findViewById(R.id.chat_input);
        findViewById(R.id.chat_back).setOnClickListener(v -> finish());
        findViewById(R.id.chat_send).setOnClickListener(v -> send(input.getText().toString()));

        renderAll();
        String seed = getIntent().getStringExtra("seed");
        if (seed != null && chat.messages.length() == 0) send(seed);
        else if (chat.messages.length() == 0 && mode.seed != null && !mode.seed.isEmpty()) {
            addBubble("აირჩიე რა გინდა, ან უბრალოდ დაწერე. " + mode.subtitle + ".", false);
        }
    }

    private void send(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty() || busy) return;
        if (text.length() > 8000) {
            Toast.makeText(this, "ტექსტი ძალიან გრძელია", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!allowSend()) {
            Toast.makeText(this, "დღევანდელი 20 უფასო პასუხი ამოიწურა. Pro სატესტოდ ირთვება გამოწერაში.", Toast.LENGTH_LONG).show();
            return;
        }
        input.setText("");
        appendMessage("user", text);
        addBubble(text, true);
        addBubble("ვწერ…", false);
        busy = true;
        final JSONArray snapshot = copy(chat.messages);
        final String system = PromptLibrary.system(Prefs.tone(this), mode);
        final String key = Prefs.apiKey(this);
        final String model = Prefs.model(this);
        new Thread(() -> {
            AiClient.Result result = AiClient.ask(key, model, system, snapshot);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                busy = false;
                if (messages.getChildCount() > 0) messages.removeViewAt(messages.getChildCount() - 1);
                addBubble(result.text, false);
                if (result.ok) {
                    appendMessage("assistant", result.text);
                    bumpCount();
                    if ("საუბარი".equals(chat.title) || mode.title.equals(chat.title)) {
                        chat.title = text.length() > 42 ? text.substring(0, 42) + "…" : text;
                    }
                }
                HistoryStore.save(this, chat);
            });
        }).start();
    }

    private void appendMessage(String role, String text) {
        try {
            JSONObject item = new JSONObject();
            item.put("role", role);
            item.put("text", text);
            chat.messages.put(item);
        } catch (Exception ignored) {
        }
    }

    private void renderAll() {
        messages.removeAllViews();
        for (int i = 0; i < chat.messages.length(); i++) {
            JSONObject item = chat.messages.optJSONObject(i);
            if (item == null) continue;
            addBubble(item.optString("text"), "user".equals(item.optString("role")));
        }
    }

    private void addBubble(String text, boolean user) {
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(0xFFF4F7FF);
        bubble.setTextSize(15);
        bubble.setPadding(28, 22, 28, 22);
        bubble.setBackgroundResource(R.drawable.bg_card);
        if (user) bubble.setBackgroundColor(0xFF2A3F72);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = user ? Gravity.END : Gravity.START;
        params.setMargins(user ? 48 : 0, 8, user ? 0 : 48, 8);
        bubble.setLayoutParams(params);
        messages.addView(bubble);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private boolean allowSend() {
        if (Prefs.pro(this)) return true;
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String saved = Prefs.of(this).getString(Prefs.KEY_DAY, "");
        int count = today.equals(saved) ? Prefs.of(this).getInt(Prefs.KEY_COUNT, 0) : 0;
        return count < 20;
    }

    private void bumpCount() {
        if (Prefs.pro(this)) return;
        String today = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        String saved = Prefs.of(this).getString(Prefs.KEY_DAY, "");
        int count = today.equals(saved) ? Prefs.of(this).getInt(Prefs.KEY_COUNT, 0) : 0;
        Prefs.of(this).edit().putString(Prefs.KEY_DAY, today).putInt(Prefs.KEY_COUNT, count + 1).apply();
    }

    private JSONArray copy(JSONArray source) {
        try {
            return new JSONArray(source.toString());
        } catch (Exception e) {
            return new JSONArray();
        }
    }
}
