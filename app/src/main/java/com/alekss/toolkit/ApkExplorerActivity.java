package com.alekss.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ApkExplorerActivity extends Activity {
    private static final int PICK = 300;
    private TextView output;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_apk_explorer);
        output = findViewById(R.id.apk_output);
        Button pick = findViewById(R.id.btn_apk_pick);
        Button clear = findViewById(R.id.btn_apk_clear);
        pick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("*/*");
            startActivityForResult(i, PICK);
        });
        clear.setOnClickListener(v -> output.setText(""));
    }

    @Override
    protected void onActivityResult(int r, int res, Intent d) {
        super.onActivityResult(r, res, d);
        if (r == PICK && res == RESULT_OK && d != null && d.getData() != null) listZip(d.getData());
    }

    private void listZip(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            ZipInputStream zis = new ZipInputStream(is);
            StringBuilder sb = new StringBuilder();
            sb.append("📦 APK/ZIP Contents\n─────────────────────\n\n");
            ZipEntry e;
            int count = 0;
            long totalSize = 0;
            while ((e = zis.getNextEntry()) != null) {
                count++;
                totalSize += e.getSize();
                sb.append(e.isDirectory() ? "📁 " : "📄 ")
                  .append(e.getName());
                if (!e.isDirectory()) sb.append("  (").append(FileUtils.formatSize(e.getSize())).append(")");
                sb.append("\n");
                if (count >= 500) { sb.append("\n... (500+ entries)"); break; }
            }
            zis.close();
            sb.append("\n─────────────────────\n");
            sb.append("Total entries: ").append(count).append("\n");
            sb.append("Uncompressed: ").append(FileUtils.formatSize(totalSize));
            output.setText(sb.toString());
        } catch (Exception e) {
            output.setText("❌ " + e.getMessage());
        }
    }
}
