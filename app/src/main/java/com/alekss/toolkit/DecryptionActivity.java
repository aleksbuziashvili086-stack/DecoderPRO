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

public class DecryptionActivity extends Activity {
    private static final int PICK = 1300;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_decryption);
        output = findViewById(R.id.dec_output);
        progress = findViewById(R.id.dec_progress);
        Button pick = findViewById(R.id.btn_dec_pick);
        Button run = findViewById(R.id.btn_dec_run);
        Button copy = findViewById(R.id.btn_dec_copy);
        Button clear = findViewById(R.id.btn_dec_clear);
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
            cm.setPrimaryClip(ClipData.newPlainText("dec", lastResult));
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
            output.setText("✅ აირჩიე: " + fileName + "\n\nდააჭირე ANALYZE-ს.");
            lastResult = "";
        }
    }

    private void run() {
        if (uri == null) { toast("ჯერ აირჩიე ფაილი!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ მიმდინარეობს ანალიზი...");
        new DecTask().execute();
    }

    private class DecTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            try {
                publishProgress("📥 ფაილის წაკითხვა...");
                byte[] data = FileUtils.readFileBytes(DecryptionActivity.this, uri);
                publishProgress("🔬 ანალიზი...");
                DecryptionEngine.Result r = DecryptionEngine.analyze(data, fileName);
                StringBuilder sb = r.report;
                sb.append("\n\n━━━ XOR CANDIDATES ━━━\n");
                for (String s : r.xorCandidates) sb.append("   ").append(s).append("\n");
                sb.append("\n━━━ CAESAR SHIFTS ━━━\n");
                for (String s : r.caesarCandidates) sb.append("   ").append(s).append("\n");
                sb.append("\n━━━ STRINGS ━━━\n");
                for (String s : r.foundStrings) sb.append("   ").append(s).append("\n");
                return sb.toString();
            } catch (OutOfMemoryError e) { return "❌ OOM — ფაილი ძალიან დიდია"; }
            catch (Exception e) { return "❌ " + e.getMessage(); }
        }
        @Override protected void onProgressUpdate(String... v) { output.setText(v[0]); }
        @Override protected void onPostExecute(String res) {
            lastResult = res; output.setText(res);
            progress.setVisibility(ProgressBar.GONE);
            toast("✅ დასრულდა");
        }
    }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
