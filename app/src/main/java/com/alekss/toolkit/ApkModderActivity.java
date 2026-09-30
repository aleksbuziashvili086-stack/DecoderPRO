package com.alekss.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkModderActivity extends Activity {

    private static final int PICK = 1500;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";
    private File workDir;
    private File inputApk;
    private File decodedDir;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_apk_modder);
        output = findViewById(R.id.modder_output);
        progress = findViewById(R.id.modder_progress);
        Button pickBtn = findViewById(R.id.btn_modder_pick);
        Button decodeBtn = findViewById(R.id.btn_modder_decode);
        Button clearBtn = findViewById(R.id.btn_modder_clear);
        progress.setVisibility(ProgressBar.GONE);
        pickBtn.setOnClickListener(v -> pickFile());
        decodeBtn.setOnClickListener(v -> startDecode());
        clearBtn.setOnClickListener(v -> { output.setText(""); lastResult = ""; });
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, PICK);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == PICK && res == RESULT_OK && d != null && d.getData() != null) {
            uri = d.getData();
            fileName = FileUtils.getFileName(this, uri);
            output.setText("✅ აირჩიე: " + fileName + "\n\nდააჭირე DECODE-ს.");
            lastResult = "";
        }
    }

    private void startDecode() {
        if (uri == null) { toast("ჯერ აირჩიე APK!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ APK extract მიმდინარეობს...");
        new DecodeTask().execute();
    }

    private class DecodeTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            try {
                workDir = new File(getExternalFilesDir(null), "apkmodder");
                if (workDir.exists()) deleteRecursive(workDir);
                workDir.mkdirs();

                inputApk = new File(workDir, fileName.isEmpty() ? "input.apk" : fileName);

                publishProgress("📥 APK კოპირება...");
                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(inputApk);
                byte[] buf = new byte[16384];
                int n;
                long total = 0;
                while ((n = is.read(buf)) != -1) { fos.write(buf, 0, n); total += n; }
                fos.close();
                is.close();

                publishProgress("📦 ზომა: " + FileUtils.formatSize(total));

                decodedDir = new File(workDir, "decoded");
                decodedDir.mkdirs();

                publishProgress("📦 ZIP extract...");

                int dexCount = 0, soCount = 0, assetCount = 0, resCount = 0, xmlCount = 0;
                long dexTotalSize = 0, soTotalSize = 0;
                StringBuilder dexList = new StringBuilder();
                StringBuilder soList = new StringBuilder();

                InputStream is2 = getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is2);
                ZipEntry e;

                while ((e = zis.getNextEntry()) != null) {
                    String name = e.getName();
                    String lower = name.toLowerCase();

                    if (name.equals("AndroidManifest.xml") ||
                        name.equals("resources.arsc") ||
                        (name.startsWith("classes") && name.endsWith(".dex"))) {
                        File out = new File(decodedDir, name);
                        out.getParentFile().mkdirs();
                        FileOutputStream ofos = new FileOutputStream(out);
                        int len;
                        while ((len = zis.read(buf)) != -1) ofos.write(buf, 0, len);
                        ofos.close();
                    }

                    if (name.endsWith(".dex")) {
                        dexCount++;
                        long sz = e.getSize();
                        if (sz > 0) dexTotalSize += sz;
                        if (dexList.length() < 1000) dexList.append("   ").append(name)
                            .append("  (").append(FileUtils.formatSize(sz)).append(")\n");
                    } else if (name.endsWith(".so")) {
                        soCount++;
                        long sz = e.getSize();
                        if (sz > 0) soTotalSize += sz;
                        if (soList.length() < 1500) soList.append("   ").append(name)
                            .append("  (").append(FileUtils.formatSize(sz)).append(")\n");
                    } else if (lower.startsWith("assets/")) {
                        assetCount++;
                    } else if (lower.startsWith("res/")) {
                        resCount++;
                    } else if (lower.endsWith(".xml")) {
                        xmlCount++;
                    }
                }
                zis.close();

                publishProgress("✅ extract დასრულდა");

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║  APK EXTRACT — STAGE 2               ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n");
                sb.append("📏 Size: ").append(FileUtils.formatSize(total)).append("\n");
                sb.append("📁 Work dir: ").append(workDir.getAbsolutePath()).append("\n\n");

                sb.append("━━━ STRUCTURE ━━━\n");
                sb.append("   DEX files:      ").append(dexCount)
                  .append("  (").append(FileUtils.formatSize(dexTotalSize)).append(")\n");
                sb.append("   Native .so:     ").append(soCount)
                  .append("  (").append(FileUtils.formatSize(soTotalSize)).append(")\n");
                sb.append("   Asset files:    ").append(assetCount).append("\n");
                sb.append("   Resource files: ").append(resCount).append("\n");
                sb.append("   XML files:      ").append(xmlCount).append("\n\n");

                if (dexList.length() > 0) {
                    sb.append("━━━ DEX FILES ━━━\n").append(dexList).append("\n");
                }
                if (soList.length() > 0) {
                    sb.append("━━━ NATIVE LIBS (first 20) ━━━\n").append(soList).append("\n");
                }

                File manifest = new File(decodedDir, "AndroidManifest.xml");
                File arsc = new File(decodedDir, "resources.arsc");

                sb.append("━━━ EXTRACTED ━━━\n");
                sb.append("   AndroidManifest: ").append(manifest.exists() ? "✅" : "❌").append("\n");
                sb.append("   resources.arsc:  ").append(arsc.exists() ? "✅" : "❌").append("\n\n");

                sb.append("═══ READY FOR DEX ANALYSIS ═══\n");
                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — APK ძალიან დიდია";
            } catch (Throwable e) {
                return "❌ " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        @Override protected void onProgressUpdate(String... v) { output.setText(v[0]); }
        @Override protected void onPostExecute(String res) {
            lastResult = res;
            output.setText(res);
            progress.setVisibility(ProgressBar.GONE);
            toast("✅ დასრულდა");
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] c = f.listFiles();
            if (c != null) for (File x : c) deleteRecursive(x);
        }
        f.delete();
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
