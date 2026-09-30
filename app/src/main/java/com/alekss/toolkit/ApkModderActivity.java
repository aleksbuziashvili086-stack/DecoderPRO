package com.alekss.toolkit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apktool.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

public class ApkModderActivity extends Activity {

    private static final int PICK = 1500;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private Button pickBtn, decodeBtn, clearBtn;
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
        pickBtn = findViewById(R.id.btn_modder_pick);
        decodeBtn = findViewById(R.id.btn_modder_decode);
        clearBtn = findViewById(R.id.btn_modder_clear);

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
            output.setText("✅ აირჩიე: " + fileName + "\n\n" +
                "დააჭირე DECODE-ს.");
            lastResult = "";
        }
    }

    private void startDecode() {
        if (uri == null) { toast("ჯერ აირჩიე APK!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ DECODE მიმდინარეობს...\n\n" +
            "• APK extract\n" +
            "• DEX → smali\n" +
            "• Resources decode\n" +
            "• Manifest decode\n\n" +
            "ეს შეიძლება 2-5 წუთი გაგრძელდეს.");
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

                decodedDir = new File(workDir, "decoded");
                decodedDir.mkdirs();

                publishProgress("🔧 apktool decode...");

                // apktool decode
                String[] args = new String[]{
                    "d",                              // decode
                    "-f",                             // force overwrite
                    "-o", decodedDir.getAbsolutePath(),
                    inputApk.getAbsolutePath()
                };

                // capture output
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                PrintStream orig = System.out;
                System.setOut(new PrintStream(baos));

                try {
                    Main.main(args);
                } catch (Throwable t) {
                    System.setOut(orig);
                    return "❌ apktool error: " + t.getMessage();
                } finally {
                    System.setOut(orig);
                }

                publishProgress("✅ decode დასრულდა!");

                // analyze result
                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║  APK DECODE RESULT                   ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n");
                sb.append("📁 Work dir: ").append(decodedDir.getAbsolutePath()).append("\n\n");

                // count files
                int smaliDirs = 0, smaliFiles = 0, resFiles = 0, assetFiles = 0, libFiles = 0;
                File manifest = null;
                File apktoolYml = null;

                File[] children = decodedDir.listFiles();
                if (children != null) {
                    for (File f : children) {
                        if (f.isDirectory()) {
                            if (f.getName().startsWith("smali")) {
                                smaliDirs++;
                                int[] counts = countFilesRecursive(f);
                                smaliFiles += counts[0];
                            } else if (f.getName().equals("res")) {
                                resFiles = countFilesRecursive(f)[0];
                            } else if (f.getName().equals("assets")) {
                                assetFiles = countFilesRecursive(f)[0];
                            } else if (f.getName().equals("lib")) {
                                libFiles = countFilesRecursive(f)[0];
                            }
                        } else if (f.getName().equals("AndroidManifest.xml")) {
                            manifest = f;
                        } else if (f.getName().equals("apktool.yml")) {
                            apktoolYml = f;
                        }
                    }
                }

                sb.append("━━━ DECODED STRUCTURE ━━━\n");
                sb.append("   smali dirs:     ").append(smaliDirs).append("\n");
                sb.append("   smali files:    ").append(smaliFiles).append("\n");
                sb.append("   resource files: ").append(resFiles).append("\n");
                sb.append("   asset files:    ").append(assetFiles).append("\n");
                sb.append("   native libs:    ").append(libFiles).append("\n");
                sb.append("   Manifest:       ").append(manifest != null ? "✅" : "❌").append("\n");
                sb.append("   apktool.yml:    ").append(apktoolYml != null ? "✅" : "❌").append("\n\n");

                // manifest preview
                if (manifest != null && manifest.exists()) {
                    sb.append("━━━ ANDROIDMANIFEST.XML (first 30 lines) ━━━\n");
                    try {
                        java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(manifest));
                        String line;
                        int count = 0;
                        while ((line = br.readLine()) != null && count < 30) {
                            sb.append("   ").append(line).append("\n");
                            count++;
                        }
                        br.close();
                    } catch (Exception ignored) {}
                    sb.append("\n");
                }

                sb.append("═══ READY FOR EDIT ═══\n");
                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — APK ძალიან დიდია";
            } catch (Exception e) {
                return "❌ " + e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }

        private int[] countFilesRecursive(File dir) {
            int files = 0, dirs = 0;
            File[] children = dir.listFiles();
            if (children == null) return new int[]{0, 0};
            for (File f : children) {
                if (f.isDirectory()) {
                    dirs++;
                    int[] sub = countFilesRecursive(f);
                    files += sub[0];
                    dirs += sub[1];
                } else {
                    files++;
                }
            }
            return new int[]{files, dirs};
        }

        @Override protected void onProgressUpdate(String... v) { output.setText(v[0]); }
        @Override protected void onPostExecute(String res) {
            lastResult = res;
            output.setText(res);
            progress.setVisibility(ProgressBar.GONE);
            toast("✅ decode დასრულდა");
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
