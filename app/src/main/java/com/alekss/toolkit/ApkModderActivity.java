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

public class ApkModderActivity extends Activity {

    private static final int PICK = 1500;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";
    private File workDir;
    private File inputApk;

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
        output.setText("⏳ DECODE მიმდინარეობს...\n\nეს შეიძლება 2-5 წუთი გაგრძელდეს.");
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

                publishProgress("📦 APK ზომა: " + FileUtils.formatSize(total));

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║  APK MODDER — STAGE 1                ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n");
                sb.append("📏 Size: ").append(FileUtils.formatSize(total)).append("\n");
                sb.append("📁 Work dir: ").append(workDir.getAbsolutePath()).append("\n\n");
                sb.append("━━━ STATUS ━━━\n");
                sb.append("   ✅ APK copied\n");
                sb.append("   ⏳ apktool API integration — stage 2\n\n");
                sb.append("ℹ️  apktool-lib ჩაშენებულია.\n");
                sb.append("   decode API stage 2-ში დაემატება.\n\n");
                sb.append("═══ READY ═══\n");

                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — APK ძალიან დიდია";
            } catch (Exception e) {
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
