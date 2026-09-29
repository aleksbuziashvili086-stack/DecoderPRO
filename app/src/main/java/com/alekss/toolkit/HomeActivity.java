package com.alekss.toolkit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class HomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        LinearLayout container = findViewById(R.id.modules_container);

        String[][] modules = {
            {"🔐", "Decoder", "HEX / Base64 / URL", "decoder"},
            {"#️⃣", "Hash Lab", "MD5 / SHA-256", "hash"},
            {"🔒", "Cipher", "XOR / AES / Caesar", "cipher"},
            {"📊", "Entropy", "Entropy analysis", "entropy"},
            {"🔬", "File Analysis", "Deep analysis", "file_analysis"},
            {"🔢", "HEX Viewer", "HEX viewer / editor", "hex_viewer"},
            {"🔤", "Strings", "String extraction", "strings"},
            {"📦", "APK Explorer", "APK / ZIP explorer", "apk"},
            {"📋", "Manifest", "AndroidManifest", "manifest"},
            {"🧬", "DEX Header", "DEX analysis", "dex"},
            {"🎨", "Assets", "Assets explorer", "assets"},
            {"⚙️", "Native .so", "Native libraries", "so"}
        };

        for (String[] m : modules) {
            addCard(container, m[0], m[1], m[2], m[3]);
        }
    }

    private void addCard(LinearLayout c, String icon, String title, String desc, String id) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 30, 30, 30);
        card.setBackgroundColor(0xFF131824);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(20, 15, 20, 15);
        card.setLayoutParams(p);

        TextView i = new TextView(this);
        i.setText(icon);
        i.setTextSize(28);
        card.addView(i);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFF00E5FF);
        t.setTextSize(16);
        t.setPadding(0, 15, 0, 0);
        card.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(0xFF8892B0);
        d.setTextSize(12);
        card.addView(d);

        card.setOnClickListener(v -> openModule(id));

        c.addView(card);
    }

    private void openModule(String id) {
        try {
            Class<?> target = null;
            if (id.equals("decoder")) {
                target = DecoderActivity.class;
            }
            // სხვა მოდულები მოგვიანებით დაემატება

            if (target != null) {
                startActivity(new Intent(this, target));
            } else {
                Toast.makeText(this, "Module: " + id + " (მალე!)", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "შეცდომა: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
