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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DecryptionActivity extends Activity {
    private static final int PICK = 1300;
    private static final int PER_FILE = 256 * 1024;
    private static final int MAX_FILES = 800;
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
            output.setText("✅ აირჩიე: " + fileName + "\n\n" +
                "🔧 Streaming რეჟიმი — მეხსიერება არ ივსება\n" +
                "📦 თითო ფაილი 256KB-მდე ანალიზდება\n\n" +
                "დააჭირე START-ს.");
            lastResult = "";
        }
    }

    private void run() {
        if (uri == null) { toast("ჯერ აირჩიე ფაილი!"); return; }
        progress.setVisibility(ProgressBar.VISIBLE);
        output.setText("⏳ Streaming ანალიზი...");
        new StreamTask().execute();
    }

    private class StreamTask extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... v) {
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry e;

                int totalEntries = 0, analyzed = 0;
                int b64 = 0, hex = 0, url = 0, json = 0, highEnt = 0;
                List<String> xorCandidates = new ArrayList<>();
                List<String> interesting = new ArrayList<>();
                List<String> foundUrls = new ArrayList<>();

                byte[] buf = new byte[16384];
                byte[] header = new byte[4096];
                StringBuilder strWin = new StringBuilder();

                while ((e = zis.getNextEntry()) != null && analyzed < MAX_FILES) {
                    totalEntries++;
                    if (e.isDirectory()) continue;
                    String name = e.getName();
                    String lower = name.toLowerCase();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int total = 0, n, headerLen = 0;
                    strWin.setLength(0);
                    int[] freq = new int[256];
                    long byteCount = 0;

                    while ((n = zis.read(buf)) != -1) {
                        int remaining = PER_FILE - total;
                        if (remaining <= 0) break;
                        int toWrite = Math.min(n, remaining);
                        baos.write(buf, 0, toWrite);
                        total += toWrite;
                        for (int i = 0; i < toWrite; i++) freq[buf[i] & 0xFF]++;
                        byteCount += toWrite;
                        if (headerLen < 4096) {
                            int copy = Math.min(toWrite, 4096 - headerLen);
                            System.arraycopy(buf, 0, header, headerLen, copy);
                            headerLen += copy;
                        }
                        if (strWin.length() < 512) {
                            for (int i = 0; i < toWrite; i++) {
                                char c = (char)(buf[i] & 0xFF);
                                if (c >= 32 && c <= 126) strWin.append(c);
                                else {
                                    if (strWin.length() >= 6 && foundUrls.size() < 40) {
                                        String s = strWin.toString();
                                        if (s.startsWith("http")) foundUrls.add(name + " → " + s);
                                    }
                                    strWin.setLength(0);
                                }
                            }
                        }
                    }

                    byte[] data = baos.toByteArray();
                    if (data.length < 32) continue;

                    double ent = 0;
                    if (byteCount > 0) {
                        for (int c : freq) if (c > 0) {
                            double p = (double) c / byteCount;
                            ent -= p * (Math.log(p) / Math.log(2));
                        }
                    }
                    if (ent > 7.9) highEnt++;

                    String sample = new String(header, 0, Math.min(headerLen, 4096), "UTF-8");
                    String trimmed = sample.trim();
                    boolean isB64 = trimmed.length() >= 16 && trimmed.length() % 4 == 0
                        && trimmed.matches("^[A-Za-z0-9+/\\s]+={0,2}$");
                    boolean isHex = trimmed.length() >= 16
                        && trimmed.replaceAll("\\s+", "").matches("^[0-9A-Fa-f]+$");
                    boolean isUrl = sample.contains("%") && sample.matches(".*(%[0-9A-Fa-f]{2}){3,}.*");
                    boolean isJson = trimmed.startsWith("{") && trimmed.endsWith("}");

                    if (isB64) b64++;
                    else if (isHex) hex++;
                    else if (isUrl) url++;
                    else if (isJson) json++;

                    for (int key = 0; key < 256; key++) {
                        int printable = 0;
                        int total2 = Math.min(data.length, 512);
                        for (int i = 0; i < total2; i++) {
                            char c = (char)((data[i] ^ key) & 0xFF);
                            if (c >= 32 && c <= 126) printable++;
                        }
                        double score = (double) printable / total2;
                        if (score > 0.95 && xorCandidates.size() < 10) {
                            StringBuilder line = new StringBuilder();
                            for (int i = 0; i < Math.min(data.length, 60); i++) {
                                char c = (char)((data[i] ^ key) & 0xFF);
                                line.append((c >= 32 && c <= 126) ? c : '.');
                            }
                            xorCandidates.add(String.format("Key 0x%02X | %s | %s",
                                key, name, line.toString()));
                        }
                    }

                    if ((isB64 || isHex || isUrl || ent > 7.9) && interesting.size() < 40) {
                        String tag = "";
                        if (isB64) tag += "B64 ";
                        if (isHex) tag += "HEX ";
                        if (isUrl) tag += "URL ";
                        if (ent > 7.9) tag += "HIGH-ENT ";
                        interesting.add("[" + tag.trim() + "] " + name + "  (" + String.format("%.2f", ent) + ")");
                    }

                    analyzed++;
                    if (analyzed % 50 == 0) publishProgress("⏳ " + analyzed + " ფაილი...");
                }
                zis.close();

                StringBuilder sb = new StringBuilder();
                sb.append("╔══════════════════════════════════════╗\n");
                sb.append("║  STREAMING DECRYPTION ANALYSIS       ║\n");
                sb.append("║  Memory-safe • Handles 4GB+ APK      ║\n");
                sb.append("╚══════════════════════════════════════╝\n\n");
                sb.append("📄 File: ").append(fileName).append("\n\n");
                sb.append("━━━ SUMMARY ━━━\n");
                sb.append("   Total entries:  ").append(totalEntries).append("\n");
                sb.append("   Analyzed:       ").append(analyzed).append("\n\n");
                sb.append("━━━ ENCODING HITS ━━━\n");
                sb.append("   Base64:         ").append(b64).append("\n");
                sb.append("   Hex:            ").append(hex).append("\n");
                sb.append("   URL:            ").append(url).append("\n");
                sb.append("   JSON:           ").append(json).append("\n");
                sb.append("   High-Entropy:   ").append(highEnt).append("\n\n");
                sb.append("━━━ XOR CANDIDATES ━━━\n");
                if (xorCandidates.isEmpty()) sb.append("   (ვერაფერი)\n\n");
                else { for (String s : xorCandidates) sb.append("   ").append(s).append("\n"); sb.append("\n"); }
                sb.append("━━━ URLs ━━━\n");
                if (foundUrls.isEmpty()) sb.append("   (ვერაფერი)\n\n");
                else { for (String s : foundUrls) sb.append("   ").append(s).append("\n"); sb.append("\n"); }
                sb.append("━━━ INTERESTING FILES ━━━\n");
                if (interesting.isEmpty()) sb.append("   (ვერაფერი)\n\n");
                else { for (String s : interesting) sb.append("   ").append(s).append("\n"); sb.append("\n"); }
                sb.append("═══ END ═══\n");
                return sb.toString();
            } catch (OutOfMemoryError oom) {
                return "❌ OOM — streaming-ში ეს არ უნდა მოხდეს";
            } catch (Exception ex) {
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
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
