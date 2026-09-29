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

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DexDecompilerActivity extends Activity {

    private static final int PICK = 1000;
    private Uri uri;
    private String fileName = "";
    private TextView output;
    private ProgressBar progress;
    private String lastResult = "";
    private boolean running = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_dex_decompiler);

        output = findViewById(R.id.dd_output);
        progress = findViewById(R.id.dd_progress);
        Button pickBtn = findViewById(R.id.btn_dd_pick);
        Button decompileBtn = findViewById(R.id.btn_dd_decompile);
        Button copyBtn = findViewById(R.id.btn_dd_copy);
        Button clearBtn = findViewById(R.id.btn_dd_clear);

        progress.setVisibility(ProgressBar.GONE);

        pickBtn.setOnClickListener(v -> pickFile());
        decompileBtn.setOnClickListener(v -> startAnalyze());
        copyBtn.setOnClickListener(v -> {
            if (lastResult.isEmpty()) { toast("ჯერ ანალიზი გააკეთე!"); return; }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("dex", lastResult));
            toast("✅ დაკოპირდა (" + (lastResult.length()/1024) + " KB)");
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
            output.setText("✅ აირჩიე: " + fileName + "\n\nდააჭირე DECOMPILE-ს.");
            lastResult = "";
        }
    }

    private void startAnalyze() {
        if (uri == null) { toast("ჯერ აირჩიე ფაილი!"); return; }
        if (running) { toast("უკვე მიმდინარეობს..."); return; }
        running = true;
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ მიმდინარეობს DEX ანალიზი...\n\n30-90 წამი.\n\nეკრანი არ ჩააქრო!");
        new DexTask().execute();
    }

    private class DexTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... voids) {
            File workDir = new File(getCacheDir(), "dex_work");
            try {
                if (workDir.exists()) deleteRecursive(workDir);
                workDir.mkdirs();

                publishProgress("📥 ფაილის კოპირება...");
                File inputFile = new File(workDir, fileName.isEmpty() ? "input.apk" : fileName);
                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = new FileOutputStream(inputFile);
                byte[] buf = new byte[16384];
                int n;
                long total = 0;
                while ((n = is.read(buf)) != -1) { fos.write(buf, 0, n); total += n; }
                fos.close(); is.close();
                publishProgress("✅ ფაილი: " + FileUtils.formatSize(total));

                publishProgress("📦 DEX ფაილების ამოღება...");
                List<File> dexFiles = new ArrayList<>();
                if (inputFile.getName().toLowerCase().endsWith(".dex")) {
                    dexFiles.add(inputFile);
                } else {
                    ZipInputStream zis = new ZipInputStream(
                        getContentResolver().openInputStream(uri));
                    ZipEntry e;
                    while ((e = zis.getNextEntry()) != null) {
                        String name = e.getName();
                        if (name.endsWith(".dex") && !e.isDirectory()) {
                            File out = new File(workDir, name.replace("/", "_"));
                            FileOutputStream outFos = new FileOutputStream(out);
                            byte[] b = new byte[16384];
                            int len;
                            while ((len = zis.read(b)) != -1) outFos.write(b, 0, len);
                            outFos.close();
                            dexFiles.add(out);
                        }
                    }
                    zis.close();
                }

                if (dexFiles.isEmpty()) return "❌ DEX ფაილი ვერ მოიძებნა";

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║   DEX INSPECTOR — dexlib2 engine     ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n");
                sb.append("📦 DEX files: ").append(dexFiles.size()).append("\n\n");

                int totalClasses = 0, totalMethods = 0, totalFields = 0;
                TreeMap<String, Integer> packages = new TreeMap<>();
                StringBuilder classesDump = new StringBuilder();
                StringBuilder methodsDump = new StringBuilder();

                for (File dexFile : dexFiles) {
                    publishProgress("🔬 " + dexFile.getName() + "...");
                    DexFile dex = DexFileFactory.loadDexFile(dexFile, Opcodes.forApi(24));

                    for (ClassDef cls : dex.getClasses()) {
                        totalClasses++;
                        String type = cls.getType();
                        String className = type.substring(1, type.length() - 1).replace('/', '.');
                        String pkg = className.contains(".")
                            ? className.substring(0, className.lastIndexOf('.'))
                            : "(default)";
                        packages.put(pkg, packages.getOrDefault(pkg, 0) + 1);

                        classesDump.append("L ").append(className).append("\n");

                        for (Field f : cls.getFields()) {
                            totalFields++;
                        }

                        for (Method m : cls.getMethods()) {
                            totalMethods++;
                            String mName = m.getName();
                            StringBuilder params = new StringBuilder();
                            for (CharSequence p : m.getParameterTypes()) {
                                if (params.length() > 0) params.append(", ");
                                params.append(shortType(p.toString()));
                            }
                            String ret = shortType(m.getReturnType());
                            methodsDump.append("M ").append(className)
                                .append("::").append(mName)
                                .append("(").append(params).append(")")
                                .append(ret).append("\n");
                        }
                    }
                }

                sb.append("📊 STATISTICS:\n");
                sb.append("   Classes: ").append(totalClasses).append("\n");
                sb.append("   Methods: ").append(totalMethods).append("\n");
                sb.append("   Fields:  ").append(totalFields).append("\n");
                sb.append("   Packages: ").append(packages.size()).append("\n\n");

                sb.append("📦 PACKAGES (top 50):\n");
                int pkgCount = 0;
                for (java.util.Map.Entry<String, Integer> e : packages.entrySet()) {
                    sb.append("   ").append(e.getKey())
                      .append("  (").append(e.getValue()).append(")\n");
                    pkgCount++;
                    if (pkgCount >= 50) {
                        sb.append("   ... +").append(packages.size() - 50).append(" more\n");
                        break;
                    }
                }
                sb.append("\n");

                sb.append("═══════════════════════════════════════\n");
                sb.append("🔧 METHODS (first 200):\n");
                sb.append("═══════════════════════════════════════\n");
                String[] methodLines = methodsDump.toString().split("\n");
                int limit = Math.min(methodLines.length, 200);
                for (int i = 0; i < limit; i++) sb.append(methodLines[i]).append("\n");
                if (methodLines.length > 200)
                    sb.append("... +").append(methodLines.length - 200).append(" more methods\n");
                sb.append("\n");

                sb.append("═══════════════════════════════════════\n");
                sb.append("🏛️ CLASSES:\n");
                sb.append("═══════════════════════════════════════\n");
                String[] classLines = classesDump.toString().split("\n");
                limit = Math.min(classLines.length, 500);
                for (int i = 0; i < limit; i++) sb.append(classLines[i]).append("\n");
                if (classLines.length > 500)
                    sb.append("... +").append(classLines.length - 500).append(" more classes\n");

                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ Out of memory!\n\nAPK ძალიან დიდია.";
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
            toast("✅ დასრულდა: " + (lastResult.length()/1024) + " KB");
        }
    }

    private String shortType(String desc) {
        if (desc == null) return "?";
        if (desc.startsWith("L") && desc.endsWith(";"))
            return desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
        if (desc.startsWith("[")) return desc;
        switch (desc) {
            case "V": return "void";
            case "Z": return "boolean";
            case "B": return "byte";
            case "S": return "short";
            case "C": return "char";
            case "I": return "int";
            case "J": return "long";
            case "F": return "float";
            case "D": return "double";
        }
        return desc;
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
        f.delete();
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
