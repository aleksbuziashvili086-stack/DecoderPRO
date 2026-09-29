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

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class GameLabActivity extends Activity {
    private static final int PICK = 900;
    private Uri uri;
    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_game_lab);
        output = findViewById(R.id.gl_output);
        Button pick = findViewById(R.id.btn_gl_pick);
        Button copy = findViewById(R.id.btn_gl_copy);
        Button clear = findViewById(R.id.btn_gl_clear);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("game", output.getText().toString()));
            Toast.makeText(this, "✅ დაკოპირდა!", Toast.LENGTH_SHORT).show();
        });
        clear.setOnClickListener(v -> output.setText(""));
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null && d.getData() != null) analyze(d.getData());
    }

    private void analyze(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry e;
            StringBuilder sb = new StringBuilder();
            sb.append("🎮 Game Analysis\n═══════════════════════════\n\n");

            String engine = "Unknown";
            int assets = 0, libs = 0, unityFiles = 0, ueFiles = 0, textures = 0, audio = 0, dataFiles = 0;
            long totalSize = 0;
            StringBuilder libList = new StringBuilder();
            StringBuilder engineFiles = new StringBuilder();
            StringBuilder assetSample = new StringBuilder();

            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName().toLowerCase();
                long size = e.getSize();
                totalSize += size > 0 ? size : 0;

                if (name.startsWith("assets/")) assets++;
                if (name.startsWith("lib/")) {
                    libs++;
                    if (libList.length() < 2000) libList.append("  ").append(e.getName()).append("\n");
                }
                if (name.contains("unity") || name.endsWith(".assets") || name.endsWith(".unity3d") ||
                    name.contains("il2cpp") || name.contains("global-metadata")) {
                    unityFiles++;
                    if (engineFiles.length() < 1000) engineFiles.append("  ").append(e.getName()).append("\n");
                    engine = "Unity";
                }
                if (name.endsWith(".pak") || name.endsWith(".uasset") || name.endsWith(".umap") ||
                    name.contains("libue4") || name.contains("libunreal")) {
                    ueFiles++;
                    if (engineFiles.length() < 1000) engineFiles.append("  ").append(e.getName()).append("\n");
                    engine = "Unreal Engine";
                }
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".dds") ||
                    name.endsWith(".ktx") || name.endsWith(".etc") || name.endsWith(".astc")) textures++;
                if (name.endsWith(".ogg") || name.endsWith(".mp3") || name.endsWith(".wav") ||
                    name.endsWith(".m4a") || name.endsWith(".aac")) audio++;
                if (name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".ini") ||
                    name.endsWith(".cfg") || name.endsWith(".dat")) dataFiles++;
                if (name.contains("unity") && name.endsWith(".so")) engine = "Unity (IL2CPP)";
                if (name.contains("cocos")) engine = "Cocos2d";
                if (name.contains("godot")) engine = "Godot";
                if (assetSample.length() < 1500 && name.startsWith("assets/") && !name.endsWith("/"))
                    assetSample.append("  ").append(e.getName()).append("\n");
            }
            zis.close();

            sb.append("🎯 Engine: ").append(engine).append("\n");
            sb.append("📦 Total size: ").append(FileUtils.formatSize(totalSize)).append("\n\n");

            sb.append("📊 Statistics:\n");
            sb.append("   Assets:         ").append(assets).append("\n");
            sb.append("   Libraries:      ").append(libs).append("\n");
            sb.append("   Unity files:    ").append(unityFiles).append("\n");
            sb.append("   Unreal files:   ").append(ueFiles).append("\n");
            sb.append("   Textures:       ").append(textures).append("\n");
            sb.append("   Audio files:    ").append(audio).append("\n");
            sb.append("   Data files:     ").append(dataFiles).append("\n\n");

            if (engineFiles.length() > 0) {
                sb.append("🔍 Engine-specific files:\n").append(engineFiles).append("\n");
            }
            if (libList.length() > 0) {
                sb.append("⚙️ Native libraries:\n").append(libList).append("\n");
            }
            if (assetSample.length() > 0) {
                sb.append("🎨 Asset samples:\n").append(assetSample).append("\n");
            }
            sb.append("═══════════════════════════\n");
            output.setText(sb.toString());
        } catch (Exception ex) {
            output.setText("❌ " + ex.getMessage());
        }
    }
}
