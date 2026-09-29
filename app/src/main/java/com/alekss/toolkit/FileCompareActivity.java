package com.alekss.toolkit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class FileCompareActivity extends Activity {

    private static final int PICK1 = 800;
    private static final int PICK2 = 801;

    private Uri uri1, uri2;
    private String name1 = "(not selected)", name2 = "(not selected)";
    private byte[] data1, data2;
    private String report = "";
    private String diffOnly = "";
    private TextView info, result;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_file_compare);
        info = findViewById(R.id.fc_info);
        result = findViewById(R.id.fc_result);
        Button pick1 = findViewById(R.id.btn_fc_pick1);
        Button pick2 = findViewById(R.id.btn_fc_pick2);
        Button compare = findViewById(R.id.btn_fc_compare);
        Button copyAll = findViewById(R.id.btn_fc_copy);
        Button copyDiff = findViewById(R.id.btn_fc_copy_diff);
        Button edit = findViewById(R.id.btn_fc_edit);
        Button clear = findViewById(R.id.btn_fc_clear);

        pick1.setOnClickListener(v -> pickFile(PICK1));
        pick2.setOnClickListener(v -> pickFile(PICK2));
        compare.setOnClickListener(v -> compare());
        copyAll.setOnClickListener(v -> copy(report, "Report"));
        copyDiff.setOnClickListener(v -> copy(diffOnly, "Diff"));
        edit.setOnClickListener(v -> toast("Edit: აირჩიე ფაილი შედარების შემდეგ"));
        clear.setOnClickListener(v -> {
            uri1 = uri2 = null; data1 = data2 = null;
            name1 = name2 = "(not selected)";
            report = diffOnly = ""; info.setText(""); result.setText("");
        });
        updateInfo();
    }

    private void pickFile(int code) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        startActivityForResult(i, code);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent d) {
        super.onActivityResult(req, res, d);
        if (res != RESULT_OK || d == null || d.getData() == null) return;
        Uri u = d.getData();
        String n = FileUtils.getFileName(this, u);
        if (req == PICK1) { uri1 = u; name1 = n; }
        else if (req == PICK2) { uri2 = u; name2 = n; }
        updateInfo();
    }

    private void updateInfo() {
        info.setText("📁 File 1: " + name1 + "\n📁 File 2: " + name2);
    }

    private void compare() {
        if (uri1 == null || uri2 == null) { toast("აირჩიე ორივე ფაილი!"); return; }
        try {
            data1 = FileUtils.readFileBytes(this, uri1);
            data2 = FileUtils.readFileBytes(this, uri2);
            report = buildReport();
            result.setText(report);
        } catch (Exception e) {
            result.setText("❌ " + e.getMessage());
        }
    }

    private String buildReport() throws Exception {
        StringBuilder sb = new StringBuilder();
        StringBuilder diffSb = new StringBuilder();
        sb.append("╔══════════════════════════════════╗\n");
        sb.append("║   BINARY DIFF ANALYSIS REPORT    ║\n");
        sb.append("╚══════════════════════════════════╝\n\n");

        // === 1. BASIC INFO ===
        sb.append("📄 FILES:\n");
        sb.append("   F1: ").append(name1).append("\n");
        sb.append("       ").append(FileUtils.formatSize(data1.length))
          .append(" (").append(data1.length).append(" bytes)\n");
        sb.append("   F2: ").append(name2).append("\n");
        sb.append("       ").append(FileUtils.formatSize(data2.length))
          .append(" (").append(data2.length).append(" bytes)\n\n");

        // === 2. HASHES ===
        sb.append("🔐 HASHES:\n");
        String md51 = md5(data1), md52 = md5(data2);
        String sha1_1 = sha(data1, "SHA-1"), sha1_2 = sha(data2, "SHA-1");
        String sha256_1 = sha(data1, "SHA-256"), sha256_2 = sha(data2, "SHA-256");
        String sha512_1 = sha(data1, "SHA-512"), sha512_2 = sha(data2, "SHA-512");

        sb.append("   MD5      F1: ").append(md51).append("\n");
        sb.append("   MD5      F2: ").append(md52).append("\n");
        sb.append("   SHA-1    F1: ").append(sha1_1).append("\n");
        sb.append("   SHA-1    F2: ").append(sha1_2).append("\n");
        sb.append("   SHA-256  F1: ").append(sha256_1).append("\n");
        sb.append("   SHA-256  F2: ").append(sha256_2).append("\n");
        sb.append("   SHA-512  F1: ").append(sha512_1).append("\n");
        sb.append("   SHA-512  F2: ").append(sha512_2).append("\n\n");

        boolean identical = md51.equals(md52);
        if (identical) {
            sb.append("   ✅ IDENTICAL — 100% ემთხვევა\n\n");
            diffSb.append("✅ ფაილები იდენტურია. განსხვავება არ არის.\n");
            return sb.toString();
        } else {
            sb.append("   ❌ DIFFERENT — ფაილები სხვადასხვაა\n\n");
        }

        // === 3. SIZE ===
        int sizeDiff = data2.length - data1.length;
        sb.append("📏 SIZE DIFFERENCE:\n");
        if (sizeDiff > 0) sb.append("   +").append(sizeDiff).append(" bytes (F2 უფრო დიდი)\n\n");
        else if (sizeDiff < 0) sb.append("   ").append(sizeDiff).append(" bytes (F1 უფრო დიდი)\n\n");
        else sb.append("   იგივე ზომა\n\n");

        // === 4. ENTROPY ===
        double e1 = entropy(data1), e2 = entropy(data2);
        sb.append("🔐 ENTROPY:\n");
        sb.append("   F1: ").append(String.format("%.4f", e1)).append(" bits/byte\n");
        sb.append("   F2: ").append(String.format("%.4f", e2)).append(" bits/byte\n");
        sb.append("   Δ:  ").append(String.format("%.4f", Math.abs(e1 - e2))).append("\n\n");

        // === 5. BYTE-BY-BYTE ===
        int minLen = Math.min(data1.length, data2.length);
        int maxLen = Math.max(data1.length, data2.length);
        int diffCount = 0;
        int firstDiffOffset = -1, lastDiffOffset = -1;
        List<String> diffs = new ArrayList<>();

        for (int i = 0; i < minLen; i++) {
            if (data1[i] != data2[i]) {
                diffCount++;
                if (firstDiffOffset == -1) firstDiffOffset = i;
                lastDiffOffset = i;
                if (diffs.size() < 100) {
                    char c1 = (char)(data1[i] & 0xFF), c2 = (char)(data2[i] & 0xFF);
                    String a1 = (c1 >= 32 && c1 <= 126) ? String.valueOf(c1) : ".";
                    String a2 = (c2 >= 32 && c2 <= 126) ? String.valueOf(c2) : ".";
                    diffs.add(String.format("   0x%08X  F1=%02X[%s]  F2=%02X[%s]",
                        i, data1[i] & 0xFF, a1, data2[i] & 0xFF, a2));
                }
            }
        }

        double diffPercent = minLen > 0 ? (diffCount * 100.0 / minLen) : 0;
        double similarity = 100.0 - diffPercent;

        sb.append("📊 BYTE COMPARISON:\n");
        sb.append("   Shared length:  ").append(minLen).append("\n");
        sb.append("   Match:          ").append(minLen - diffCount).append("\n");
        sb.append("   Different:      ").append(diffCount).append("\n");
        sb.append("   Diff %:         ").append(String.format("%.4f%%", diffPercent)).append("\n");
        sb.append("   Similarity:     ").append(String.format("%.4f%%", similarity)).append("\n");

        // ASCII bar
        sb.append("   [");
        int bars = 30;
        int filled = (int)(similarity / 100.0 * bars);
        for (int i = 0; i < bars; i++) sb.append(i < filled ? "█" : "░");
        sb.append("]\n\n");

        // === 6. REGION ANALYSIS ===
        if (minLen > 0) {
            sb.append("🗺️ REGION ANALYSIS (10 regions):\n");
            int regionSize = Math.max(1, minLen / 10);
            for (int r = 0; r < 10; r++) {
                int start = r * regionSize;
                int end = Math.min(start + regionSize, minLen);
                if (start >= minLen) break;
                int rDiff = 0;
                for (int i = start; i < end; i++) if (data1[i] != data2[i]) rDiff++;
                double rPct = (end - start) > 0 ? (rDiff * 100.0 / (end - start)) : 0;
                String regionAscii = (rPct > 50) ? "███" : (rPct > 10) ? "██░" : (rPct > 1) ? "█░░" : "░░░";
                sb.append("   Region ").append(r + 1)
                  .append(" 0x").append(String.format("%08X", start))
                  .append("-0x").append(String.format("%08X", end))
                  .append("  ").append(regionAscii)
                  .append(" ").append(String.format("%.2f%%", rPct)).append("\n");
            }
            sb.append("\n");
        }

        // === 7. FIRST DIFFS ===
        if (!diffs.isEmpty()) {
            sb.append("📍 FIRST ").append(diffs.size()).append(" DIFFERENCES:\n");
            diffSb.append("📍 განსხვავებები (").append(diffCount).append(" სულ):\n");
            for (String s : diffs) {
                sb.append(s).append("\n");
                diffSb.append(s).append("\n");
            }
            if (diffCount > diffs.size())
                sb.append("   ... და კიდევ ").append(diffCount - diffs.size()).append("\n");
            sb.append("\n");
            sb.append("📍 First offset: 0x").append(String.format("%08X", firstDiffOffset)).append("\n");
            sb.append("📍 Last  offset: 0x").append(String.format("%08X", lastDiffOffset)).append("\n\n");
        }

        // === 8. TRAILING ===
        if (data1.length != data2.length) {
            byte[] longer = data1.length > data2.length ? data1 : data2;
            String lname = data1.length > data2.length ? "File 1" : "File 2";
            sb.append("📎 TRAILING BYTES (").append(lname).append("):\n");
            sb.append("   +").append(maxLen - minLen).append(" bytes\n");
            sb.append("   HEX: ");
            for (int i = minLen; i < Math.min(maxLen, minLen + 32); i++)
                sb.append(String.format("%02X ", longer[i]));
            sb.append("\n   ASCII: ");
            for (int i = minLen; i < Math.min(maxLen, minLen + 64); i++) {
                char c = (char)(longer[i] & 0xFF);
                sb.append((c >= 32 && c <= 126) ? c : '.');
            }
            sb.append("\n\n");
        }

        // === 9. FINAL VERDICT ===
        sb.append("═══════════════════════════════════\n");
        sb.append("📌 FINAL VERDICT:\n");
        sb.append("   Similarity: ").append(String.format("%.2f%%", similarity)).append("\n");
        if (similarity >= 99.99) sb.append("   → ფაილები იდენტურია\n");
        else if (similarity >= 99) sb.append("   → ძალიან მცირე ცვლილება (<1%)\n");
        else if (similarity >= 90) sb.append("   → მცირე ცვლილება (1-10%)\n");
        else if (similarity >= 50) sb.append("   → საშუალო ცვლილება (10-50%)\n");
        else sb.append("   → მნიშვნელოვანი ცვლილება (>50%)\n");

        diffOnly = diffSb.toString();
        return sb.toString();
    }

    private String md5(byte[] b) throws Exception {
        return toHex(MessageDigest.getInstance("MD5").digest(b));
    }
    private String sha(byte[] b, String algo) throws Exception {
        return toHex(MessageDigest.getInstance(algo).digest(b));
    }
    private String toHex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }
    private double entropy(byte[] b) {
        if (b.length == 0) return 0;
        int[] f = new int[256];
        for (byte x : b) f[x & 0xFF]++;
        double e = 0;
        for (int c : f) if (c > 0) {
            double p = (double) c / b.length;
            e -= p * (Math.log(p) / Math.log(2));
        }
        return e;
    }
    private void copy(String text, String label) {
        if (text.isEmpty()) { toast("ჯერ შეადარე!"); return; }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText(label, text));
        toast("✅ " + label + " დაკოპირდა!");
    }
    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
    }
}
