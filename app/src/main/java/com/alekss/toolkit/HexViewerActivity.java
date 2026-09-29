package com.alekss.toolkit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class HexViewerActivity extends Activity {
    private static final int PICK = 200;
    private TextView output;
    private Uri uri;
    private byte[] data;
    private String fileName = "file";
    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_hex_viewer);
        output = findViewById(R.id.hex_output);
        Button pick = findViewById(R.id.btn_hex_pick);
        Button copyAll = findViewById(R.id.btn_hex_copy);
        Button copyAscii = findViewById(R.id.btn_hex_copy_ascii);
        Button save = findViewById(R.id.btn_hex_save);
        Button edit = findViewById(R.id.btn_hex_edit);
        Button clear = findViewById(R.id.btn_hex_clear);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        copyAll.setOnClickListener(v -> copyToClipboard(output.getText().toString(), "HEX"));
        copyAscii.setOnClickListener(v -> copyToClipboard(extractAscii(), "ASCII"));
        save.setOnClickListener(v -> saveFile());
        edit.setOnClickListener(v -> showEditDialog());
        clear.setOnClickListener(v -> {
            output.setText("");
            uri = null;
            data = null;
        });
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null) {
            uri = d.getData();
            if (uri != null) {
                fileName = FileUtils.getFileName(this, uri);
                showHex();
            }
        }
    }

    private void showHex() {
        try {
            data = FileUtils.readFileBytes(this, uri);
            output.setText(formatHex(data));
        } catch (Exception e) {
            output.setText("❌ " + e.getMessage());
        }
    }

    private String formatHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        int limit = Math.min(bytes.length, 16384); // 16KB
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
        return sb.toString();
    }

    private String extractAscii() {
        if (data == null) return "";
        StringBuilder sb = new StringBuilder();
        StringBuilder current = new StringBuilder();
        for (byte b : data) {
            char c = (char) (b & 0xFF);
            if (c >= 32 && c <= 126) {
                current.append(c);
            } else {
                if (current.length() >= 4) {
                    sb.append(current).append("\n");
                }
                current = new StringBuilder();
            }
        }
        if (current.length() >= 4) sb.append(current).append("\n");
        return sb.toString();
    }

    private void copyToClipboard(String text, String label) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        Toast.makeText(this, "✅ " + label + " დაკოპირდა!", Toast.LENGTH_SHORT).show();
    }

    private void saveFile() {
        if (data == null) {
            Toast.makeText(this, "ჯერ აირჩიე ფაილი!", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File dir = new File(Environment.getExternalStorageDirectory(), "DecoderPRO");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "hex_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(out);
            fos.write(output.getText().toString().getBytes());
            fos.close();
            Toast.makeText(this, "✅ შენახულია: " + out.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showEditDialog() {
        if (data == null) {
            Toast.makeText(this, "ჯერ აირჩიე ფაილი!", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(this);
        input.setHint("შეიყვანე offset (მაგ: 0, 16, 32)");
        input.setTextColor(0xFF00E5FF);
        input.setBackgroundColor(0xFF131824);
        input.setPadding(20, 20, 20, 20);

        new AlertDialog.Builder(this)
            .setTitle("HEX Edit Mode")
            .setMessage("შეიყვანე byte-ის offset და ახალი HEX მნიშვნელობა")
            .setView(input)
            .setPositiveButton("Edit", (dialog, which) -> {
                String offsetStr = input.getText().toString().trim();
                if (offsetStr.isEmpty()) return;
                Toast.makeText(this, "Edit mode: offset " + offsetStr + " (მალე!)", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
