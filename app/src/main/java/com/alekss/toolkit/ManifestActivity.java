package com.alekss.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ManifestActivity extends Activity {
    private static final int PICK = 400;
    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_manifest);
        output = findViewById(R.id.manifest_output);
        Button pick = findViewById(R.id.btn_manifest_pick);
        Button clear = findViewById(R.id.btn_manifest_clear);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        clear.setOnClickListener(v -> output.setText(""));
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null && d.getData() != null) readManifest(d.getData());
    }

    private void readManifest(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.getName().equals("AndroidManifest.xml")) {
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = zis.read(buf)) != -1) baos.write(buf, 0, n);
                    byte[] data = baos.toByteArray();
                    StringBuilder sb = new StringBuilder();
                    sb.append("📋 AndroidManifest.xml\n─────────────────────\n\n");
                    sb.append("📏 Size: ").append(data.length).append(" bytes\n");
                    sb.append("🎯 Format: ").append(data.length >= 4 && data[0]==0x03 && data[1]==0x00 ? "Binary AXML" : "Text XML").append("\n\n");
                    sb.append("🎯 First 64 bytes (HEX):\n");
                    for (int i = 0; i < Math.min(data.length, 64); i++) {
                        sb.append(String.format("%02X ", data[i]));
                        if ((i + 1) % 16 == 0) sb.append("\n");
                    }
                    sb.append("\n\n💡 Tip: Binary AXML — გამოიყენე apktool ან jadx დეკოდირებისთვის");
                    output.setText(sb.toString());
                    zis.close();
                    return;
                }
            }
            zis.close();
            output.setText("❌ AndroidManifest.xml ვერ მოიძებნა");
        } catch (Exception e) {
            output.setText("❌ " + e.getMessage());
        }
    }
}
