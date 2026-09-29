package com.alekss.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class HexViewerActivity extends Activity {
    private static final int PICK = 200;
    private TextView output;
    private Uri uri;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_hex_viewer);
        output = findViewById(R.id.hex_output);
        Button pick = findViewById(R.id.btn_hex_pick);
        Button clear = findViewById(R.id.btn_hex_clear);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        clear.setOnClickListener(v -> { output.setText(""); uri = null; });
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null) {
            uri = d.getData();
            if (uri != null) showHex();
        }
    }

    private void showHex() {
        try {
            byte[] bytes = FileUtils.readFileBytes(this, uri);
            StringBuilder sb = new StringBuilder();
            int limit = Math.min(bytes.length, 4096);
            for (int i = 0; i < limit; i += 16) {
                sb.append(String.format("%08X  ", i));
                StringBuilder ascii = new StringBuilder();
                for (int j = 0; j < 16; j++) {
                    if (i + j < limit) {
                        sb.append(String.format("%02X ", bytes[i + j]));
                        char c = (char) (bytes[i + j] & 0xFF);
                        ascii.append(c >= 32 && c <= 126 ? c : '.');
                    } else {
                        sb.append("   ");
                        ascii.append(' ');
                    }
                }
                sb.append(" ").append(ascii).append("\n");
            }
            if (bytes.length > limit) sb.append("\n... (").append(bytes.length - limit).append(" bytes more)");
            output.setText(sb.toString());
        } catch (Exception e) {
            output.setText("❌ " + e.getMessage());
        }
    }
}
