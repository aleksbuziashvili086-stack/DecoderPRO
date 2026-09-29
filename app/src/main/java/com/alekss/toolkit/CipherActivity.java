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

public class CipherActivity extends Activity {
    private EditText input, keyInput;
    private TextView output;
    private String currentMode = "XOR";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cipher);

        input = findViewById(R.id.cipher_input);
        keyInput = findViewById(R.id.cipher_key);
        output = findViewById(R.id.cipher_output);

        Button btnXor = findViewById(R.id.btn_xor);
        Button btnCaesar = findViewById(R.id.btn_caesar);
        Button btnRot13 = findViewById(R.id.btn_rot13);
        Button btnDecode = findViewById(R.id.btn_cipher_decode);
        Button btnEncode = findViewById(R.id.btn_cipher_encode);
        Button btnCopy = findViewById(R.id.btn_cipher_copy);
        Button btnClear = findViewById(R.id.btn_cipher_clear);

        btnXor.setOnClickListener(v -> currentMode = "XOR");
        btnCaesar.setOnClickListener(v -> currentMode = "CAESAR");
        btnRot13.setOnClickListener(v -> currentMode = "ROT13");

        btnDecode.setOnClickListener(v -> process());
        btnEncode.setOnClickListener(v -> process());

        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("out", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        btnClear.setOnClickListener(v -> {
            input.setText(""); keyInput.setText(""); output.setText("");
        });
    }

    private void process() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String result;
            if (currentMode.equals("XOR")) {
                String key = keyInput.getText().toString();
                if (key.isEmpty()) key = "K";
                result = xor(text, key);
            } else if (currentMode.equals("CAESAR")) {
                int shift = 3;
                try {
                    if (!keyInput.getText().toString().isEmpty())
                        shift = Integer.parseInt(keyInput.getText().toString());
                } catch (Exception ignored) {}
                result = caesar(text, shift);
            } else {
                result = caesar(text, 13);
            }
            output.setText(result);
        } catch (Exception e) {
            output.setText("❌ " + e.getMessage());
        }
    }

    private String xor(String text, String key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
            sb.append((char) (text.charAt(i) ^ key.charAt(i % key.length())));
        return sb.toString();
    }

    private String caesar(String text, int shift) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isUpperCase(c))
                sb.append((char) ((c - 'A' + shift + 26) % 26 + 'A'));
            else if (Character.isLowerCase(c))
                sb.append((char) ((c - 'a' + shift + 26) % 26 + 'a'));
            else sb.append(c);
        }
        return sb.toString();
    }
}
