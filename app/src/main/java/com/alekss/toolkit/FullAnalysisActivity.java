package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FullAnalysisActivity extends Activity {

    private static final int PICK = 2000;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";
    private boolean running = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_full_analysis);
        output = findViewById(R.id.fa_output);
        progress = findViewById(R.id.fa_progress);
        Button pick = findViewById(R.id.btn_fa_pick);
        Button run = findViewById(R.id.btn_fa_run);
        Button copy = findViewById(R.id.btn_fa_copy);
        Button clear = findViewById(R.id.btn_fa_clear);

        progress.setVisibility(ProgressBar.GONE);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        run.setOnClickListener(v -> runAnalysis());
        copy.setOnClickListener(v -> {
            if (lastResult.isEmpty()) { toast("ჯერ სკანირება გაუშვი!"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("full", lastResult));
            toast("✅ დაკოპირდა (" + (lastResult.length()/1024) + " KB)");
        });
        clear.setOnClickListener(v -> { output.setText(""); lastResult = ""; });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == PICK && res == RESULT_OK && d != null && d.getData() != null) {
            uri = d.getData();
            fileName = FileUtils.getFileName(this, uri);
            output.setText("✅ აირჩიე: " + fileName + "\n\nდააჭირე START ANALYSIS-ს.");
            lastResult = "";
        }
    }

    private void runAnalysis() {
        if (uri == null) { toast("ჯერ აირჩიე APK!"); return; }
        if (running) { toast("უკვე მიმდინარეობს..."); return; }
        running = true;
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ სრული სკანირება მიმდინარეობს...\n\n" +
                       "• Recursive scan\n" +
                       "• File type detection\n" +
                       "• Engine detection\n" +
                       "• Auto findings\n\n" +
                       "ეკრანი არ ჩააქრო!");
        new FullScanTask().execute();
    }

    private class FullScanTask extends AsyncTask<Void, String, String> {

        @Override
        protected String doInBackground(Void... voids) {
            try {
                publishProgress("📦 ZIP სკანირება...");

                // Counters
                int totalEntries = 0, fileCount = 0, dirCount = 0;
                long totalSize = 0, uncompressedSize = 0;

                // Categorization
                int dexCount = 0, soCount = 0, assetCount = 0, resCount = 0;
                int jsonCount = 0, xmlCount = 0, dbCount = 0, pakCount = 0;
                int obbCount = 0, bundleCount = 0, pngCount = 0, jpgCount = 0;
                int audioCount = 0, txtCount = 0, binCount = 0, datCount = 0;
                int arscCount = 0, metaCount = 0;

                // Engine indicators
                boolean hasIl2cpp = false;
                boolean hasUnity = false;
                boolean hasUnreal = false;
                boolean hasGodot = false;
                boolean hasCocos = false;
                boolean hasGlobalMetadata = false;
                boolean hasUnity3d = false;
                boolean hasPak = false;
                boolean hasMonobundle = false;

                // Findings
                List<String> findings = new ArrayList<>();
                TreeSet<String> soPaths = new TreeSet<>();
                TreeSet<String> unityFiles = new TreeSet<>();
                TreeSet<String> unrealFiles = new TreeSet<>();
                TreeSet<String> dbFiles = new TreeSet<>();
                TreeSet<String> configFiles = new TreeSet<>();
                TreeMap<String, Integer> extCount = new TreeMap<>();

                InputStream is = getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry e;

                while ((e = zis.getNextEntry()) != null) {
                    totalEntries++;
                    String name = e.getName();
                    String lower = name.toLowerCase();
                    long size = e.getSize();
                    long comp = e.getCompressedSize();
                    if (size > 0) uncompressedSize += size;
                    if (comp > 0) totalSize += comp;

                    if (e.isDirectory()) { dirCount++; continue; }
                    fileCount++;

                    // Extension counter
                    int dot = lower.lastIndexOf('.');
                    if (dot > 0) {
                        String ext = lower.substring(dot);
                        extCount.put(ext, extCount.getOrDefault(ext, 0) + 1);
                    }

                    // Categorize
                    if (lower.endsWith(".dex")) dexCount++;
                    else if (lower.endsWith(".so")) {
                        soCount++;
                        soPaths.add(name);
                    }
                    else if (lower.startsWith("assets/")) {
                        assetCount++;
                        if (lower.endsWith(".json")) jsonCount++;
                        if (lower.endsWith(".txt")) txtCount++;
                        if (lower.endsWith(".bin")) binCount++;
                        if (lower.endsWith(".dat")) datCount++;
                    }
                    else if (lower.startsWith("res/")) {
                        resCount++;
                        if (lower.endsWith(".png")) pngCount++;
                        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) jpgCount++;
                    }
                    else if (lower.endsWith(".xml")) xmlCount++;
                    else if (lower.endsWith(".db") || lower.endsWith(".sqlite") ||
                             lower.endsWith(".sqlite3")) {
                        dbCount++;
                        dbFiles.add(name);
                    }
                    else if (lower.endsWith(".pak")) {
                        pakCount++;
                        unrealFiles.add(name);
                    }
                    else if (lower.endsWith(".obb")) obbCount++;
                    else if (lower.endsWith(".bundle")) {
                        bundleCount++;
                        unityFiles.add(name);
                    }
                    else if (lower.endsWith(".assets") || lower.endsWith(".resource")) {
                        unityFiles.add(name);
                    }
                    else if (lower.endsWith(".ogg") || lower.endsWith(".mp3") ||
                             lower.endsWith(".wav") || lower.endsWith(".m4a")) audioCount++;

                    if (name.equals("resources.arsc")) arscCount++;
                    if (name.startsWith("META-INF/") && (lower.endsWith(".rsa") ||
                        lower.endsWith(".dsa") || lower.endsWith(".ec") ||
                        lower.endsWith(".sf") || lower.endsWith("manifest.mf"))) metaCount++;

                    // Engine detection
                    if (lower.contains("libil2cpp") || lower.contains("il2cpp")) hasIl2cpp = true;
                    if (lower.contains("libunity") || lower.contains("unityplayer")) hasUnity = true;
                    if (lower.contains("libue4") || lower.contains("libunreal")) hasUnreal = true;
                    if (lower.contains("godot")) hasGodot = true;
                    if (lower.contains("cocos")) hasCocos = true;
                    if (lower.contains("global-metadata")) {
                        hasGlobalMetadata = true;
                        unityFiles.add(name);
                    }
                    if (lower.endsWith("data.unity3d") || lower.contains("unity3d")) hasUnity3d = true;
                    if (lower.endsWith(".pak")) hasPak = true;
                    if (lower.contains("monobundle") || lower.contains(".mono/")) hasMonobundle = true;

                    // Config files
                    if (lower.endsWith(".json") || lower.endsWith(".xml") ||
                        lower.endsWith(".ini") || lower.endsWith(".cfg")) {
                        if (configFiles.size() < 50) configFiles.add(name);
                    }
                }
                zis.close();

                // ═══════════════════════════════════════════
                // BUILD REPORT
                // ═══════════════════════════════════════════

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════════╗\n");
                sb.append("║   FULL ANALYSIS REPORT                   ║\n");
                sb.append("║   Decoder PRO — Alekss Toolkit           ║\n");
                sb.append("╚══════════════════════════════════════════╝\n\n");

                // 1. FILE OVERVIEW
                sb.append("━━━ 1. FILE OVERVIEW ━━━\n");
                sb.append("📄 Name:         ").append(fileName).append("\n");
                sb.append("📏 Compressed:   ").append(FileUtils.formatSize(totalSize)).append("\n");
                sb.append("📏 Uncompressed: ").append(FileUtils.formatSize(uncompressedSize)).append("\n");
                sb.append("📊 Entries:      ").append(totalEntries).append("\n");
                sb.append("   Files:        ").append(fileCount).append("\n");
                sb.append("   Directories:  ").append(dirCount).append("\n");
                sb.append("   Ratio:        ").append(String.format("%.1f%%",
                    uncompressedSize > 0 ? (totalSize * 100.0 / uncompressedSize) : 0)).append("\n\n");

                // 2. STRUCTURE
                sb.append("━━━ 2. CONTAINER STRUCTURE ━━━\n");
                sb.append("   DEX files:        ").append(dexCount).append("\n");
                sb.append("   Native .so:       ").append(soCount).append("\n");
                sb.append("   Assets:           ").append(assetCount).append("\n");
                sb.append("   Resources:        ").append(resCount).append("\n");
                sb.append("   resources.arsc:   ").append(arscCount > 0 ? "Present ✅" : "Missing").append("\n");
                sb.append("   META-INF:         ").append(metaCount).append(" files\n");
                sb.append("   Images (PNG):     ").append(pngCount).append("\n");
                sb.append("   Images (JPG):     ").append(jpgCount).append("\n");
                sb.append("   Audio files:      ").append(audioCount).append("\n");
                sb.append("   JSON files:       ").append(jsonCount).append("\n");
                sb.append("   XML files:        ").append(xmlCount).append("\n");
                sb.append("   Text files:       ").append(txtCount).append("\n");
                sb.append("   BIN files:        ").append(binCount).append("\n");
                sb.append("   DAT files:        ").append(datCount).append("\n");
                sb.append("   Databases (.db):  ").append(dbCount).append("\n");
                sb.append("   PAK files:        ").append(pakCount).append("\n");
                sb.append("   OBB files:        ").append(obbCount).append("\n");
                sb.append("   AssetBundles:     ").append(bundleCount).append("\n\n");

                // 3. AUTO FINDINGS
                sb.append("━━━ 3. AUTO FINDINGS ━━━\n");

                // Engine detection
                if (hasIl2cpp) findings.add("🧬 Unity IL2CPP detected — libil2cpp.so present");
                if (hasUnity) findings.add("🎮 Unity engine detected — libunity.so present");
                if (hasUnreal) findings.add("🎮 Unreal Engine detected — libUE4.so present");
                if (hasGodot) findings.add("🎮 Godot engine detected");
                if (hasCocos) findings.add("🎮 Cocos2d engine detected");
                if (hasGlobalMetadata) findings.add("🧬 Unity IL2CPP metadata — assets/bin/Data/global-metadata.dat");
                if (hasUnity3d) findings.add("🧬 Unity asset bundle — data.unity3d");
                if (hasPak) findings.add("📦 Unreal-style package — *.pak files present");
                if (hasMonobundle) findings.add("🧬 Unity Mono runtime — .mono/ directory");

                // File-specific findings
                if (dbCount > 0) findings.add("🗄️  SQLite database(s) detected — " + dbCount + " file(s)");
                if (obbCount > 0) findings.add("📦 OBB expansion file(s) — " + obbCount);
                if (arscCount > 0) findings.add("📋 resources.arsc present — resource table");
                if (metaCount > 0) findings.add("🔐 APK signed — META-INF/ present (" + metaCount + " files)");
                else findings.add("⚠️  No META-INF/ — unsigned or unusual APK");

                if (dexCount > 1) findings.add("📦 Multidex — " + dexCount + " DEX files");
                if (dexCount == 0) findings.add("⚠️  No DEX files — unusual for APK");

                if (soCount > 0) findings.add("⚙️  Native libraries — " + soCount + " .so files");
                if (assetCount > 1000) findings.add("📁 Large assets folder — " + assetCount + " files");
                if (configFiles.size() > 20) findings.add("⚙️  Many config files — " + configFiles.size() + "+");

                if (findings.isEmpty()) sb.append("   (ვერაფერი ვერ მოიძებნა)\n");
                else for (String f : findings) sb.append("   ").append(f).append("\n");
                sb.append("\n");

                // 4. ENGINE VERDICT
                sb.append("━━━ 4. ENGINE VERDICT ━━━\n");
                String engine = "Unknown";
                String confidence = "low";
                StringBuilder evidence = new StringBuilder();

                if (hasIl2cpp && hasGlobalMetadata) {
                    engine = "Unity (IL2CPP)";
                    confidence = "★★★★★ HIGH";
                    evidence.append("     ✓ libil2cpp.so\n");
                    evidence.append("     ✓ global-metadata.dat\n");
                } else if (hasUnity) {
                    engine = "Unity";
                    confidence = "★★★★☆ MEDIUM";
                    evidence.append("     ✓ libunity.so\n");
                } else if (hasUnreal || hasPak) {
                    engine = "Unreal Engine";
                    confidence = "★★★★☆ MEDIUM";
                    if (hasUnreal) evidence.append("     ✓ libUE4.so\n");
                    if (hasPak) evidence.append("     ✓ .pak files\n");
                } else if (hasGodot) {
                    engine = "Godot";
                    confidence = "★★★☆☆ MEDIUM";
                    evidence.append("     ✓ godot references\n");
                } else if (hasCocos) {
                    engine = "Cocos2d";
                    confidence = "★★★☆☆ MEDIUM";
                    evidence.append("     ✓ cocos references\n");
                } else {
                    engine = "Native Android (no game engine)";
                    confidence = "★★☆☆☆";
                }

                sb.append("   Engine:      ").append(engine).append("\n");
                sb.append("   Confidence:  ").append(confidence).append("\n");
                if (evidence.length() > 0) {
                    sb.append("   Evidence:\n").append(evidence);
                }
                sb.append("\n");

                // 5. NATIVE LIBRARIES
                if (!soPaths.isEmpty()) {
                    sb.append("━━━ 5. NATIVE LIBRARIES ━━━\n");
                    int shown = 0;
                    for (String p : soPaths) {
                        sb.append("   ").append(p).append("\n");
                        shown++;
                        if (shown >= 30) {
                            sb.append("   ... +").append(soPaths.size() - 30).append(" more\n");
                            break;
                        }
                    }
                    sb.append("\n");
                }

                // 6. UNITY FILES
                if (!unityFiles.isEmpty()) {
                    sb.append("━━━ 6. UNITY / ENGINE FILES ━━━\n");
                    int shown = 0;
                    for (String p : unityFiles) {
                        sb.append("   ").append(p).append("\n");
                        shown++;
                        if (shown >= 20) {
                            sb.append("   ... +").append(unityFiles.size() - 20).append(" more\n");
                            break;
                        }
                    }
                    sb.append("\n");
                }

                // 7. UNREAL FILES
                if (!unrealFiles.isEmpty()) {
                    sb.append("━━━ 7. UNREAL FILES ━━━\n");
                    int shown = 0;
                    for (String p : unrealFiles) {
                        sb.append("   ").append(p).append("\n");
                        shown++;
                        if (shown >= 20) {
                            sb.append("   ... +").append(unrealFiles.size() - 20).append(" more\n");
                            break;
                        }
                    }
                    sb.append("\n");
                }

                // 8. DATABASES
                if (!dbFiles.isEmpty()) {
                    sb.append("━━━ 8. DATABASES ━━━\n");
                    for (String p : dbFiles) {
                        sb.append("   ").append(p).append("\n");
                    }
                    sb.append("   ℹ️  SQLite schema inspection — soon\n\n");
                }

                // 9. FILE TYPES
                sb.append("━━━ 9. FILE TYPE DISTRIBUTION ━━━\n");
                int extShown = 0;
                List<Map.Entry<String, Integer>> extList = new ArrayList<>(extCount.entrySet());
                extList.sort((a, b) -> b.getValue() - a.getValue());
                for (Map.Entry<String, Integer> entry : extList) {
                    sb.append(String.format("   %-12s %d\n", entry.getKey(), entry.getValue()));
                    extShown++;
                    if (extShown >= 25) {
                        sb.append("   ... +").append(extList.size() - 25).append(" more\n");
                        break;
                    }
                }
                sb.append("\n");

                // 10. WARNINGS
                sb.append("━━━ 10. WARNINGS / OBSERVATIONS ━━━\n");
                if (metaCount == 0) sb.append("   ⚠️  No signature files found\n");
                if (dexCount == 0) sb.append("   ⚠️  No DEX — not a standard APK\n");
                if (soCount > 0 && !hasIl2cpp && !hasUnreal && !hasUnity && !hasCocos)
                    sb.append("   ℹ️  Native libs without known engine\n");
                if (assetCount > 5000) sb.append("   ℹ️  Very large assets folder\n");
                sb.append("   ✅ Scan completed\n\n");

                // 11. SUMMARY
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("📌 SUMMARY:\n");
                sb.append("   Engine:       ").append(engine).append("\n");
                sb.append("   DEX:          ").append(dexCount).append(" files\n");
                sb.append("   Native libs:  ").append(soCount).append("\n");
                sb.append("   Assets:       ").append(assetCount).append("\n");
                sb.append("   Resources:    ").append(resCount).append("\n");
                sb.append("   Findings:     ").append(findings.size()).append("\n");
                sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                sb.append("Generated by Decoder PRO v1.0 — Alekss Toolkit\n");

                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ Out of memory — APK too large";
            } catch (Exception e) {
                return "❌ Error: " + e.getClass().getSimpleName() + "\n" + e.getMessage();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            output.setText(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            lastResult = result;
            output.setText(result);
            progress.setVisibility(ProgressBar.GONE);
            running = false;
            toast("✅ დასრულდა!");
        }
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
