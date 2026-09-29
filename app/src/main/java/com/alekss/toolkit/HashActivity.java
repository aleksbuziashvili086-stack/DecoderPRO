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
import java.security.MessageDigest;

public class HashActivity extends Activity {

    private EditText input;
    private TextView outMd5, outSha1, outSha256, outSha512;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hash);

        input = findViewById(R.id.hash_input);
        outMd5 = findViewById(R.id.hash_md5);
        outSha1 = findViewById(R.id.hash_sha1);
        outSha256 = findViewById(R.id.hash_sha256);
        outSha512 = findViewById(R.id.hash_sha512);

        Button btnHash = findViewById(R.id.btn_hash);
        Button btnClear = findViewById(R.id.btn_hash_clear);
        Button btnCopy = findViewById(R.id.btn_hash_copy);

        btnHash.setOnClickListener(v -> computeHashes());
        btnClear.setOnClickListener(v -> {
            input.setText("");
            outMd5.setText("");
            outSha1.setText("");
            outSha256.setText("");
            outSha512.setText("");
        });
        btnCopy.setOnClickListener(v -> {
            String all = "MD5: " + outMd5.getText() + "\n" +
                         "SHA-1: " + outSha1.getText() + "\n" +
                         "SHA-256: " + outSha256.getText() + "\n" +
                         "SHA-512: " + outSha512.getText();
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("hashes", all));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
    }

    private void computeHashes() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            outMd5.setText(hash(text, "MD5"));
            outSha1.setText(hash(text, "SHA-1"));
            outSha256.setText(hash(text, "SHA-256"));
            outSha512.setText(hash(text, "SHA-512"));
        } catch (Exception e) {
            Toast.makeText(this, "შეცდომა: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String hash(String text, String algo) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] digest = md.digest(text.getBytes("UTF-8"));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
