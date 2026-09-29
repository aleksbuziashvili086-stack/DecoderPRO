package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class FileAnalysisActivity extends Activity {

    private static final int PICK_FILE = 100;
    private EditText input;
    private TextView output;
    private Uri selectedUri;
    private String selectedName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_analysis);

        input = findViewById(R.id.fa_input);
        output = findViewById(R.id.fa_output);

        Button btnPick = findViewById(R.id.btn_fa_pick);
        Button btnAnalyze = findViewById(R.id.btn_fa_analyze);
        Button btnCopy = findViewById(R.id.btn_fa_copy);
        Button btnClear = findViewById(R.id.btn_fa_clear);

        btnPick.setOnClickListener(v -> pickFile());
        btnAnalyze.setOnClickListener(v -> analyze());
        btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("analysis", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        btnClear.setOnClickListener(v -> {
            input.setText("");
            output.setText("");
            selectedUri = null;
            selectedName = "";
        });
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "აირჩიე ფაილი"), PICK_FILE);
        } catch (Exception e) {
            Toast.makeText(this, "File picker ვერ გაიხსნა: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && resultCode == RESULT_OK && data != null) {
            selectedUri = data.getData();
            if (selectedUri != null) {
                selectedName = FileUtils.getFileName(this, selectedUri);
                Toast.makeText(this, "✅ აირჩიე: " + selectedName, Toast.LENGTH_SHORT).show();
                input.setText("[ფაილი: " + selectedName + "]");
            }
        }
    }

    private void analyze() {
        if (selectedUri != null) {
            analyzeFile();
        } else {
            analyzeText();
        }
    }

    private void analyzeFile() {
        try {
            byte[] bytes = FileUtils.readFileBytes(this, selectedUri);
            StringBuilder sb = new StringBuilder();
            sb.append("📁 File Analysis\n");
            sb.append("─────────────────────\n\n");
            sb.append("📄 Name: ").append(selectedName).append("\n");
            sb.append("📏 Size: ").append(FileUtils.formatSize(bytes.length))
              .append(" (").append(bytes.length).append(" bytes)\n\n");

            sb.append("🔍 Detected Type:\n");
            sb.append("  ").append(detectType(bytes)).append("\n\n");

            sb.append("🎯 Magic Bytes (first 16):\n");
            sb.append("  ").append(toHex(bytes, 16)).append("\n\n");

            sb.append("📊 Byte Distribution:\n");
            sb.append("  ASCII (0x20-0x7E): ").append(countAscii(bytes)).append("\n");
            sb.append("  Control (<0x20):   ").append(countControl(bytes)).append("\n");
            sb.append("  High (>=0x80):     ").append(countHigh(bytes)).append("\n\n");

            sb.append("🔐 Entropy: ").append(String.format("%.4f", entropy(bytes))).append(" bits/byte\n");

            output.setText(sb.toString());
        } catch (Exception e) {
            output.setText("❌ შეცდომა: " + e.getMessage());
        }
    }

    private void analyzeText() {
        String text = input.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "შეიყვანე ტექსტი ან აირჩიე ფაილი!", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] bytes;
        try { bytes = text.getBytes("UTF-8"); } catch (Exception e) { bytes = text.getBytes(); }

        StringBuilder sb = new StringBuilder();
        sb.append("📊 Text Analysis\n");
        sb.append("─────────────────────\n\n");
        sb.append("📏 Size: ").append(bytes.length).append(" bytes\n");
        sb.append("📝 Characters: ").append(text.length()).append("\n");
        sb.append("📄 Lines: ").append(text.split("\n").length).append("\n");
        sb.append("🔤 Words: ").append(text.split("\\s+").length).append("\n\n");
        sb.append("🔍 Detected Type:\n  ").append(detectType(bytes)).append("\n\n");
        sb.append("🎯 Magic Bytes:\n  ").append(toHex(bytes, 16)).append("\n\n");
        sb.append("🔐 Entropy: ").append(String.format("%.4f", entropy(bytes))).append(" bits/byte\n");
        output.setText(sb.toString());
    }

    private String detectType(byte[] b) {
        if (b.length >= 4) {
            if (b[0]==0x50 && b[1]==0x4B && b[2]==0x03 && b[3]==0x04) return "ZIP / APK / JAR";
            if (b[0]==0x7F && b[1]==0x45 && b[2]==0x4C && b[3]==0x46) return "ELF (Linux binary)";
            if (b[0]==(byte)0x89 && b[1]==0x50 && b[2]==0x4E && b[3]==0x47) return "PNG image";
            if (b[0]==(byte)0xFF && b[1]==(byte)0xD8) return "JPEG image";
            if (b[0]==0x64 && b[1]==0x65 && b[2]==0x78 && b[3]==0x0A) return "DEX (Android)";
            if (b[0]==0x25 && b[1]==0x50 && b[2]==0x44 && b[3]==0x46) return "PDF";
            if (b[0]==0x47 && b[1]==0x49 && b[2]==0x46 && b[3]==0x38) return "GIF image";
            if (b[0]==0x52 && b[1]==0x61 && b[2]==0x72 && b[3]==0x21) return "RAR archive";
            if (b[0]==0x1F && b[1]==(byte)0x8B) return "GZIP";
            if (b[0]==0x42 && b[1]==0x5A && b[2]==0x68) return "BZIP2";
        }
        return "Text / Unknown";
    }

    private String toHex(byte[] b, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, max); i++)
            sb.append(String.format("%02X ", b[i]));
        if (b.length > max) sb.append("...");
        return sb.toString().trim();
    }

    private int countAscii(byte[] b) { int c=0; for (byte x : b) if (x>=0x20 && x<=0x7E) c++; return c; }
    private int countControl(byte[] b) { int c=0; for (byte x : b) if (x<0x20) c++; return c; }
    private int countHigh(byte[] b) { int c=0; for (byte x : b) if ((x & 0xFF)>=0x80) c++; return c; }
    private double entropy(byte[] b) {
        int[] freq = new int[256];
        for (byte x : b) freq[x & 0xFF]++;
        double e = 0;
        for (int f : freq) {
            if (f > 0) {
                double p = (double) f / b.length;
                e -= p * (Math.log(p) / Math.log(2));
            }
        }
        return e;
    }
}
