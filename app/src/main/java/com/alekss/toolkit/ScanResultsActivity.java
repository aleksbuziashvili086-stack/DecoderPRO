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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ScanResultsActivity extends Activity {

    private static final int PICK = 1700;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";
    private File workDir;
    private List<File> dexFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_scan_results);

        output = findViewById(R.id.scan_output);
        progress = findViewById(R.id.scan_progress);
        Button pick = findViewById(R.id.btn_scan_pick);
        Button run = findViewById(R.id.btn_scan_run);
        Button copy = findViewById(R.id.btn_scan_copy);
        Button clear = findViewById(R.id.btn_scan_clear);

        progress.setVisibility(ProgressBar.GONE);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        run.setOnClickListener(v -> run());
        copy.setOnClickListener(v -> {
            if (lastResult.isEmpty()) { toast("ჯერ scan გაუშვი!"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("scan", lastResult));
            toast("✅ დაკოპირდა");
        });
        clear.setOnClickListener(v -> { output.setText(""); lastResult = ""; });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (req == PICK && res == RESULT_OK && d != null && d.getData() != null) {
            uri = d.getData();
            fileName = FileUtils.getFileName(this, uri);
            output.setText("✅ აირჩიე: " + fileName + "\n\n" +
                "დააჭირე PATTERN SCAN-ს.\n\n" +
                "აპი ავტომატურად მოძებნის:\n" +
                "coins, lives, premium, unlock,\n" +
                "ads, score, gems, weapon,\n" +
                "power, god mode, level...");
            lastResult = "";
        }
    }

    private void run() {
        if (uri == null) { toast("ჯერ აირჩიე APK!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ Pattern scan მიმდინარეობს...");
        new ScanTask().execute();
    }

    private class ScanTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            try {
                workDir = new File(getExternalFilesDir(null), "scan");
                if (workDir.exists()) deleteRecursive(workDir);
                workDir.mkdirs();

                File inputApk = new File(workDir, fileName.isEmpty() ? "input.apk" : fileName);

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

                // extract DEX files
                dexFiles.clear();
                publishProgress("📦 DEX ფაილების ამოღება...");

                InputStream is2 = getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is2);
                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    String name = e.getName();
                    if (name.endsWith(".dex") && !e.isDirectory()) {
                        File out = new File(workDir, name.replace("/", "_"));
                        FileOutputStream ofos = new FileOutputStream(out);
                        int len;
                        while ((len = zis.read(buf)) != -1) ofos.write(buf, 0, len);
                        ofos.close();
                        dexFiles.add(out);
                    }
                }
                zis.close();

                if (dexFiles.isEmpty()) return "❌ DEX ფაილი ვერ მოიძებნა";

                publishProgress("🔬 " + dexFiles.size() + " DEX ფაილი. Pattern scan...");

                PatternScanner.Result result = PatternScanner.scan(dexFiles, msg -> publishProgress(msg));

                return result.report.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — APK ძალიან დიდია";
            } catch (Throwable ex) {
                return "❌ " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
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
