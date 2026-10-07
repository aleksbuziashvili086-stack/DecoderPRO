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
import com.alekss.kartveli.ai.AIOrchestrator;
import com.alekss.kartveli.ai.AIProvider;
import com.alekss.kartveli.data.MemoryRepository;
import com.alekss.kartveli.files.LocalFileReader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
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
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                String name = String.valueOf(uri.getLastPathSegment());
                pendingFile = name.endsWith(".zip") ? LocalFileReader.readZip(in) : LocalFileReader.readText(in);
                Toast.makeText(this, "ფაილი მზად არის", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "ფაილი ვერ გაიხსნა", Toast.LENGTH_SHORT).show();
            }
        });
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            pendingImage = readBase64(uri);
            pendingMime = getContentResolver().getType(uri);
            Toast.makeText(this, pendingImage == null ? "სურათი ვერ წავიკითხე" : "სურათი მზად არის", Toast.LENGTH_SHORT).show();
        });
        tts = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) tts.setLanguage(new Locale("ka", "GE")); });
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
    }

    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ka-GE");
        try { startActivityForResult(intent, VOICE); }
        catch (Exception e) { Toast.makeText(this, "ქართული ხმა ამ ტელეფონზე არ დგას", Toast.LENGTH_SHORT).show(); }
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
            MemoryRepository.add(this, text.replaceFirst("დაიმახსოვრე", "").trim(), "PREFERENCE");
            addBubble("დავიმახსოვრე", false);
            input.setText("");
            return;
        }
        String shown = text.isEmpty() ? "ფაილი ან სურათი" : text;
        input.setText("");
        appendMessage("user", shown + (pendingFile.isEmpty() ? "" : "\n\nფაილი:\n" + pendingFile));
        addBubble(shown, true);
        addBubble("ვწერ…", false);
        busy = true;
        final String file = pendingFile;
        final String image = pendingImage;
        final String mime = pendingMime;
        pendingFile = "";
        pendingImage = null;
        final JSONArray snapshot = copy(chat.messages);
        new Thread(() -> {
            AIProvider.Result result = AIOrchestrator.send(this, mode, shown, snapshot, file, image, mime);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                busy = false;
                if (messages.getChildCount() > 0) messages.removeViewAt(messages.getChildCount() - 1);
                addBubble(result.text, false);
                if (result.ok) {
                    appendMessage("assistant", result.text);
                    if (tts != null) tts.speak(result.text, TextToSpeech.QUEUE_FLUSH, null, "kartveli");
                }
                HistoryStore.save(this, chat);
            });
        }).start();
    }

    private String readBase64(android.net.Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n; int total = 0;
            while ((n = in.read(buf)) > 0 && total < 1200000) { out.write(buf, 0, n); total += n; }
            in.close();
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
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
        if (text.contains("```")) bubble.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = user ? Gravity.END : Gravity.START;
        params.setMargins(user ? 48 : 0, 8, user ? 0 : 48, 8);
        bubble.setLayoutParams(params);
        messages.addView(bubble);
        scroll.post(() -> scroll.fullScroll(ScrollView.FOCUS_DOWN));
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
