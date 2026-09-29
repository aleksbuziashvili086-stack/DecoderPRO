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

public class DeepAnalysisActivity extends Activity {
    private static final int PICK = 1400;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_deep_analysis);
        output = findViewById(R.id.deep_output);
        progress = findViewById(R.id.deep_progress);
        Button pick = findViewById(R.id.btn_deep_pick);
        Button run = findViewById(R.id.btn_deep_run);
        Button copy = findViewById(R.id.btn_deep_copy);
        Button clear = findViewById(R.id.btn_deep_clear);
        progress.setVisibility(ProgressBar.GONE);

        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        run.setOnClickListener(v -> run());
        copy.setOnClickListener(v -> {
            if (lastResult.isEmpty()) { toast("ჯერ ანალიზი გაუშვი!"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("deep", lastResult));
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
                "🚀 Streaming analyzer — RAM-ში ~1.5MB\n" +
                "ხელს უწყობს 4GB+ APK-ს.\n\n" +
                "დააჭირე START-ს.");
            lastResult = "";
        }
    }

    private void run() {
        if (uri == null) { toast("ჯერ აირჩიე ფაილი!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ სკანირება (streaming)...");
        new DeepTask().execute();
    }

    private class DeepTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                StreamingAnalyzer.Report r = StreamingAnalyzer.scan(is, fileName, msg -> publishProgress(msg));
                return r.report.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — მაგრამ streaming-ში ეს არ უნდა მოხდეს";
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

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
