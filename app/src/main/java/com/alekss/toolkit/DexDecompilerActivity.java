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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

public class DexDecompilerActivity extends Activity {

    private static final int PICK = 1000;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private Button pickBtn, decompileBtn, copyBtn, clearBtn;
    private String lastResult = "";
    private boolean running = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_dex_decompiler);

        output = findViewById(R.id.dd_output);
        progress = findViewById(R.id.dd_progress);
        pickBtn = findViewById(R.id.btn_dd_pick);
        decompileBtn = findViewById(R.id.btn_dd_decompile);
        copyBtn = findViewById(R.id.btn_dd_copy);
        clearBtn = findViewById(R.id.btn_dd_clear);

        progress.setVisibility(ProgressBar.GONE);

        pickBtn.setOnClickListener(v -> pickFile());
        decompileBtn.setOnClickListener(v -> startDecompile());
        copyBtn.setOnClickListener(v -> {
            if (lastResult.isEmpty()) { toast("ჯერ decompile გააკეთე!"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("jadx", lastResult));
            toast("✅ დაკოპირდა (" + lastResult.length() + " chars)");
        });
        clearBtn.setOnClickListener(v -> {
            output.setText("");
            lastResult = "";
        });
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
            output.setText("✅ აირჩიე: " + fileName + "\n\nდააჭირე DECOMPILE-ს დასაწყებად.");
            lastResult = "";
        }
    }

    private void startDecompile() {
        if (uri == null) { toast("ჯერ აირჩიე APK/DEX!"); return; }
        if (running) { toast("უკვე მიმდინარეობს..."); return; }
        running = true;
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ მიმდინარეობს decompile...\n\n" +
                       "ეს შეიძლება 30-180 წამი გაგრძელდეს.\n" +
                       "დიდი APK = მეტი დრო.\n\n" +
                       "ეკრანი არ ჩააქრო!");
        new DecompileTask().execute();
    }

    private class DecompileTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... voids) {
            File workDir = new File(getCacheDir(), "jadx_work");
            File inputFile = new File(workDir, fileName.isEmpty() ? "input.apk" : fileName);

            try {
                if (workDir.exists()) deleteRecursive(workDir);
                workDir.mkdirs();

                publishProgress("📥 ფაილის კოპირება...");
                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(inputFile);
                byte[] buf = new byte[16384];
                int n;
                long total = 0;
                while ((n = is.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                    total += n;
                }
                fos.close();
                is.close();
                publishProgress("✅ ფაილი: " + FileUtils.formatSize(total));

                publishProgress("⚙️ jadx ინიციალიზაცია...");
                JadxArgs args = new JadxArgs();
                args.setInputFile(inputFile);
                args.setOutDir(new File(workDir, "out"));
                args.setThreadsCount(Runtime.getRuntime().availableProcessors());
                args.setShowInconsistentCode(true);
                args.setSkipResources(true);

                publishProgress("🔬 DEX ანალიზი და decompilation...");
                JadxDecompiler jadx = new JadxDecompiler(args);
                jadx.load();

                publishProgress("📝 კლასების ჩამოთვლა...");
                List<JavaClass> classes = jadx.getClasses();

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║   JADX DECOMPILER — FULL REPORT      ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n");
                sb.append("📦 Classes: ").append(classes.size()).append("\n");
                sb.append("🧵 Threads: ").append(args.getThreadsCount()).append("\n\n");

                // Group by package
                java.util.TreeMap<String, Integer> packages = new java.util.TreeMap<>();
                for (JavaClass cls : classes) {
                    String full = cls.getFullName();
                    int lastDot = full.lastIndexOf('.');
                    String pkg = lastDot > 0 ? full.substring(0, lastDot) : "(default)";
                    packages.put(pkg, packages.getOrDefault(pkg, 0) + 1);
                }
                sb.append("📦 PACKAGES (").append(packages.size()).append("):\n");
                int pkgCount = 0;
                for (java.util.Map.Entry<String, Integer> e : packages.entrySet()) {
                    sb.append("   ").append(e.getKey())
                      .append("  (").append(e.getValue()).append(")\n");
                    pkgCount++;
                    if (pkgCount >= 50) { sb.append("   ... +").append(packages.size() - 50).append(" more\n"); break; }
                }
                sb.append("\n");

                // Decompile each class
                sb.append("═══════════════════════════════════════\n");
                sb.append("📜 DECOMPILED SOURCE:\n");
                sb.append("═══════════════════════════════════════\n\n");

                int decompiled = 0;
                int failed = 0;
                int charLimit = 400000; // ~400KB cap
                StringBuilder codeBuilder = new StringBuilder();

                for (JavaClass cls : classes) {
                    if (codeBuilder.length() > charLimit) {
                        codeBuilder.append("\n\n... [OUTPUT TRIMMED — ").append(classes.size() - decompiled).append(" classes remaining]\n");
                        break;
                    }
                    try {
                        String code = cls.getCode();
                        codeBuilder.append("// ─────────────────────────────────────\n");
                        codeBuilder.append("// ").append(cls.getFullName()).append("\n");
                        codeBuilder.append("// ─────────────────────────────────────\n");
                        codeBuilder.append(code).append("\n\n");
                        decompiled++;
                    } catch (Exception e) {
                        failed++;
                    }
                }

                jadx.close();

                sb.append("✅ Decompiled: ").append(decompiled).append(" classes\n");
                if (failed > 0) sb.append("⚠️ Failed: ").append(failed).append(" classes\n");
                sb.append("\n");
                sb.append(codeBuilder);

                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ Out of memory!\n\n" +
                       "APK ძალიან დიდია decompile-სთვის.\n" +
                       "სცადე პატარა APK.";
            } catch (Exception e) {
                return "❌ შეცდომა: " + e.getClass().getSimpleName() + "\n" + e.getMessage();
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
            toast("✅ დასრულდა! " + (lastResult.length() / 1024) + " KB");
        }
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
        f.delete();
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
