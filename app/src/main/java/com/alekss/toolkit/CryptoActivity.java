package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class CryptoActivity extends Activity {
    private EditText input;
    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_crypto);
        input = findViewById(R.id.crypto_input);
        output = findViewById(R.id.crypto_output);

        Button xor = findViewById(R.id.btn_crypto_xor);
        Button caesar = findViewById(R.id.btn_crypto_caesar);
        Button freq = findViewById(R.id.btn_crypto_freq);
        Button auto = findViewById(R.id.btn_crypto_auto);
        Button copy = findViewById(R.id.btn_crypto_copy);
        Button clear = findViewById(R.id.btn_crypto_clear);

        xor.setOnClickListener(v -> xorBrute());
        caesar.setOnClickListener(v -> caesarBrute());
        freq.setOnClickListener(v -> frequency());
        auto.setOnClickListener(v -> autoDetect());
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("crypto", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        clear.setOnClickListener(v -> { input.setText(""); output.setText(""); });
    }

    private String getInput() {
        String s = input.getText().toString().trim();
        if (s.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
        }
        return s;
    }

    private void xorBrute() {
        String text = getInput();
        if (text.isEmpty()) return;
        byte[] data = text.getBytes();
        StringBuilder sb = new StringBuilder();
        sb.append("🔓 XOR Brute-Force (256 keys)\n");
        sb.append("═══════════════════════════\n\n");
        for (int key = 0; key < 256; key++) {
            StringBuilder line = new StringBuilder();
            int printable = 0;
            for (byte b : data) {
                char c = (char)((b ^ key) & 0xFF);
                if (c >= 32 && c <= 126) { line.append(c); printable++; }
                else line.append('.');
            }
            double ratio = (double) printable / data.length;
            if (ratio > 0.7) {
                sb.append(String.format("Key 0x%02X (%3d): %s\n", key, key, line.toString()));
            }
        }
        sb.append("\n═══════════════════════════\n");
        sb.append("💡 ნაჩვენებია key-ები სადაც >70% printable\n");
        output.setText(sb.toString());
    }

    private void caesarBrute() {
        String text = getInput();
        if (text.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("🔓 Caesar Brute-Force (25 shifts)\n");
        sb.append("═══════════════════════════\n\n");
        for (int shift = 1; shift < 26; shift++) {
            StringBuilder line = new StringBuilder();
            for (char c : text.toCharArray()) {
                if (c >= 'A' && c <= 'Z') line.append((char)((c - 'A' + shift + 26) % 26 + 'A'));
                else if (c >= 'a' && c <= 'z') line.append((char)((c - 'a' + shift + 26) % 26 + 'a'));
                else line.append(c);
            }
            sb.append(String.format("Shift %2d: %s\n", shift, line.toString()));
        }
        output.setText(sb.toString());
    }

    private void frequency() {
        String text = getInput();
        if (text.isEmpty()) return;
        Map<Character, Integer> freq = new HashMap<>();
        for (char c : text.toCharArray()) freq.put(c, freq.getOrDefault(c, 0) + 1);
        StringBuilder sb = new StringBuilder();
        sb.append("📊 Frequency Analysis\n");
        sb.append("═══════════════════════════\n\n");
        sb.append("სულ სიმბოლო: ").append(text.length()).append("\n");
        sb.append("უნიკალური: ").append(freq.size()).append("\n\n");
        sb.append("ტოპ 15 სიმბოლო:\n");
        freq.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .limit(15)
            .forEach(e -> {
                char c = e.getKey();
                String disp = (c == '\n') ? "\\n" : (c == ' ') ? "space" : String.valueOf(c);
                double pct = e.getValue() * 100.0 / text.length();
                sb.append(String.format("  '%s' : %d  (%.2f%%)\n", disp, e.getValue(), pct));
            });
        output.setText(sb.toString());
    }

    private void autoDetect() {
        String text = getInput();
        if (text.isEmpty()) return;
        StringBuilder sb = new StringBuilder();
        sb.append("🔍 Auto-Detect\n");
        sb.append("═══════════════════════════\n\n");

        // Base64 check
        if (text.matches("^[A-Za-z0-9+/=]+$") && text.length() % 4 == 0) {
            try {
                byte[] dec = android.util.Base64.decode(text, android.util.Base64.DEFAULT);
                sb.append("✅ Base64 detected!\n");
                sb.append("Decoded: ").append(new String(dec, "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        // HEX check
        String noSpace = text.replaceAll("\\s+", "");
        if (noSpace.matches("^[0-9A-Fa-f]+$") && noSpace.length() % 2 == 0) {
            try {
                StringBuilder hexDec = new StringBuilder();
                for (int i = 0; i < noSpace.length(); i += 2)
                    hexDec.append((char) Integer.parseInt(noSpace.substring(i, i + 2), 16));
                sb.append("✅ HEX detected!\n");
                sb.append("Decoded: ").append(hexDec).append("\n\n");
            } catch (Exception ignored) {}
        }

        // URL check
        if (text.contains("%")) {
            try {
                sb.append("✅ URL encoded!\n");
                sb.append("Decoded: ").append(java.net.URLDecoder.decode(text, "UTF-8")).append("\n\n");
            } catch (Exception ignored) {}
        }

        // Hash check
        if (text.matches("^[a-f0-9]{32}$")) sb.append("🔐 Possible MD5 hash (32 hex)\n\n");
        else if (text.matches("^[a-f0-9]{40}$")) sb.append("🔐 Possible SHA-1 hash (40 hex)\n\n");
        else if (text.matches("^[a-f0-9]{64}$")) sb.append("🔐 Possible SHA-256 hash (64 hex)\n\n");
        else if (text.matches("^[a-f0-9]{128}$")) sb.append("🔐 Possible SHA-512 hash (128 hex)\n\n");

        // JWT check
        if (text.matches("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$")) {
            sb.append("🔑 JWT detected!\n");
            String[] parts = text.split("\\.");
            try {
                byte[] h = android.util.Base64.decode(parts[0], android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING);
                byte[] p = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_PADDING);
                sb.append("Header: ").append(new String(h)).append("\n");
                sb.append("Payload: ").append(new String(p)).append("\n\n");
            } catch (Exception ignored) {}
        }

        sb.append("═══════════════════════════\n");
        output.setText(sb.toString());
    }
}
