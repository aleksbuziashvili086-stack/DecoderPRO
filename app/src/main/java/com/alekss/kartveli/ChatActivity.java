package com.alekss.kartveli;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private static final int VOICE = 41;
    private HistoryStore.Chat chat;
    private Mode mode;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private boolean busy;
    private String pendingFile = "";
    private String pendingImage;
    private String pendingMime;
    private TextToSpeech tts;
    private ActivityResultLauncher<String> filePicker;
    private ActivityResultLauncher<String> imagePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(Prefs.dark(this) ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        filePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pendingFile = readText(uri);
            Toast.makeText(this, pendingFile.startsWith("ამ ფაილს") ? pendingFile : "ფაილი მზად არის. დაწერე რა გინდა იცოდე.", Toast.LENGTH_SHORT).show();
        });
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pendingImage = readBase64(uri);
            pendingMime = getContentResolver().getType(uri);
            Toast.makeText(this, pendingImage == null ? "სურათი ვერ წავიკითხე" : "სურათი მზად არის. დაწერე რა გინდა იცოდე.", Toast.LENGTH_SHORT).show();
        });
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("ka", "GE"));
        });
        mode = Mode.find(getIntent().getStringExtra("mode"));
        String existing = getIntent().getStringExtra("chat_id");
        if (existing != null) chat = HistoryStore.open(this, existing);
        if (chat == null) chat = HistoryStore.create(this, mode.id, mode.title);
        ((TextView) findViewById(R.id.chat_title)).setText(mode.title);
        messages = findViewById(R.id.messages);
        scroll = findViewById(R.id.chat_scroll);
        input = findViewById(R.id.chat_input);
        findViewById(R.id.chat_back).setOnClickListener(v -> finish());
        findViewById(R.id.chat_send).setOnClickListener(v -> send(input.getText().toString()));
        findViewById(R.id.chat_file).setOnClickListener(v -> filePicker.launch("*/*"));
        findViewById(R.id.chat_image).setOnClickListener(v -> imagePicker.launch("image/*"));
        findViewById(R.id.chat_voice).setOnClickListener(v -> startVoice());
        renderAll();
        String seed = getIntent().getStringExtra("seed");
        if (seed != null && chat.messages.length() == 0) send(seed);
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ka-GE");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "ილაპარაკე ქართულად");
        try { startActivityForResult(intent, VOICE); }
        catch (Exception e) { Toast.makeText(this, "ამ ტელეფონზე ქართული ხმა არ დგას", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> heard = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (heard != null && !heard.isEmpty()) send(heard.get(0));
        }
    }

    private void send(String raw) {
        String text = raw == null ? "" : raw.trim();
        if ((text.isEmpty() && pendingFile.isEmpty() && pendingImage == null) || busy) return;
        if (text.startsWith("დაიმახსოვრე")) {
            String fact = text.replaceFirst("დაიმახსოვრე", "").trim();
            String old = Prefs.of(this).getString("memory", "");
            Prefs.of(this).edit().putString("memory", (old + "\n" + fact).trim()).apply();
            addBubble("დავიმახსოვრე: " + fact, false);
            input.setText("");
            return;
        }
        if (!allowSend()) {
            Toast.makeText(this, "დღევანდელი 20 უფასო პასუხი ამოიწურა.", Toast.LENGTH_LONG).show();
            return;
        }
        String shown = text.isEmpty() ? "ფაილი ან სურათი" : text;
        input.setText("");
        appendMessage("user", shown);
        addBubble(shown, true);
        addBubble("ვწერ…", false);
        busy = true;
        final String file = pendingFile;
        final String image = pendingImage;
        final String mime = pendingMime;
        pendingFile = "";
        pendingImage = null;
        final JSONArray snapshot = copy(chat.messages);
        if (!file.isEmpty()) {
            try { snapshot.getJSONObject(snapshot.length() - 1).put("text", shown + "\n\nფაილის ტექსტი:\n" + file); } catch (Exception ignored) {}
        }
        final String system = PromptLibrary.system(Prefs.tone(this), mode) + "\nდამახსოვრებული: " + Prefs.of(this).getString("memory", "");
        final String key = Prefs.apiKey(this);
        final String modelName = Prefs.model(this);
        new Thread(() -> {
            AiClient.Result result = AiClient.ask(key, modelName, system, snapshot, image, mime);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                busy = false;
                if (messages.getChildCount() > 0) messages.removeViewAt(messages.getChildCount() - 1);
                addBubble(result.text, false);
                if (result.ok) {
                    appendMessage("assistant", result.text);
                    bumpCount();
                    if (tts != null) tts.speak(result.text, TextToSpeech.QUEUE_FLUSH, null, "kartveli");
                }
                HistoryStore.save(this, chat);
            });
        }).start();
    }

    private String readText(android.net.Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0 && total < 20000) { out.write(buf, 0, n); total += n; }
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "ამ ფაილს ვერ ვკითხულობ. TXT, კოდი ან MD სცადე.";
        }
    }

    private String readBase64(android.net.Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0 && total < 1500000) { out.write(buf, 0, n); total += n; }
            in.close();
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    private void appendMessage(String role, String text) {
        try {
            JSONObject item = new JSONObject();
            item.put("role", role);
            item.put("text", text);
            chat.messages.put(item);
        } catch (Exception ignored) {}
    }

    private void renderAll() {
        messages.removeAllViews();
        for (int i = 0; i < chat.messages.length(); i++) {
            JSONObject item = chat.messages.optJSONObject(i);
            if (item != null) addBubble(item.optString("text"), "user".equals(item.optString("role")));
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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
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
        try { return new JSONArray(source.toString()); } catch (Exception e) { return new JSONArray(); }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}
