package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SignatureActivity extends Activity {
    private static final int PICK = 1100;
    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_signature);
        output = findViewById(R.id.sig_output);
        Button pick = findViewById(R.id.btn_sig_pick);
        Button copy = findViewById(R.id.btn_sig_copy);
        Button clear = findViewById(R.id.btn_sig_clear);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("sig", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        clear.setOnClickListener(v -> output.setText(""));
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null && d.getData() != null) {
            try {
                output.setText("⏳ მიმდინარეობს...");
                Uri uri = d.getData();
                SignatureVerifier.Result result = SignatureVerifier.verify(
                    getContentResolver().openInputStream(uri));
                output.setText(result.report.toString());
            } catch (Exception e) {
                output.setText("❌ " + e.getMessage());
            }
        }
    }
}
