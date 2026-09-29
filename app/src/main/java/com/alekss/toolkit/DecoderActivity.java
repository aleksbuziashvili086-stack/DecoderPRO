package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class DecoderActivity extends Activity {

    private EditText input;
    private TextView output;
    private String currentMode = "HEX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decoder);

        input = findViewById(R.id.decoder_input);
        output = findViewById(R.id.decoder_output);

        Button btnHex = findViewById(R.id.btn_hex);
        Button btnBase64 = findViewById(R.id.btn_base64);
        Button btnUrl = findViewById(R.id.btn_url);
        Button btnDecode = findViewById(R.id.btn_decode);
        Button btnEncode = findViewById(R.id.btn_encode);
        Button btnCopy = findViewById(R.id.btn_copy);
        Button btnClear = findViewById(R.id.btn_clear);

        btnHex.setOnClickListener(v -> currentMode = "HEX");
        btnBase64.setOnClickListener(v -> currentMode = "BASE64");
        btnUrl.setOnClickListener(v -> currentMode = "URL");

        btnDecode.setOnClickListener(v -> decode());
        btnEncode.setOnClickListener(v -> encode());

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("output", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });

        btnClear.setOnClickListener(v -> {
            input.setText("");
            output.setText("");
        });
    }

    private void decode() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String result;
            if (currentMode.equals("HEX")) {
                result = hexToString(text);
            } else if (currentMode.equals("BASE64")) {
                byte[] decoded = Base64.decode(text, Base64.DEFAULT);
                result = new String(decoded, "UTF-8");
            } else {
                result = URLDecoder.decode(text, "UTF-8");
            }
            output.setText(result);
        } catch (Exception e) {
            output.setText("❌ შეცდომა: " + e.getMessage());
        }
    }

    private void encode() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String result;
            if (currentMode.equals("HEX")) {
                result = stringToHex(text);
            } else if (currentMode.equals("BASE64")) {
                result = Base64.encodeToString(text.getBytes("UTF-8"), Base64.DEFAULT);
            } else {
                result = URLEncoder.encode(text, "UTF-8");
            }
            output.setText(result);
        } catch (Exception e) {
            output.setText("❌ შეცდომა: " + e.getMessage());
        }
    }

    private String hexToString(String hex) {
        hex = hex.replaceAll("\\s+", "");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            sb.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        return sb.toString();
    }

    private String stringToHex(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(String.format("%02X ", (int) c));
        }
        return sb.toString().trim();
    }
}
